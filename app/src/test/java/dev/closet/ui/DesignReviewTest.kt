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
@Config(sdk = [35], qualifiers = "w449dp-h988dp-mdpi")
class DesignReviewTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    private fun render(name: String, dark: Boolean = false, page: Int = 0, fontScale: Float = 1f, showNavigation: Boolean = true, ready: () -> Boolean = { true }, content: @Composable () -> Unit) {
        compose.setContent {
            ClosetTheme(dark) {
              val density = androidx.compose.ui.platform.LocalDensity.current
              androidx.compose.runtime.CompositionLocalProvider(androidx.compose.ui.platform.LocalDensity provides androidx.compose.ui.unit.Density(density.density, fontScale)) {
                Scaffold(bottomBar = { if(showNavigation) ClosetNavigation(page, true) {} }, floatingActionButton = { if(showNavigation && page == 0) AddPieceAction {} }) { insets ->
                    Box(Modifier.fillMaxSize().padding(insets)) { content() }
                }
              }
            }
        }
        compose.waitForIdle()
        compose.waitUntil(15000) { ready() }
        val folder = File("build/outputs/design-review").apply { mkdirs() }
        compose.onRoot().captureRoboImage(File(folder, "$name.png").path)
    }

    @Test fun closetEmptyLight() = render("closet-empty-light") { Wardrobe(emptyList(), {}, {}, {}) }

    @Test
    @Config(qualifiers = "w360dp-h800dp-mdpi")
    fun closetNarrow() {
        render("closet-narrow") { Wardrobe(emptyList(), {}, {}, {}) }
        compose.onNodeWithTag("wardrobe-grid").performScrollToNode(hasText("Add your first piece"))
        compose.onNodeWithText("Add your first piece").assertIsDisplayed()
    }

    @Test
    @Config(qualifiers = "w360dp-h800dp-mdpi")
    fun closetLargeText() {
        render("closet-large-text", fontScale = 1.5f) { Wardrobe(emptyList(), {}, {}, {}) }
        compose.onNodeWithTag("wardrobe-grid").performScrollToNode(hasText("Add your first piece"))
        compose.onNodeWithText("Add your first piece").assertIsDisplayed()
    }

    @Test
    @Config(qualifiers = "w800dp-h360dp-mdpi")
    fun closetLandscape() {
        render("closet-landscape") { Wardrobe(emptyList(), {}, {}, {}) }
        compose.onNodeWithTag("wardrobe-grid").performScrollToNode(hasText("Add your first piece"))
        compose.onNodeWithText("Add your first piece").assertIsDisplayed()
    }

    @Test fun closetPopulatedDark() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val bitmap = Bitmap.createBitmap(480, 600, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = android.graphics.Color.rgb(156, 180, 241) }
        val path = android.graphics.Path().apply {
            moveTo(145f, 80f); lineTo(195f, 55f); cubicTo(212f, 100f, 268f, 100f, 285f, 55f)
            lineTo(335f, 80f); lineTo(430f, 205f); lineTo(360f, 260f); lineTo(325f, 215f)
            lineTo(325f, 530f); lineTo(155f, 530f); lineTo(155f, 215f); lineTo(120f, 260f); lineTo(50f, 205f); close()
        }
        canvas.drawPath(path, paint)
        val file = File(app.cacheDir, "preview-top.png")
        file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        val loaded = java.util.concurrent.atomic.AtomicInteger()
        val loadError = java.util.concurrent.atomic.AtomicReference<Throwable>()
        coil.Coil.setImageLoader(coil.ImageLoader.Builder(app).dispatcher(kotlinx.coroutines.Dispatchers.Unconfined).eventListenerFactory {
            object : coil.EventListener {
                override fun onSuccess(request: coil.request.ImageRequest, result: coil.request.SuccessResult) { loaded.incrementAndGet() }
                override fun onError(request: coil.request.ImageRequest, result: coil.request.ErrorResult) { loadError.set(result.throwable) }
            }
        }.build())
        val items = listOf(
            ClothingItem("preview1", "Cobalt everyday tee", "top", "blue", "casual", "all", file.path, file.path),
            ClothingItem("preview2", "A very long item name that needs two lines", "top", "blue", "casual", "summer", file.path, file.path)
        )
        render("closet-populated-dark", dark = true, ready = { check(loadError.get() == null) { "Preview image load failed: ${loadError.get()}" }; loaded.get() >= 2 }) { Wardrobe(items, {}, {}, {}) }
        compose.onNodeWithText("Cobalt everyday tee").assertIsDisplayed()
    }

    @Test fun studioLight() {
        val vm = ViewModelProvider(compose.activity)[ClosetViewModel::class.java]
        render("studio-light", page = 1) { Builder(vm, emptyList()) }
        compose.onNodeWithText("STUDIO").assertIsDisplayed()
    }

    @Test fun savedDark() {
        val vm = ViewModelProvider(compose.activity)[ClosetViewModel::class.java]
        render("lookbook-dark", dark = true, page = 2) { Saved(emptyList(), vm) }
        compose.onNodeWithText("LOOKBOOK").assertIsDisplayed()
    }

    @Test fun editorLight() {
        val original = Bitmap.createBitmap(320, 400, Bitmap.Config.ARGB_8888).apply { eraseColor(android.graphics.Color.rgb(143, 164, 211)) }
        val draft = ItemDraft(null, original, original)
        render("editor-light", showNavigation = false) { ItemEditor(draft, false, { _, _, _, _, _, _ -> }, {}) }
        compose.onNodeWithText("Save piece").assertIsDisplayed()
    }
}
