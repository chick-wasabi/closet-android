package dev.closet.data

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import dev.closet.imaging.ImageStore
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[35])
@org.robolectric.annotation.GraphicsMode(org.robolectric.annotation.GraphicsMode.Mode.NATIVE)
class MigrationBackupTest {
    private val context=ApplicationProvider.getApplicationContext<Context>()
    @Test fun versionOneRowsAndPhotosSurviveRealRoomMigration()=runBlocking {
        val name="migration-test.db";context.deleteDatabase(name)
        val file=context.getDatabasePath(name);file.parentFile!!.mkdirs()
        val schema=JSONObject(File("schemas/dev.closet.data.ClosetDatabase/1.json").readText()).getJSONObject("database")
        SQLiteDatabase.openOrCreateDatabase(file,null).use{db->
            val tables=schema.getJSONArray("entities")
            for(i in 0 until tables.length()) {val entity=tables.getJSONObject(i);db.execSQL(entity.getString("createSql").replace("\${TABLE_NAME}",entity.getString("tableName")))}
            db.execSQL("INSERT INTO items VALUES ('old','Blue shirt','top','blue','casual','all','cut.png','original.png',123)")
            db.execSQL("INSERT INTO outfits VALUES ('look','First outfit','old','outfit.png',1,456)")
            db.version=1
        }
        val room=Room.databaseBuilder(context,ClosetDatabase::class.java,name).addMigrations(ClosetDatabase.MIGRATION_1_2).build()
        try{
            val item=room.closet().allItems().single();assertEquals("old",item.id);assertEquals("cut.png",item.imagePath);assertEquals("original.png",item.originalPath)
            assertEquals("solid",item.pattern);assertEquals(0,item.timesWorn);assertEquals("",item.brand)
            assertTrue(room.closet().allOutfits().single().favorite);assertEquals("outfit.png",room.closet().allOutfits().single().imagePath)
        }finally{room.close();context.deleteDatabase(name)}
    }
    @Test fun completeBackupRoundTripPreservesDatabasePhotosAndSettings()=runBlocking {
        val db=Room.inMemoryDatabaseBuilder(context,ClosetDatabase::class.java).build()
        try {
            val bitmap=Bitmap.createBitmap(20,40,Bitmap.Config.ARGB_8888).apply{eraseColor(Color.BLUE)}
            val store=ImageStore(context);val image=store.write(bitmap);val original=store.write(bitmap)
            val item=ClothingItem("tee",name="Blue tee",imagePath=image,originalPath=original,brand="Brand M",timesWorn=2,lastWorn=100)
            db.closet().putItem(item);db.closet().putOutfit(SavedOutfit("o","Look","tee",store.write(bitmap),true))
            db.closet().putWear(WearEntry("w","o","tee",100));db.closet().putPreference(PairPreference("a|b",3))
            val zip=File(context.cacheDir,"roundtrip.zip");val backup=ClosetBackup(context,db)
            backup.export(Uri.fromFile(zip),AppSettings(theme="dark",hemisphere="south"));db.closet().clearItems()
            val restored=backup.restore(Uri.fromFile(zip));assertEquals("dark",restored.theme);assertEquals("south",restored.hemisphere)
            val actual=db.closet().allItems().single();assertEquals(item.copy(imagePath=actual.imagePath,originalPath=actual.originalPath),actual)
            assertEquals(20,store.read(actual.imagePath).width);assertEquals(1,db.closet().allWear().size);assertEquals(3,db.closet().allPreferences().single().value)
            assertTrue(db.closet().allOutfits().single().favorite)
        }finally{db.close()}
    }
    @Test fun maliciousAndIncompleteBackupsNeverClearTheLiveCloset()=runBlocking {
        val db=Room.inMemoryDatabaseBuilder(context,ClosetDatabase::class.java).build()
        try {
            db.closet().putItem(ClothingItem("safe",imagePath="safe.png",originalPath="raw.png"))
            for(name in listOf("../escape.png","database.json")) {
                val zip=File(context.cacheDir,"bad.zip")
                ZipOutputStream(zip.outputStream()).use{it.putNextEntry(ZipEntry(name));it.write("{}".toByteArray());it.closeEntry()}
                assertTrue(runCatching{ClosetBackup(context,db).restore(Uri.fromFile(zip))}.isFailure)
                assertEquals("safe",db.closet().allItems().single().id)
            }
        }finally{db.close()}
    }
}
