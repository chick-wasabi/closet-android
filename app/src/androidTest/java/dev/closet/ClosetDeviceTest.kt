package dev.closet

import android.graphics.Bitmap
import android.graphics.Color
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.closet.data.*
import dev.closet.imaging.ImageStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class ClosetDeviceTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    @Test fun navigationAndAddEntryPointsWork() {
        compose.onNodeWithText("Add piece").performClick()
        compose.onNodeWithText("Choose photos").assertIsDisplayed()
        compose.onNodeWithText("Take photo").assertIsDisplayed()
        compose.onNodeWithText("A new piece").assertIsDisplayed()
        // Close the dialog with Back, then verify both secondary screens.
        androidx.test.espresso.Espresso.pressBack()
        compose.onNodeWithText("Style", useUnmergedTree = true).performClick()
        compose.onNodeWithText("STUDIO").assertIsDisplayed()
        compose.onNodeWithText("Shuffle").performClick()
        compose.onNodeWithText("Add at least one top, bottom, and pair of shoes matching these filters.").assertIsDisplayed()
        compose.onNodeWithText("Saved", useUnmergedTree = true).performClick()
        compose.onNodeWithText("LOOKBOOK").assertIsDisplayed()
    }

    @Test fun cutoutBrushAndMetadataSaveToCloset() {
        lateinit var vm: dev.closet.ui.ClosetViewModel
        val store = ImageStore(ApplicationProvider.getApplicationContext())
        val bitmap = Bitmap.createBitmap(120, 160, Bitmap.Config.ARGB_8888).apply { eraseColor(Color.BLUE) }
        val original = store.write(bitmap)
        val cutout = store.write(bitmap)
        val id = "device-test-${System.nanoTime()}"
        compose.runOnIdle {
            vm = androidx.lifecycle.ViewModelProvider(compose.activity)[dev.closet.ui.ClosetViewModel::class.java]
            vm.edit(ClothingItem(id, imagePath = cutout, originalPath = original))
        }
        try {
            compose.waitUntil(15000) { vm.draft != null && !vm.busy }
            compose.onNodeWithText("Name (optional)").performScrollTo().performTextInput("Test blue top")
            compose.onNodeWithText("Brush", substring = false).performScrollTo().performClick()
            compose.onNodeWithTag("cutoutCanvas").performScrollTo().performTouchInput {
                down(center); moveTo(center + androidx.compose.ui.geometry.Offset(20f, 20f)); up()
            }
            compose.onNodeWithText("Undo").performScrollTo().performClick()
            compose.onNodeWithText("Save piece").performScrollTo().performClick()
            compose.waitUntil(15000) { vm.items.value.any { it.id == id } && !vm.busy }
            compose.onNodeWithText("Test blue top").assertIsDisplayed()
            assertEquals("top", vm.items.value.first { it.id == id }.category)
        } finally {
            compose.runOnIdle { vm.items.value.firstOrNull { it.id == id }?.let(vm::delete) }
            compose.waitUntil(15000) { !vm.busy }
            File(original).delete(); File(cutout).delete()
        }
    }

    @Test fun roomPersistsItemsAndOutfitSnapshots() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val name = "test-${System.nanoTime()}.db"
        var db = Room.databaseBuilder(context, ClosetDatabase::class.java, name).build()
        try {
            val item = ClothingItem("one", name = "Blue shirt", imagePath = "local.png", originalPath = "original.png")
            db.closet().putItem(item)
            db.closet().putOutfit(SavedOutfit("look", "Weekend", "one", "snapshot.png", true))
            db.close()
            db = Room.databaseBuilder(context, ClosetDatabase::class.java, name).build()
            assertEquals("Blue shirt", db.closet().observeItems().first().single().name)
            db.closet().deleteItem(item)
            assertTrue(db.closet().observeItems().first().isEmpty())
            assertTrue(db.closet().observeOutfits().first().single().favorite)
        } finally { db.close(); context.deleteDatabase(name) }
    }

    @Test fun stackedPngHasTransparentBackgroundAndCorrectPieceOrder() {
        val store = ImageStore(ApplicationProvider.getApplicationContext())
        val created = mutableListOf<String>()
        try {
            val colors = listOf("hat" to Color.RED, "top" to Color.GREEN, "bottom" to Color.BLUE, "shoes" to Color.YELLOW)
            val pieces = colors.map { (category, color) ->
                val bitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888).apply { eraseColor(color) }
                val path = store.write(bitmap); created += path
                ClothingItem(category, category = category, imagePath = path, originalPath = path)
            }
            val image = store.render(pieces)
            assertEquals(1000, image.width); assertEquals(1600, image.height)
            assertEquals(0, Color.alpha(image.getPixel(0, 0)))
            assertEquals(Color.RED, image.getPixel(500, 100))
            assertEquals(Color.GREEN, image.getPixel(500, 480))
            assertEquals(Color.BLUE, image.getPixel(500, 1000))
            assertEquals(Color.YELLOW, image.getPixel(500, 1480))
            val path = store.write(image); created += path
            val decoded = store.read(path)
            assertEquals(0, Color.alpha(decoded.getPixel(0, 0)))
        } finally { created.forEach { File(it).delete() } }
    }
}
