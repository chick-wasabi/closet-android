package dev.closet.ui

import android.app.Application
import android.graphics.*
import androidx.test.core.app.ApplicationProvider
import dev.closet.data.ClothingItem
import dev.closet.imaging.ImageStore
import java.io.File
import java.util.concurrent.atomic.AtomicInteger

/** Illustrated example wardrobe for documentation only; never packaged as app records. */
class ReviewFixtures {
    private val app = ApplicationProvider.getApplicationContext<Application>()
    private val images = ImageStore(app)
    val loaded = AtomicInteger()
    init {
        coil.Coil.setImageLoader(coil.ImageLoader.Builder(app).dispatcher(kotlinx.coroutines.Dispatchers.Unconfined).eventListenerFactory {
            object : coil.EventListener {
                override fun onSuccess(request: coil.request.ImageRequest, result: coil.request.SuccessResult) { loaded.incrementAndGet() }
            }
        }.build())
    }
    val items = listOf(
        item("tee", "Pink tee", "top", "pink", Color.rgb(209, 124, 155)),
        item("jeans", "Everyday jeans", "bottom", "blue", Color.rgb(91, 111, 160)),
        item("shoes", "Cream sneakers", "shoes", "cream", Color.rgb(214, 190, 170)),
        item("sweater", "Lilac sweater", "top", "purple", Color.rgb(170, 147, 197))
    )
    val outfitPath = images.write(images.render(items.take(3)), "review-outfit")
    private fun item(id: String, name: String, category: String, color: String, ink: Int): ClothingItem {
        val bitmap = Bitmap.createBitmap(480, 600, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = ink }
        val path = Path()
        when(category) {
            "top" -> {
                path.moveTo(145f,80f);path.lineTo(195f,55f);path.cubicTo(212f,100f,268f,100f,285f,55f)
                path.lineTo(335f,80f);path.lineTo(430f,205f);path.lineTo(360f,260f);path.lineTo(325f,215f)
                path.lineTo(325f,530f);path.lineTo(155f,530f);path.lineTo(155f,215f);path.lineTo(120f,260f);path.lineTo(50f,205f);path.close()
            }
            "bottom" -> {
                path.moveTo(150f,45f);path.lineTo(330f,45f);path.lineTo(360f,555f);path.lineTo(260f,555f)
                path.lineTo(240f,210f);path.lineTo(220f,555f);path.lineTo(120f,555f);path.close()
            }
            else -> {
                path.moveTo(105f,160f);path.lineTo(225f,160f);path.lineTo(235f,290f);path.lineTo(60f,290f);path.lineTo(60f,255f);path.close()
                path.moveTo(255f,320f);path.lineTo(375f,320f);path.lineTo(420f,415f);path.lineTo(420f,450f);path.lineTo(245f,450f);path.close()
            }
        }
        canvas.drawPath(path, paint)
        paint.color = Color.argb(95,255,255,255); paint.style = Paint.Style.STROKE; paint.strokeWidth = 5f
        if(category == "bottom") canvas.drawLine(157f,72f,323f,72f,paint)
        else if(category == "top") canvas.drawLine(164f,505f,316f,505f,paint)
        val file = File(app.cacheDir, "review-$id.png")
        file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG,100,it) }
        return ClothingItem(id, name, category, color, "casual", "all", file.path, file.path)
    }
}
