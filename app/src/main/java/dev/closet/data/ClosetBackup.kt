package dev.closet.data

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.room.withTransaction
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.UUID
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/** Portable logical database snapshot. Validate everything before replacing any live rows. */
class ClosetBackup(private val context: Context,private val db: ClosetDatabase) {
    private val dao=db.closet()
    suspend fun export(uri: Uri,settings: AppSettings) {
        db.withTransaction {
            val items=dao.allItems();val outfits=dao.allOutfits();val paths=(items.flatMap { listOf(it.imagePath,it.originalPath) }+outfits.map{it.imagePath}).distinct()
            val names=paths.mapIndexed { i,p->p to "images/$i.png" }.toMap()
            val root=JSONObject().put("format","closet").put("version",2).put("settings",JSONObject(settings.json()))
            root.put("items",JSONArray(items.map { i->JSONObject().put("id",i.id).put("name",i.name).put("category",i.category).put("color",i.color)
                .put("formality",i.formality).put("season",i.season).put("imagePath",names[i.imagePath]).put("originalPath",names[i.originalPath])
                .put("createdAt",i.createdAt).put("subcategory",i.subcategory).put("pattern",i.pattern).put("brand",i.brand).put("autoFields",i.autoFields)
                .put("favorite",i.favorite).put("timesWorn",i.timesWorn).put("lastWorn",i.lastWorn) }))
            root.put("outfits",JSONArray(outfits.map { o->JSONObject().put("id",o.id).put("name",o.name).put("itemIds",o.itemIds).put("imagePath",names[o.imagePath]).put("favorite",o.favorite).put("createdAt",o.createdAt) }))
            root.put("wear",JSONArray(dao.allWear().map { JSONObject().put("id",it.id).put("outfitId",it.outfitId).put("itemIds",it.itemIds).put("wornAt",it.wornAt) }))
            root.put("preferences",JSONArray(dao.allPreferences().map { JSONObject().put("pair",it.pair).put("value",it.value) }))
            ZipOutputStream(requireNotNull(context.contentResolver.openOutputStream(uri,"wt"))).use { zip->
                zip.putNextEntry(ZipEntry("database.json"));zip.write(root.toString().toByteArray());zip.closeEntry()
                for((path,name) in names) { zip.putNextEntry(ZipEntry(name));File(path).inputStream().use { it.copyTo(zip) };zip.closeEntry() }
            }
        }
    }
    suspend fun restore(uri: Uri): AppSettings {
        val stage=File(context.filesDir,"restore-${UUID.randomUUID()}").apply{mkdirs()};val files=mutableMapOf<String,File>()
        val installed=mutableListOf<File>();var committed=false
        try {
            var total=0L
            ZipInputStream(requireNotNull(context.contentResolver.openInputStream(uri))).use { zip->
                var entry=zip.nextEntry
                while(entry!=null) {
                    val name=entry.name
                    require(name=="database.json"||Regex("images/[0-9]+\\.png").matches(name)) { "Unsupported backup entry." }
                    require(!files.containsKey(name) && files.size<20000) { "Invalid or oversized backup." }
                    val file=File(stage,files.size.toString());files[name]=file
                    var size=0L
                    file.outputStream().use { out-> val buffer=ByteArray(32768);var n=zip.read(buffer)
                        while(n!=-1) { size+=n;total+=n;require(size<=32L*1024*1024 && total<=1024L*1024*1024){"Backup is too large (1 GB limit)."};out.write(buffer,0,n);n=zip.read(buffer) }
                    };zip.closeEntry();entry=zip.nextEntry
                }
            }
            val manifest=requireNotNull(files["database.json"]) { "No Closet database in this ZIP." }
            val root=JSONObject(manifest.readText());require(root.getString("format")=="closet" && root.getInt("version")==2){"Unsupported backup version."}
            val settings=AppSettings.parse(root.getJSONObject("settings").toString())
            val target=File(context.filesDir,"images").apply{mkdirs()};val mapped=mutableMapOf<String,String>()
            for((name,file) in files.filterKeys{it!="database.json"}) {
                val bounds=BitmapFactory.Options().apply{inJustDecodeBounds=true};BitmapFactory.decodeFile(file.path,bounds)
                require(bounds.outWidth in 1..4096 && bounds.outHeight in 1..4096 && bounds.outMimeType=="image/png"){"Invalid backup image."}
                val dest=File(target,"${UUID.randomUUID()}.png");file.copyTo(dest);installed+=dest;mapped[name]=dest.absolutePath
            }
            fun image(j: JSONObject,key: String)=requireNotNull(mapped[j.getString(key)]){"Backup is missing a photo."}
            fun rows(key: String)=root.getJSONArray(key).let{a->(0 until a.length()).map{a.getJSONObject(it)}}
            val items=rows("items").map { j->ClothingItem(j.getString("id"),j.getString("name"),j.getString("category"),j.getString("color"),j.getString("formality"),j.getString("season"),image(j,"imagePath"),image(j,"originalPath"),j.getLong("createdAt"),j.getString("subcategory"),j.getString("pattern"),j.getString("brand"),j.getString("autoFields"),j.getBoolean("favorite"),j.getInt("timesWorn"),j.getLong("lastWorn")) }
            val outfits=rows("outfits").map{j->SavedOutfit(j.getString("id"),j.getString("name"),j.getString("itemIds"),image(j,"imagePath"),j.getBoolean("favorite"),j.getLong("createdAt"))}
            require(items.map{it.id}.distinct().size==items.size && outfits.map{it.id}.distinct().size==outfits.size){"Duplicate backup IDs."}
            val wear=rows("wear").map{WearEntry(it.getString("id"),it.getString("outfitId"),it.getString("itemIds"),it.getLong("wornAt"))}
            val preferences=rows("preferences").map{PairPreference(it.getString("pair"),it.getInt("value").coerceIn(-10,10))}
            val old=db.withTransaction { val paths=dao.allItems().flatMap{listOf(it.imagePath,it.originalPath)}+dao.allOutfits().map{it.imagePath}
                dao.clearItems();dao.clearOutfits();dao.clearWear();dao.clearPreferences()
                items.forEach{dao.putItem(it)};outfits.forEach{dao.putOutfit(it)};wear.forEach{dao.putWear(it)};preferences.forEach{dao.putPreference(it)};paths
            }
            committed=true;old.filter{it.startsWith(target.absolutePath+File.separator)}.forEach{File(it).delete()}
            return settings
        } finally { if(!committed)installed.forEach{it.delete()};stage.deleteRecursively() }
    }
}
