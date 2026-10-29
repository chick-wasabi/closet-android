package dev.closet.imaging

import android.content.*
import android.graphics.*
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import androidx.exifinterface.media.ExifInterface
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.segmentation.subject.SubjectSegmentation
import com.google.mlkit.vision.segmentation.subject.SubjectSegmenterOptions
import dev.closet.data.ClothingItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID
import kotlin.math.roundToInt

class ImageStore(private val context: Context) {
    private val directory = File(context.filesDir, "images").apply { mkdirs() }
    fun file(name: String) = File(directory, "$name.png")
    fun read(path: String): Bitmap = requireNotNull(BitmapFactory.decodeFile(path)) { "This image could not be opened." }
    fun write(bitmap: Bitmap, name: String = UUID.randomUUID().toString()): String {
        val file = file(name)
        try { file.outputStream().use { check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)) } }
        catch (e: Exception) { file.delete(); throw e }
        return file.absolutePath
    }
    suspend fun import(uri: Uri): Bitmap = withContext(Dispatchers.IO) {
        if (Build.VERSION.SDK_INT >= 28) {
            ImageDecoder.decodeBitmap(ImageDecoder.createSource(context.contentResolver, uri)) { decoder, info, _ ->
                val ratio = 1280f / maxOf(info.size.width, info.size.height).coerceAtLeast(1280)
                decoder.setTargetSize((info.size.width * ratio).roundToInt().coerceAtLeast(1), (info.size.height * ratio).roundToInt().coerceAtLeast(1))
                decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                decoder.isMutableRequired = true
            }.copy(Bitmap.Config.ARGB_8888, true)
        } else {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            context.contentResolver.openInputStream(uri).use { BitmapFactory.decodeStream(it, null, bounds) }
            require(bounds.outWidth > 0) { "Unsupported image. Try a JPEG or PNG." }
            var sample = 1
            while (maxOf(bounds.outWidth, bounds.outHeight) / sample > 1600) sample *= 2
            val bitmap = context.contentResolver.openInputStream(uri).use { BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sample }) }
                ?: error("Could not read image")
            val matrix = context.contentResolver.openInputStream(uri).use { input ->
                val exif = ExifInterface(requireNotNull(input))
                Matrix().apply {
                    postRotate(exif.rotationDegrees.toFloat())
                    if (exif.isFlipped) postScale(-1f, 1f)
                }
            }
            Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true).copy(Bitmap.Config.ARGB_8888, true)
        }
    }
    suspend fun cutout(original: Bitmap): Bitmap {
        val client = SubjectSegmentation.getClient(SubjectSegmenterOptions.Builder().enableForegroundBitmap().build())
        return try {
            val result = client.process(InputImage.fromBitmap(original, 0)).await()
            val bitmap = requireNotNull(result.foregroundBitmap) { "No subject was found." }
            // An empty output is a failure, not a successfully imported invisible item.
            val pixels = IntArray(bitmap.width * bitmap.height)
            bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
            require(pixels.any { Color.alpha(it) > 100 }) { "No subject was found." }
            bitmap.copy(Bitmap.Config.ARGB_8888, true)
        } finally { client.close() }
    }

    fun render(items: List<ClothingItem>): Bitmap {
        require(items.isNotEmpty()) { "Choose some pieces first." }
        val output = Bitmap.createBitmap(1000, 1600, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        val slots = mapOf("hat" to RectF(240f, 20f, 760f, 230f), "top" to RectF(170f, 235f, 830f, 730f),
            "jacket" to RectF(185f, 245f, 815f, 720f), "bottom" to RectF(240f, 740f, 760f, 1350f), "shoes" to RectF(220f, 1370f, 780f, 1580f))
        for (item in dev.closet.domain.OutfitEngine.ordered(items)) {
            val source = read(item.imagePath)
            val cropped = trim(source)
            val slot = slots[item.category] ?: continue
            val scale = minOf(slot.width() / cropped.width, slot.height() / cropped.height)
            val w = cropped.width * scale; val h = cropped.height * scale
            val dest = RectF(slot.centerX() - w / 2, slot.centerY() - h / 2, slot.centerX() + w / 2, slot.centerY() + h / 2)
            canvas.drawBitmap(cropped, null, dest, Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG))
            if (cropped !== source) cropped.recycle()
            source.recycle()
        }
        return output
    }
    private fun trim(bitmap: Bitmap): Bitmap {
        val pixels = IntArray(bitmap.width * bitmap.height)
        bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
        var left = bitmap.width; var top = bitmap.height; var right = -1; var bottom = -1
        for (y in 0 until bitmap.height) for (x in 0 until bitmap.width) if (Color.alpha(pixels[y * bitmap.width + x]) > 15) {
            left = minOf(left, x); right = maxOf(right, x); top = minOf(top, y); bottom = maxOf(bottom, y)
        }
        require(right >= left) { "An item is entirely transparent. Restore it in the cutout editor." }
        return Bitmap.createBitmap(bitmap, left, top, right - left + 1, bottom - top + 1)
    }
    suspend fun share(path: String) {
        val dir = File(context.cacheDir, "shared").apply { mkdirs() }
        dir.listFiles()?.filter { System.currentTimeMillis() - it.lastModified() > 86400000 }?.forEach { it.delete() }
        val file = File(dir, "Closet-${UUID.randomUUID()}.png")
        withContext(Dispatchers.IO) { File(path).copyTo(file) }
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.files", file)
        context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
            type = "image/png"; putExtra(Intent.EXTRA_STREAM, uri)
            clipData = ClipData.newRawUri("Outfit", uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }, "Share outfit").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }
    fun export(path: String) {
        val name = "Closet-${System.currentTimeMillis()}.png"
        if (Build.VERSION.SDK_INT >= 29) {
            val values = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, name); put(MediaStore.Images.Media.MIME_TYPE, "image/png")
                put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/Closet"); put(MediaStore.Images.Media.IS_PENDING, 1)
            }
            val uri = requireNotNull(context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values))
            try {
                context.contentResolver.openOutputStream(uri).use { output -> File(path).inputStream().use { it.copyTo(requireNotNull(output)) } }
                values.clear(); values.put(MediaStore.Images.Media.IS_PENDING, 0)
                context.contentResolver.update(uri, values, null, null)
            } catch (e: Exception) { context.contentResolver.delete(uri, null, null); throw e }
        } else {
            @Suppress("DEPRECATION") val target = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "Closet/$name")
            target.parentFile?.mkdirs(); File(path).copyTo(target)
            android.media.MediaScannerConnection.scanFile(context, arrayOf(target.absolutePath), arrayOf("image/png"), null)
        }
    }
}
