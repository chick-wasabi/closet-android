package dev.closet.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "items")
data class ClothingItem(
    @PrimaryKey val id: String,
    val name: String = "",
    val category: String = "top",
    val color: String = "neutral",
    val formality: String = "casual",
    val season: String = "all",
    val imagePath: String,
    val originalPath: String,
    val createdAt: Long = System.currentTimeMillis(),
    @ColumnInfo(defaultValue = "''") val subcategory: String = "",
    @ColumnInfo(defaultValue = "'solid'") val pattern: String = "solid",
    @ColumnInfo(defaultValue = "''") val brand: String = "",
    @ColumnInfo(defaultValue = "''") val autoFields: String = "",
    @ColumnInfo(defaultValue = "0") val favorite: Boolean = false,
    @ColumnInfo(defaultValue = "0") val timesWorn: Int = 0,
    @ColumnInfo(defaultValue = "0") val lastWorn: Long = 0
)

@Entity(tableName = "outfits")
data class SavedOutfit(
    @PrimaryKey val id: String,
    val name: String,
    val itemIds: String,
    val imagePath: String,
    val favorite: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "wear_log")
data class WearEntry(@PrimaryKey val id: String, val outfitId: String, val itemIds: String, val wornAt: Long)

@Entity(tableName = "pair_preferences")
data class PairPreference(@PrimaryKey val pair: String, val value: Int)

@Dao
interface ClosetDao {
    @Query("SELECT * FROM items ORDER BY createdAt DESC") fun observeItems(): Flow<List<ClothingItem>>
    @Query("SELECT * FROM outfits ORDER BY createdAt DESC") fun observeOutfits(): Flow<List<SavedOutfit>>
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun putItem(item: ClothingItem)
    @Delete suspend fun deleteItem(item: ClothingItem)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun putOutfit(outfit: SavedOutfit)
    @Delete suspend fun deleteOutfit(outfit: SavedOutfit)
    @Query("SELECT * FROM items") suspend fun allItems(): List<ClothingItem>
    @Query("SELECT * FROM outfits") suspend fun allOutfits(): List<SavedOutfit>
    @Query("SELECT * FROM wear_log ORDER BY wornAt DESC") fun observeWear(): Flow<List<WearEntry>>
    @Query("SELECT * FROM pair_preferences") fun observePreferences(): Flow<List<PairPreference>>
    @Query("SELECT * FROM pair_preferences") suspend fun allPreferences(): List<PairPreference>
    @Query("SELECT * FROM wear_log") suspend fun allWear(): List<WearEntry>
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun putWear(entry: WearEntry)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun putPreference(entry: PairPreference)
    @Query("DELETE FROM items") suspend fun clearItems()
    @Query("DELETE FROM outfits") suspend fun clearOutfits()
    @Query("DELETE FROM wear_log") suspend fun clearWear()
    @Query("DELETE FROM pair_preferences") suspend fun clearPreferences()
}

@Database(entities = [ClothingItem::class, SavedOutfit::class, WearEntry::class, PairPreference::class], version = 2, exportSchema = true)
abstract class ClosetDatabase : RoomDatabase() {
    abstract fun closet(): ClosetDao
    companion object {
        val MIGRATION_1_2 = object : androidx.room.migration.Migration(1, 2) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                for ((name, type, default) in listOf(
                    Triple("subcategory", "TEXT", "''"), Triple("pattern", "TEXT", "'solid'"),
                    Triple("brand", "TEXT", "''"), Triple("autoFields", "TEXT", "''"),
                    Triple("favorite", "INTEGER", "0"), Triple("timesWorn", "INTEGER", "0"), Triple("lastWorn", "INTEGER", "0")
                )) db.execSQL("ALTER TABLE items ADD COLUMN `$name` $type NOT NULL DEFAULT $default")
                db.execSQL("CREATE TABLE IF NOT EXISTS wear_log (id TEXT NOT NULL PRIMARY KEY, outfitId TEXT NOT NULL, itemIds TEXT NOT NULL, wornAt INTEGER NOT NULL)")
                db.execSQL("CREATE TABLE IF NOT EXISTS pair_preferences (pair TEXT NOT NULL PRIMARY KEY, value INTEGER NOT NULL)")
            }
        }
    }
}
