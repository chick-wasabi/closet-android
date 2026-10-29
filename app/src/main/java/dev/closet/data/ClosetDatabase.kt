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
    val createdAt: Long = System.currentTimeMillis()
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

@Dao
interface ClosetDao {
    @Query("SELECT * FROM items ORDER BY createdAt DESC") fun observeItems(): Flow<List<ClothingItem>>
    @Query("SELECT * FROM outfits ORDER BY createdAt DESC") fun observeOutfits(): Flow<List<SavedOutfit>>
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun putItem(item: ClothingItem)
    @Delete suspend fun deleteItem(item: ClothingItem)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun putOutfit(outfit: SavedOutfit)
    @Delete suspend fun deleteOutfit(outfit: SavedOutfit)
}

@Database(entities = [ClothingItem::class, SavedOutfit::class], version = 1, exportSchema = false)
abstract class ClosetDatabase : RoomDatabase() {
    abstract fun closet(): ClosetDao
}
