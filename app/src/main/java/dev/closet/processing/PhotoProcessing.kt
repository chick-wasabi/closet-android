package dev.closet.processing

import android.graphics.*
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.label.ImageLabeling
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.tasks.await

object PhotoProcessing {
    fun pixels(bitmap: Bitmap) = IntArray(bitmap.width*bitmap.height).also { bitmap.getPixels(it,0,bitmap.width,0,0,bitmap.width,bitmap.height) }
    fun transform(bitmap: Bitmap, turns: Int = 0, flip: Boolean = false): Bitmap {
        val matrix = Matrix().apply { postRotate(turns*90f); if(flip) postScale(-1f,1f) }
        return Bitmap.createBitmap(bitmap,0,0,bitmap.width,bitmap.height,matrix,true).copy(Bitmap.Config.ARGB_8888,true)
    }
    suspend fun labels(bitmap: Bitmap): List<Pair<String,Float>> {
        val client=ImageLabeling.getClient(ImageLabelerOptions.DEFAULT_OPTIONS)
        return try { client.process(InputImage.fromBitmap(bitmap,0)).await().map { it.text to it.confidence } } finally { client.close() }
    }
    suspend fun labelText(bitmap: Bitmap): String {
        val client=TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        return try { client.process(InputImage.fromBitmap(bitmap,0)).await().text.lineSequence().filter { it.isNotBlank() }.take(4).joinToString(" · ").take(120) } finally { client.close() }
    }
    /** Same crop/scale on both images keeps the restore brush exactly aligned. */
    fun clean(original: Bitmap, cutout: Bitmap, tone: Boolean = false): Pair<Bitmap,Bitmap> {
        val w=cutout.width; val h=cutout.height; val data=pixels(cutout); val raw=pixels(original)
        require(original.width==w && original.height==h) { "Original and cutout must stay aligned." }
        val visited=BooleanArray(data.size); val queue=IntArray(data.size)
        var largest=IntArray(0)
        fun neighbors(p: Int, visit: (Int)->Unit) {
            val x=p%w; if(x>0)visit(p-1);if(x<w-1)visit(p+1);if(p>=w)visit(p-w);if(p<data.size-w)visit(p+w)
        }
        for(i in data.indices) if(!visited[i] && data[i] ushr 24 > 100) {
            var head=0;var tail=1;queue[0]=i;visited[i]=true
            while(head<tail) {
                val p=queue[head++]
                neighbors(p){n->if(!visited[n]&&data[n] ushr 24>100){visited[n]=true;queue[tail++]=n}}
            }
            if(tail>largest.size) largest=queue.copyOf(tail)
        }
        require(largest.isNotEmpty()) { "No visible item. Keep the original or restore with the brush." }
        val keep=BooleanArray(data.size);largest.forEach{keep[it]=true}
        for(i in data.indices)if(!keep[i])data[i]=0
        visited.fill(false)
        for(i in data.indices) if(!keep[i] && !visited[i]) {
            var head=0;var tail=1;queue[0]=i;visited[i]=true;var touchesEdge=false
            while(head<tail){val p=queue[head++];val x=p%w;val y=p/w
                if(x==0||y==0||x==w-1||y==h-1)touchesEdge=true
                neighbors(p){n->if(!keep[n]&&!visited[n]){visited[n]=true;queue[tail++]=n}}
            }
            if(!touchesEdge&&tail<=maxOf(4,data.size/4000)) for(j in 0 until tail){val p=queue[j];data[p]=raw[p] or (255 shl 24);keep[p]=true}
        }
        val soft=data.copyOf()
        for(y in 1 until h-1) for(x in 1 until w-1) { val i=y*w+x
            if(!keep[i]) continue
            var alpha=0; for(dy in -1..1) for(dx in -1..1) alpha+=data[i+dy*w+dx] ushr 24
            soft[i]=(data[i] and 0xFFFFFF) or ((alpha/9) shl 24)
        }
        val left=largest.minOf { it%w };val right=largest.maxOf { it%w };val top=largest.minOf { it/w };val bottom=largest.maxOf { it/w }
        val pad=(maxOf(right-left,bottom-top)*.06).toInt().coerceAtLeast(2)
        // Uniform transparent padding, even when item touches a photo edge.
        val cw=right-left+1+pad*2; val ch=bottom-top+1+pad*2
        fun normalize(src: Bitmap, paint: Paint?): Bitmap {
            val frame=Bitmap.createBitmap(cw,ch,Bitmap.Config.ARGB_8888)
            Canvas(frame).drawBitmap(src,(pad-left).toFloat(),(pad-top).toFloat(),paint)
            val scale=960f/maxOf(cw,ch)
            return Bitmap.createScaledBitmap(frame,(cw*scale).toInt().coerceAtLeast(1),(ch*scale).toInt().coerceAtLeast(1),true)
        }
        val cleaned=Bitmap.createBitmap(soft,w,h,Bitmap.Config.ARGB_8888)
        val paint=if(tone) Paint().apply { colorFilter=ColorMatrixColorFilter(ColorMatrix(floatArrayOf(1.06f,0f,0f,0f,3f,0f,1.06f,0f,0f,3f,0f,0f,1.06f,0f,3f,0f,0f,0f,1f,0f))) } else null
        return normalize(original,null) to normalize(cleaned,paint)
    }
}

/** Reserved extension only; no cloud implementation or network permission. */
interface GarmentCleanupProvider { suspend fun clean(original: Bitmap, mask: Bitmap): Bitmap }
