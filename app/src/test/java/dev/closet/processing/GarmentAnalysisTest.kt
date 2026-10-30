package dev.closet.processing

import org.junit.Assert.*
import org.junit.Test

class GarmentAnalysisTest {
    @Test fun dominantColorIgnoresTransparentEdgesAndDoesNotAverageColors() {
        val pixels=IntArray(100){if(it<60) 0xff151515.toInt() else if(it<90) 0x80ff0000.toInt() else 0xffff0000.toInt()}
        assertEquals("black",GarmentAnalysis.color(pixels));assertEquals("neutral",GarmentAnalysis.color(IntArray(20)))
        assertEquals("blue",GarmentAnalysis.color(IntArray(30){0xff417dc8.toInt()}))
    }
    @Test fun labelSuggestionsFallBackWhenConfidenceIsLow() {
        assertEquals("shoes",GarmentAnalysis.category(listOf("Footwear" to .92f),100,200))
        assertEquals("bottom",GarmentAnalysis.category(listOf("Hat" to .2f),70,200))
        assertEquals("smart" to "all",GarmentAnalysis.defaults("shirt"))
    }
    @Test fun solidAndStripedPatternsAreDeterministic() {
        assertEquals("solid",GarmentAnalysis.pattern(IntArray(10000){0xff336699.toInt()},100,100))
        val stripe=IntArray(10000){if((it%100)/8%2==0) 0xff000000.toInt() else 0xffffffff.toInt()}
        assertEquals("striped",GarmentAnalysis.pattern(stripe,100,100))
    }
    @Test fun uprightShouldersStayUprightAndUpsideDownShirtRotates() {
        val w=60;val h=80
        val shirt=IntArray(w*h){i->val x=i%w;val y=i/w;if((y in 10..30&&x in 3..56)||(y in 31..70&&x in 18..42)) -1 else 0}
        assertEquals(0,GarmentAnalysis.orientation(shirt,w,h,"top"))
        assertEquals(2,GarmentAnalysis.orientation(shirt.reversedArray(),w,h,"top"))
        val sideways=IntArray(w*h);for(y in 0 until h)for(x in 0 until w)sideways[x*h+h-1-y]=shirt[y*w+x]
        assertEquals(3,GarmentAnalysis.orientation(sideways,h,w,"top"))
    }
    @Test fun ambiguousShapesAndAccessoriesStayUnchanged() {
        assertEquals(0,GarmentAnalysis.orientation(IntArray(10000){-1},100,100,"top"))
        assertEquals(0,GarmentAnalysis.orientation(IntArray(400){-1},20,20,"accessory"))
        val shoe=IntArray(200*70){-1};assertEquals(0,GarmentAnalysis.orientation(shoe,200,70,"shoes"))
    }
}
