package dev.closet.ui

import android.app.Application
import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.runtime.*
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.room.Room
import dev.closet.data.*
import dev.closet.domain.*
import dev.closet.imaging.ImageStore
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import java.io.File
import java.util.UUID

val categories = listOf("hat", "top", "jacket", "bottom", "shoes", "pajamas", "accessory", "other")
val colors = listOf("neutral", "black", "white", "cream", "gray", "navy", "brown", "blue", "green", "red", "pink", "purple", "yellow", "orange")
val formalities = listOf("casual", "smart", "formal")
val seasons = listOf("all", "spring", "summer", "autumn", "winter")

data class ItemDraft(val item: ClothingItem?, val original: Bitmap, val cutout: Bitmap, val warning: String? = null)

class ClosetViewModel(app: Application) : AndroidViewModel(app) {
    private val db = Room.databaseBuilder(app, ClosetDatabase::class.java, "closet.db").build()
    private val dao = db.closet()
    val images = ImageStore(app)
    val items = dao.observeItems().stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val outfits = dao.observeOutfits().stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    private val engine = OutfitEngine(StyleRules(app.assets.open("style_rules.json").bufferedReader().use { it.readText() }))
    val messages = Channel<String>(Channel.BUFFERED)
    var busy by mutableStateOf(false); private set
    var progress by mutableStateOf(""); private set
    var draft by mutableStateOf<ItemDraft?>(null); private set
    var current by mutableStateOf<List<ClothingItem>>(emptyList()); private set
    var preview by mutableStateOf<Bitmap?>(null); private set
    var currentSavedId by mutableStateOf<String?>(null); private set
    var favorite by mutableStateOf(false); private set
    private val queue = ArrayDeque<Uri>()

    private fun job(label: String, block: suspend () -> Unit) {
        if (busy) return
        viewModelScope.launch {
            busy = true; progress = label
            try { block() }
            catch (e: CancellationException) { throw e }
            catch (e: Exception) { messages.send(e.message ?: "Something went wrong. Please try again.") }
            finally { busy = false; progress = "" }
        }
    }
    fun import(uris: List<Uri>) {
        if (busy || draft != null) return
        queue.addAll(uris.take(30)); next()
    }
    private fun next() {
        if (queue.isEmpty()) { draft = null; return }
        val uri = queue.removeFirst()
        job("Preparing photo · ${queue.size + 1} remaining") {
            val original = images.import(uri)
            val segmented = try { withTimeout(30000) { withContext(Dispatchers.Default) { images.cutout(original) } } }
            catch (e: CancellationException) { if (e !is TimeoutCancellationException) throw e; null }
            catch (_: Exception) { null }
            draft = ItemDraft(null, original, segmented ?: original.copy(Bitmap.Config.ARGB_8888, true),
                if (segmented == null) "Automatic cutout unavailable. Google Play services may need a one-time model download. Keep the original or use the brush." else null)
        }
        viewModelScope.launch {
            while (busy) delay(50)
            if (draft == null && queue.isNotEmpty()) next()
        }
    }
    fun skipDraft() { draft = null; next() }
    fun edit(item: ClothingItem) = job("Opening item") {
        draft = withContext(Dispatchers.IO) { ItemDraft(item, images.read(item.originalPath), images.read(item.imagePath)) }
    }
    fun saveDraft(bitmap: Bitmap, name: String, category: String, color: String, formality: String, season: String) {
        val d = draft ?: return
        job("Saving item") {
            withContext(Dispatchers.IO) {
                val pixels = IntArray(bitmap.width * bitmap.height)
                bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
                require(pixels.any { android.graphics.Color.alpha(it) > 15 }) { "The cutout is empty. Restore part of the photo or keep the original." }
                val originalPath = d.item?.originalPath ?: images.write(d.original)
                val path = images.write(bitmap)
                val item = ClothingItem(d.item?.id ?: UUID.randomUUID().toString(), name.trim().take(80), category, color, formality, season,
                    path, originalPath, d.item?.createdAt ?: System.currentTimeMillis())
                try { dao.putItem(item) } catch(e: Exception) { File(path).delete(); if(d.item == null) File(originalPath).delete(); throw e }
                d.item?.imagePath?.let { File(it).delete() }
                if (current.any { it.id == item.id }) {
                    current = current.map { if (it.id == item.id) item else it }
                    preview = images.render(current); currentSavedId = null; favorite = false
                }
            }
            draft = null
            messages.send("Item saved")
        }
        // Continue after the save job has released its busy flag.
        viewModelScope.launch { while (busy) delay(50); if (draft == null) next() }
    }
    fun delete(item: ClothingItem) = job("Deleting item") {
        withContext(Dispatchers.IO) {
            dao.deleteItem(item); File(item.imagePath).delete(); File(item.originalPath).delete()
            current = current.filterNot { it.id == item.id }
            preview = if (current.isEmpty()) null else images.render(current)
            currentSavedId = null; favorite = false
        }
    }
    fun shuffle(season: String, formality: String) = job("Finding your next outfit") {
        val selection = engine.generate(items.value, season, formality)
        val bitmap = withContext(Dispatchers.Default) { images.render(selection) }
        current = selection; preview = bitmap; currentSavedId = null; favorite = false
    }
    fun swap(category: String, item: ClothingItem?) = job("Updating outfit") {
        val next = OutfitEngine.ordered(current.filterNot { it.category == category } + listOfNotNull(item))
        preview = withContext(Dispatchers.Default) { images.render(next) }
        current = next; currentSavedId = null; favorite = false
    }
    fun saveOutfit(makeFavorite: Boolean = favorite) = job("Saving outfit") {
        val bitmap = preview ?: error("Build an outfit first.")
        withContext(Dispatchers.IO) {
            val existing = outfits.value.firstOrNull { it.id == currentSavedId }
            val path = existing?.imagePath ?: images.write(bitmap)
            val outfit = SavedOutfit(existing?.id ?: UUID.randomUUID().toString(), existing?.name ?: "Outfit ${outfits.value.size + 1}",
                current.joinToString(",") { it.id }, path, makeFavorite, existing?.createdAt ?: System.currentTimeMillis())
            try { dao.putOutfit(outfit) } catch(e: Exception) { if(existing == null) File(path).delete(); throw e }
            currentSavedId = outfit.id; favorite = makeFavorite
        }
        messages.send(if (makeFavorite) "Saved to favorites" else "Outfit saved")
    }
    fun toggleFavorite(outfit: SavedOutfit) = job("Updating favorite") { dao.putOutfit(outfit.copy(favorite = !outfit.favorite)) }
    fun deleteOutfit(outfit: SavedOutfit) = job("Deleting outfit") {
        withContext(Dispatchers.IO) { dao.deleteOutfit(outfit); File(outfit.imagePath).delete() }
        if(currentSavedId == outfit.id) currentSavedId = null
    }
    fun export(outfit: SavedOutfit) = job("Exporting PNG") { withContext(Dispatchers.IO) { images.export(outfit.imagePath) }; messages.send("Saved in Pictures/Closet") }
    fun share(outfit: SavedOutfit) = job("Preparing share") { images.share(outfit.imagePath) }
    override fun onCleared() { db.close(); super.onCleared() }
}
