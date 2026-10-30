package dev.closet.processing

import android.graphics.Bitmap
import android.graphics.Color
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[35])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class PhotoProcessingTest {
    @Test fun rotationAndFlipPreserveCorrespondingOriginalPixels() {
        val b=Bitmap.createBitmap(2,3,Bitmap.Config.ARGB_8888);b.setPixel(0,0,Color.RED)
        val r=PhotoProcessing.transform(b,1);assertEquals(3,r.width);assertEquals(2,r.height);assertEquals(Color.RED,r.getPixel(2,0))
        assertEquals(Color.RED,PhotoProcessing.transform(b,flip=true).getPixel(1,0))
        assertTrue(b.sameAs(PhotoProcessing.transform(r,-1)))
    }
    @Test fun cleanupRemovesSpecksFillsTinyHolesAndKeepsOriginalAligned() {
        val cut=Bitmap.createBitmap(100,100,Bitmap.Config.ARGB_8888)
        for(y in 20..79)for(x in 25..74)cut.setPixel(x,y,Color.BLUE)
        cut.setPixel(50,50,Color.TRANSPARENT);cut.setPixel(3,3,Color.RED)
        val original=Bitmap.createBitmap(100,100,Bitmap.Config.ARGB_8888).apply{eraseColor(Color.BLUE)}
        val (raw,clean)=PhotoProcessing.clean(original,cut)
        assertEquals(raw.width,clean.width);assertEquals(raw.height,clean.height)
        assertEquals(0,Color.alpha(clean.getPixel(0,0)));assertEquals(Color.BLUE,clean.getPixel(clean.width/2,clean.height/2))
        assertFalse(PhotoProcessing.pixels(clean).any{Color.red(it)>200&&Color.alpha(it)>0})
    }
}
