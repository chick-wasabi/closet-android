package dev.closet.ui

import android.app.Application
import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.runtime.*
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.room.Room
import androidx.room.withTransaction
import dev.closet.data.*
import dev.closet.domain.*
import dev.closet.imaging.ImageStore
import dev.closet.processing.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import java.io.File
import java.util.UUID

val categories=listOf("hat","top","jacket","bottom","shoes","pajamas","accessory","other")
val colors=listOf("neutral","black","white","cream","gray","navy","brown","blue","green","red","pink","purple","yellow","orange")
val formalities=listOf("casual","smart","formal")
val seasons=listOf("all","spring","summer","autumn","winter")
val occasions=listOf("Casual","College/Work","Date","Party","Gym/Home","Sleep")
val patterns=listOf("solid","striped","dotted","checked","printed")
data class ItemDraft(val item: ClothingItem?,val original: Bitmap,val cutout: Bitmap,val warning: String?=null)
data class EditResult(val original: Bitmap,val cutout: Bitmap,val item: ClothingItem)
data class ReviewItem(val item: ClothingItem,val warning: String?,val reviewed: Boolean=false)
data class OutfitOption(val pieces: List<ClothingItem>,val image: Bitmap,val why: String)

class ClosetViewModel(app: Application): AndroidViewModel(app) {
    private val db=Room.databaseBuilder(app,ClosetDatabase::class.java,"closet.db").addMigrations(ClosetDatabase.MIGRATION_1_2).build()
    private val dao=db.closet()
    val images=ImageStore(app)
    val items=dao.observeItems().stateIn(viewModelScope,SharingStarted.Eagerly,emptyList())
    val outfits=dao.observeOutfits().stateIn(viewModelScope,SharingStarted.Eagerly,emptyList())
    val wear=dao.observeWear().stateIn(viewModelScope,SharingStarted.Eagerly,emptyList())
    private val preferences=dao.observePreferences().stateIn(viewModelScope,SharingStarted.Eagerly,emptyList())
    private val engine=runCatching{OutfitEngine(StyleRules(app.assets.open("style_rules.json").bufferedReader().use{it.readText()}))}
    val messages=Channel<String>(Channel.BUFFERED)
    var settings by mutableStateOf(AppSettings.load(app));private set
    var busy by mutableStateOf(false);private set
    var progress by mutableStateOf("");private set
    var completed by mutableIntStateOf(0);private set
    var total by mutableIntStateOf(0);private set
    var draft by mutableStateOf<ItemDraft?>(null);private set
    var review by mutableStateOf<List<ReviewItem>>(emptyList());private set
    var current by mutableStateOf<List<ClothingItem>>(emptyList());private set
    var preview by mutableStateOf<Bitmap?>(null);private set
    var options by mutableStateOf<List<OutfitOption>>(emptyList());private set
    var optionIndex by mutableIntStateOf(0);private set
    var locks by mutableStateOf<Set<String>>(emptySet());private set
    var occasion by mutableStateOf("Casual");private set
    var currentSavedId by mutableStateOf<String?>(null);private set
    var favorite by mutableStateOf(false);private set
    private fun job(label: String,block: suspend ()->Unit) {
        if(busy)return
        viewModelScope.launch { busy=true;progress=label
            try{block()}catch(e: CancellationException){throw e}catch(e: Exception){messages.send(e.message?:"Please try again.")}
            finally{busy=false;progress="";total=0;completed=0}
        }
    }
    fun updateSettings(value: AppSettings) = job("Saving preferences") { withContext(Dispatchers.IO){AppSettings.save(getApplication(),value)};settings=value }
    fun import(uris: List<Uri>)=job("Preparing photos") {
        if(draft!=null||review.isNotEmpty()) return@job
        total=uris.size
        val pending=mutableListOf<ReviewItem>()
        for((index,uri) in uris.withIndex()) {
            progress="Photo ${index+1} of $total"
            try {
                val raw=images.import(uri)
                val segmented=try{withTimeout(30000){images.cutout(raw)}}catch(e: CancellationException){if(e !is TimeoutCancellationException)throw e;null}catch(_:Exception){null}
                var original=raw;var cutout=segmented?:raw.copy(Bitmap.Config.ARGB_8888,true)
                var item=ClothingItem(UUID.randomUUID().toString(),imagePath="",originalPath="")
                withContext(Dispatchers.Default) {
                    val labels=if(settings.autoFill) runCatching{withTimeout(8000){PhotoProcessing.labels(cutout)}}.getOrDefault(emptyList()) else emptyList()
                    val px=PhotoProcessing.pixels(cutout)
                    if(settings.autoFill) {
                        val bounds=GarmentAnalysis.bounds(px,cutout.width,cutout.height)
                        val cat=GarmentAnalysis.category(labels,bounds.first,bounds.second);val sub=GarmentAnalysis.subcategory(cat,labels)
                        val (formal,season)=GarmentAnalysis.defaults(sub);val color=GarmentAnalysis.color(px);val pattern=GarmentAnalysis.pattern(px,cutout.width,cutout.height)
                        item=item.copy(category=cat,subcategory=sub,color=color,pattern=pattern,formality=formal,season=season,
                            name="${color.replaceFirstChar{it.uppercase()}} ${if(pattern=="solid") "" else "$pattern "}$sub",autoFields="name,category,subcategory,color,pattern,formality,season")
                    }
                    if(settings.autoRotate&&segmented!=null) { val q=GarmentAnalysis.orientation(px,cutout.width,cutout.height,item.category)
                        if(q!=0){original=PhotoProcessing.transform(original,q);cutout=PhotoProcessing.transform(cutout,q)}
                    }
                }
                val paths=withContext(Dispatchers.IO){images.write(original) to images.write(cutout)}
                item=item.copy(originalPath=paths.first,imagePath=paths.second)
                pending+=ReviewItem(item,if(segmented==null)"Cutout unavailable. Keep the photo or use the brush." else null)
                review=pending.toList()
            }catch(e: CancellationException){throw e}catch(_:Exception){messages.trySend("Photo ${index+1} could not be read; the others are kept.")}
            completed=index+1
        }
    }
    fun editReview(value: ReviewItem)=job("Opening photo") { draft=withContext(Dispatchers.IO){ItemDraft(value.item,images.read(value.item.originalPath),images.read(value.item.imagePath),value.warning)} }
    fun skipDraft(){draft=null}
    fun edit(item: ClothingItem)=job("Opening item"){draft=withContext(Dispatchers.IO){ItemDraft(item,images.read(item.originalPath),images.read(item.imagePath))}}
    fun saveDraft(bitmap: Bitmap,name: String,category: String,color: String,formality: String,season: String) {
        val d=draft?:return
        saveEdit(EditResult(d.original,bitmap,(d.item?:ClothingItem(UUID.randomUUID().toString(),imagePath="",originalPath="")).copy(name=name,category=category,color=color,formality=formality,season=season)))
    }
    fun saveEdit(result: EditResult)=job("Saving piece") {
        require(PhotoProcessing.pixels(result.cutout).any{it ushr 24>15}){"The cutout is empty. Restore it or keep the photo."}
        val old=draft?.item
        withContext(Dispatchers.IO) {
            val original=images.write(result.original);val path=try{images.write(result.cutout)}catch(e:Exception){File(original).delete();throw e}
            val item=result.item.copy(name=result.item.name.trim().take(80),imagePath=path,originalPath=original)
            val staged=review.any{it.item.id==item.id}
            try { if(staged) review=review.map{if(it.item.id==item.id)it.copy(item=item,warning=null,reviewed=true) else it} else dao.putItem(item) }
            catch(e:Exception){File(path).delete();File(original).delete();throw e}
            old?.let{File(it.imagePath).delete();File(it.originalPath).delete()}
            if(current.any{it.id==item.id}){current=current.map{if(it.id==item.id)item else it};resetOptions()}
        }
        draft=null;messages.send("Piece saved")
    }
    fun saveAll()=job("Saving closet"){
        val ready=mutableListOf<ClothingItem>();val created=mutableListOf<File>();var committed=false
        try {
            withContext(Dispatchers.IO) {
                for(value in review) {
                    var item=value.item
                    if(settings.cleanup && !value.reviewed && value.warning==null) {
                        val pair=withContext(Dispatchers.Default){PhotoProcessing.clean(images.read(item.originalPath),images.read(item.imagePath))}
                        val raw=images.write(pair.first);created+=File(raw)
                        val cut=images.write(pair.second);created+=File(cut)
                        item=item.copy(originalPath=raw,imagePath=cut)
                    }
                    ready+=item
                }
                db.withTransaction { ready.forEach{dao.putItem(it)} };committed=true
                review.filter{old->ready.any{it.id==old.item.id&&it.imagePath!=old.item.imagePath}}.forEach{File(it.item.imagePath).delete();File(it.item.originalPath).delete()}
            }
        } catch(e:Exception){if(!committed)created.forEach{it.delete()};throw e}
        review=emptyList();messages.send("All pieces saved")
    }
    fun removeReview(value: ReviewItem){if(busy)return;File(value.item.imagePath).delete();File(value.item.originalPath).delete();review=review.filterNot{it.item.id==value.item.id}}
    fun discardReview(){if(busy)return;review.forEach{File(it.item.imagePath).delete();File(it.item.originalPath).delete()};review=emptyList()}
    fun delete(item: ClothingItem)=deleteMany(listOf(item))
    fun deleteMany(selected: List<ClothingItem>)=job("Deleting pieces"){
        withContext(Dispatchers.IO){db.withTransaction{selected.forEach{dao.deleteItem(it)}};selected.forEach{File(it.imagePath).delete();File(it.originalPath).delete()}}
        current=current.filterNot{p->selected.any{it.id==p.id}};locks=locks.intersect(current.map{it.id}.toSet());resetOptions()
    }
    fun saveOrientation(item: ClothingItem, turns: Int, flip: Boolean)=job("Saving orientation") {
        withContext(Dispatchers.IO) {
            val raw=images.write(PhotoProcessing.transform(images.read(item.originalPath),turns,flip))
            val cut=try{images.write(PhotoProcessing.transform(images.read(item.imagePath),turns,flip))}catch(e:Exception){File(raw).delete();throw e}
            val updated=item.copy(originalPath=raw,imagePath=cut)
            try{dao.putItem(updated)}catch(e:Exception){File(raw).delete();File(cut).delete();throw e}
            File(item.originalPath).delete();File(item.imagePath).delete()
            current=current.map{if(it.id==item.id)updated else it}
        }
        if(current.any{it.id==item.id})resetOptions()
    }
    fun favoriteItem(item: ClothingItem)=job("Updating favorite"){dao.putItem(item.copy(favorite=!item.favorite))}
    fun duplicate(item: ClothingItem)=job("Copying piece"){
        withContext(Dispatchers.IO){val image=images.write(images.read(item.imagePath));val original=images.write(images.read(item.originalPath))
            try{dao.putItem(item.copy(id=UUID.randomUUID().toString(),name="${item.name.ifBlank{item.category}} copy",imagePath=image,originalPath=original,createdAt=System.currentTimeMillis(),timesWorn=0,lastWorn=0))}
            catch(e:Exception){File(image).delete();File(original).delete();throw e}}
    }
    fun recut(item: ClothingItem)=job("Making a new cutout"){
        val original=withContext(Dispatchers.IO){images.read(item.originalPath)}
        try{val cut=withTimeout(30000){images.cutout(original)};draft=ItemDraft(item,original,cut)}
        catch(e: CancellationException){if(e !is TimeoutCancellationException)throw e;draft=ItemDraft(item,original,images.read(item.imagePath),"Cutout timed out. Your saved photo is safe.")}
        catch(_:Exception){draft=ItemDraft(item,original,images.read(item.imagePath),"Cutout unavailable. Use the brush or keep the photo.")}
    }
    fun changeOccasion(value: String){occasion=value;locks=emptySet()}
    fun toggleLock(item: ClothingItem){locks=if(item.id in locks)locks-item.id else locks+item.id}
    fun shuffle(season: String,formality: String)=job("Finding outfits"){
        val generator=engine.getOrThrow()
        val selections=generator.options(items.value,season,formality,occasion,current.filter{it.id in locks},preferences.value.associate{it.pair to it.value},settings.repeatDays,settings.hatFrequency.toDouble(),settings.jacketFrequency.toDouble())
        options=withContext(Dispatchers.Default){selections.map{OutfitOption(it,images.render(it),generator.explain(it))}}
        selectOption(0)
    }
    fun selectOption(index: Int){options.getOrNull(index)?.let{optionIndex=index;current=it.pieces;preview=it.image;currentSavedId=null;favorite=false}}
    private suspend fun resetOptions(){preview=if(current.isEmpty())null else withContext(Dispatchers.Default){images.render(current)};options=emptyList();currentSavedId=null;favorite=false}
    fun swap(category: String,item: ClothingItem?)=job("Updating outfit"){
        val old=current.filter{it.category==category};locks=locks-old.map{it.id}.toSet()
        current=OutfitEngine.ordered(current.filterNot{it.category==category}+listOfNotNull(item));resetOptions()
    }
    fun feedback(liked: Boolean)=job("Remembering preference"){
        db.withTransaction{val old=dao.allPreferences().associate{it.pair to it.value};for(i in current.indices)for(j in 0 until i){val key=OutfitEngine.pairKey(current[i].id,current[j].id);dao.putPreference(PairPreference(key,((old[key]?:0)+if(liked)1 else -1).coerceIn(-10,10)))}}
        messages.send("Preference saved on this device")
    }
    fun saveOutfit(makeFavorite: Boolean=favorite)=job("Saving outfit"){
        val bitmap=preview?:error("Build an outfit first.")
        withContext(Dispatchers.IO){val existing=dao.allOutfits().firstOrNull{it.id==currentSavedId};val path=existing?.imagePath?:images.write(bitmap)
            val outfit=SavedOutfit(existing?.id?:UUID.randomUUID().toString(),existing?.name?:"Outfit ${outfits.value.size+1}",current.joinToString(","){it.id},path,makeFavorite,existing?.createdAt?:System.currentTimeMillis())
            try{dao.putOutfit(outfit)}catch(e:Exception){if(existing==null)File(path).delete();throw e};currentSavedId=outfit.id;favorite=makeFavorite}
        messages.send("Outfit saved")
    }
    fun toggleFavorite(outfit: SavedOutfit)=job("Updating favorite"){dao.putOutfit(outfit.copy(favorite=!outfit.favorite));if(currentSavedId==outfit.id)favorite=!outfit.favorite}
    fun rename(outfit: SavedOutfit,name: String)=job("Renaming outfit"){dao.putOutfit(outfit.copy(name=name.trim().take(80).ifBlank{"Outfit"}))}
    fun wore(outfit: SavedOutfit)=job("Recording wear"){
        val now=System.currentTimeMillis();val today=java.time.LocalDate.now();val zone=java.time.ZoneId.systemDefault()
        db.withTransaction{
            require(dao.allWear().none{it.outfitId==outfit.id&&java.time.Instant.ofEpochMilli(it.wornAt).atZone(zone).toLocalDate()==today}){"Already logged today."}
            dao.putWear(WearEntry(UUID.randomUUID().toString(),outfit.id,outfit.itemIds,now));val ids=outfit.itemIds.split(",").toSet()
            dao.allItems().filter{it.id in ids}.forEach{dao.putItem(it.copy(timesWorn=it.timesWorn+1,lastWorn=now))}
        };messages.send("Worn today")
    }
    fun deleteOutfit(outfit: SavedOutfit)=job("Deleting outfit"){withContext(Dispatchers.IO){dao.deleteOutfit(outfit);File(outfit.imagePath).delete()};if(currentSavedId==outfit.id){currentSavedId=null;favorite=false}}
    fun export(outfit: SavedOutfit)=job("Exporting PNG"){withContext(Dispatchers.IO){images.export(outfit.imagePath)};messages.send("Saved in Pictures/Closet")}
    fun share(outfit: SavedOutfit)=job("Preparing share"){images.share(outfit.imagePath)}
    fun backup(uri: Uri)=job("Backing up closet"){withContext(Dispatchers.IO){ClosetBackup(getApplication(),db).export(uri,settings)};messages.send("Backup saved. Keep this ZIP somewhere safe.")}
    fun restore(uri: Uri)=job("Restoring closet"){
        val restored=withContext(Dispatchers.IO){ClosetBackup(getApplication(),db).restore(uri)}
        settings=restored;withContext(Dispatchers.IO){AppSettings.save(getApplication(),restored)};current=emptyList();locks=emptySet();resetOptions();messages.send("Closet restored")
    }
    fun deleteAll()=job("Deleting closet"){
        withContext(Dispatchers.IO){db.withTransaction{dao.clearItems();dao.clearOutfits();dao.clearWear();dao.clearPreferences()};File(getApplication<Application>().filesDir,"images").listFiles()?.forEach{it.delete()}}
        settings=AppSettings();withContext(Dispatchers.IO){AppSettings.save(getApplication(),settings)};current=emptyList();locks=emptySet();resetOptions();messages.send("Closet cleared")
    }
    override fun onCleared(){db.close();super.onCleared()}
}
