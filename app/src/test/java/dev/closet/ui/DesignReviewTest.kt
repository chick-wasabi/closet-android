package dev.closet.ui

import android.app.Application
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.lifecycle.ViewModelProvider
import androidx.test.core.app.ApplicationProvider
import com.github.takahirom.roborazzi.captureRoboImage
import dev.closet.data.ClothingItem
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

/** Actual Compose renders on the host; no emulator, network, or real wardrobe data. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w393dp-h873dp-mdpi")
class DesignReviewTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    private fun render(name: String, dark: Boolean = false, page: Int = 0, fontScale: Float = 1f, showNavigation: Boolean = true, showAddAction: Boolean = false, beforeCapture: () -> Unit = {}, ready: () -> Boolean = { true }, content: @Composable () -> Unit) {
        compose.setContent {
            ClosetTheme(dark) {
              val density = androidx.compose.ui.platform.LocalDensity.current
              androidx.compose.runtime.CompositionLocalProvider(androidx.compose.ui.platform.LocalDensity provides androidx.compose.ui.unit.Density(density.density, fontScale)) {
                Scaffold(bottomBar = { if(showNavigation) ClosetNavigation(page, true) {} }, floatingActionButton = { if(showNavigation && showAddAction && page == 0) AddPieceAction {} }) { insets ->
                    Box(Modifier.fillMaxSize().padding(insets)) { content() }
                }
              }
            }
        }
        compose.waitForIdle()
        beforeCapture()
        compose.waitUntil(15000) { ready() }
        val folder = File("build/outputs/design-review").apply { mkdirs() }
        compose.onRoot().captureRoboImage(File(folder, "$name.png").path)
    }

    @Test fun closetEmptyLight() = render("closet-empty-light") { Wardrobe(emptyList(), {}, {}, {}) }

    @Test
    @Config(qualifiers = "w360dp-h800dp-mdpi")
    fun closetNarrow() {
        render("closet-narrow") { Wardrobe(emptyList(), {}, {}, {}) }
        compose.onNodeWithTag("wardrobe-grid").performScrollToNode(hasText("Add clothes"))
        compose.onNodeWithText("Add clothes").assertIsDisplayed()
    }

    @Test
    @Config(qualifiers = "w360dp-h800dp-mdpi")
    fun closetLargeText() {
        render("closet-large-text", fontScale = 1.5f) { Wardrobe(emptyList(), {}, {}, {}) }
        compose.onNodeWithTag("wardrobe-grid").performScrollToNode(hasText("Add clothes"))
        compose.onNodeWithText("Add clothes").assertIsDisplayed()
    }

    @Test
    @Config(qualifiers = "w800dp-h360dp-mdpi")
    fun closetLandscape() {
        render("closet-landscape") { Wardrobe(emptyList(), {}, {}, {}) }
        compose.onNodeWithTag("wardrobe-grid").performScrollToNode(hasText("Add clothes"))
        compose.onNodeWithText("Add clothes").assertIsDisplayed()
    }

    @Test fun closetPopulatedDark() {
        val fixture = ReviewFixtures()
        render("closet-populated-dark", dark = true, showAddAction = true, ready = { fixture.loaded.get() >= fixture.items.size }) { Wardrobe(fixture.items, {}, {}, {}) }
        compose.onNodeWithText("Pink tee").assertIsDisplayed()
        singleLineTitle()
    }

    @Test fun closetPopulatedLight() {
        val fixture = ReviewFixtures()
        render("closet-populated-light", showAddAction = true, ready = { fixture.loaded.get() >= fixture.items.size }) { Wardrobe(fixture.items, {}, {}, {}) }
        singleLineTitle()
    }

    @Test fun studioLight() {
        val fixture = ReviewFixtures()
        val bitmap = dev.closet.imaging.ImageStore(ApplicationProvider.getApplicationContext()).read(fixture.outfitPath)
        render("studio-light", page = 1) { OutfitBuilder(fixture.items, bitmap, fixture.items.take(3), false, { _, _ -> }, {}, {}, { _, _ -> }) }
        singleLineTitle()
    }

    @Test fun savedDark() {
        val vm = ViewModelProvider(compose.activity)[ClosetViewModel::class.java]
        val fixture = ReviewFixtures()
        val outfit = dev.closet.data.SavedOutfit("sample", "Easy everyday", "tee,jeans,shoes", fixture.outfitPath, true)
        render("lookbook-dark", dark = true, page = 2, ready = { fixture.loaded.get() >= 1 }) { Saved(listOf(outfit), vm) }
        singleLineTitle()
    }

    @Test fun editorLight() {
        val fixture = ReviewFixtures()
        val item = fixture.items.first()
        val original = dev.closet.imaging.ImageStore(ApplicationProvider.getApplicationContext()).read(item.imagePath)
        val draft = ItemDraft(item, original, original)
        render("editor-light", showNavigation = false) { ItemEditor(draft, false, { _, _, _, _, _, _ -> }, {}) }
        compose.onNodeWithText("Save").assertIsDisplayed()
    }
    private fun singleLineTitle() {
        val layouts = mutableListOf<androidx.compose.ui.text.TextLayoutResult>()
        compose.onNodeWithTag("screen-title").performSemanticsAction(androidx.compose.ui.semantics.SemanticsActions.GetTextLayoutResult) { it(layouts) }
        org.junit.Assert.assertEquals(1, layouts.single().lineCount)
        org.junit.Assert.assertFalse("Title overflow: width=${layouts.single().didOverflowWidth}, height=${layouts.single().didOverflowHeight}, size=${layouts.single().size}, bottom=${layouts.single().getLineBottom(0)}, end=${layouts.single().getLineEnd(0)}", layouts.single().hasVisualOverflow)
    }

    @Test
    @Config(qualifiers = "w320dp-h640dp-mdpi")
    fun savedSmallPhoneLargeFont() {
        val vm = ViewModelProvider(compose.activity)[ClosetViewModel::class.java]
        render("saved-small-large-font", fontScale = 2f, page = 2) { Saved(emptyList(), vm) }
        singleLineTitle()
    }

    @Test
    @Config(qualifiers = "w320dp-h640dp-mdpi")
    fun outfitsSmallPhoneLargeFont() {
        val vm = ViewModelProvider(compose.activity)[ClosetViewModel::class.java]
        render("outfits-small-large-font", fontScale = 2f, page = 1) { Builder(vm, emptyList()) }
        singleLineTitle()
        compose.onNode(hasScrollToIndexAction()).performScrollToNode(hasText("Shuffle"))
        compose.onNodeWithText("Shuffle").assertIsDisplayed()
    }

    @Test
    @Config(qualifiers = "w600dp-h960dp-mdpi")
    fun closetWide() {
        render("closet-wide") { Wardrobe(emptyList(), {}, {}, {}) }
        singleLineTitle()
    }

}
