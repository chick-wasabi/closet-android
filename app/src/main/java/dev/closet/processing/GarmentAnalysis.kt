package dev.closet.processing

import kotlin.math.*

/** Deterministic, conservative guesses; never replaces the user's choices. */
object GarmentAnalysis {
    val palette = linkedMapOf("black" to intArrayOf(25,25,30), "white" to intArrayOf(240,240,240),
        "cream" to intArrayOf(230,215,180), "gray" to intArrayOf(130,130,135), "navy" to intArrayOf(30,45,80),
        "brown" to intArrayOf(115,75,45), "blue" to intArrayOf(65,125,200), "green" to intArrayOf(65,130,70),
        "red" to intArrayOf(190,40,40), "pink" to intArrayOf(220,130,165), "purple" to intArrayOf(130,70,170),
        "yellow" to intArrayOf(230,200,55), "orange" to intArrayOf(225,125,40))
    fun color(pixels: IntArray): String {
        // Quantized histogram avoids averaging red/blue into a nonexistent purple.
        val bins = pixels.asSequence().filter { (it ushr 24) >= 245 }.groupingBy {
            ((it shr 19 and 31) shl 10) or ((it shr 11 and 31) shl 5) or (it shr 3 and 31)
        }.eachCount()
        val bin = bins.maxWithOrNull(compareBy<Map.Entry<Int,Int>> { it.value }.thenBy { -it.key })?.key ?: return "neutral"
        val rgb = intArrayOf(((bin shr 10) and 31)*8+4, ((bin shr 5) and 31)*8+4, (bin and 31)*8+4)
        return palette.minBy { (_, v) -> v.indices.sumOf { (v[it]-rgb[it]).toDouble().pow(2) } }.key
    }
    fun bounds(pixels: IntArray,width: Int,height: Int): Pair<Int,Int> {
        var left=width;var right=-1;var top=height;var bottom=-1
        for(y in 0 until height)for(x in 0 until width)if(pixels[y*width+x] ushr 24>=180){left=minOf(left,x);right=maxOf(right,x);top=minOf(top,y);bottom=maxOf(bottom,y)}
        return if(right<left)width to height else right-left+1 to bottom-top+1
    }
    fun category(labels: List<Pair<String, Float>>, width: Int, height: Int): String {
        val known = listOf("pajamas","jacket","shoes","bottom","hat","top","accessory")
        val words = mapOf("pajamas" to listOf("pajama","sleepwear"), "jacket" to listOf("jacket","coat"),
            "shoes" to listOf("shoe","footwear","sneaker","sandal","boot"), "bottom" to listOf("jean","trouser","pants","shorts","skirt"),
            "hat" to listOf("hat","cap"), "top" to listOf("shirt","hoodie","sweater","jersey"), "accessory" to listOf("bag","scarf","belt"))
        val confident = labels.filter { it.second >= .55f }.sortedByDescending { it.second }
        for ((label, _) in confident) for (c in known) if(words.getValue(c).any { label.lowercase().contains(it) }) return c
        return if(height > width * 1.65) "bottom" else if(width > height * 2.4) "accessory" else "top"
    }
    val subcategories = listOf("t-shirt","shirt","hoodie","sweater","jeans","joggers","shorts","trousers","sneakers","formal shoes","sandals","jacket","pajamas","hat","accessory")
    fun subcategory(category: String, labels: List<Pair<String,Float>>): String {
        val options = when(category) { "top" -> subcategories.take(4); "bottom" -> subcategories.subList(4,8); "shoes" -> subcategories.subList(8,11); else -> listOf(category) }
        return options.firstOrNull { option -> labels.any { it.second >= .55 && it.first.contains(option, true) } } ?: options.first()
    }
    fun defaults(subcategory: String): Pair<String,String> = when(subcategory) {
        "formal shoes","trousers","shirt" -> "smart" to "all"
        "hoodie","sweater","jacket" -> "casual" to "winter"
        "shorts","sandals" -> "casual" to "summer"
        else -> "casual" to "all"
    }
    fun pattern(pixels: IntArray, width: Int, height: Int): String {
        if(width < 8 || height < 8) return "solid"
        fun light(p: Int) = ((p shr 16 and 255)*.299 + (p shr 8 and 255)*.587 + (p and 255)*.114)
        var horizontal = 0; var vertical = 0; var count = 0; var edges = 0
        val step = maxOf(1, minOf(width,height)/100)
        for(y in step until height step step) for(x in step until width step step) {
            val i=y*width+x; val p=pixels[i]; if(p ushr 24 < 245 || pixels[i-step] ushr 24 < 245 || pixels[i-step*width] ushr 24 < 245) continue
            val dx=abs(light(p)-light(pixels[i-step])); val dy=abs(light(p)-light(pixels[i-step*width])); count++
            if(dx>35) horizontal++; if(dy>35) vertical++; if(max(dx,dy)>35) edges++
        }
        if(count==0 || edges.toDouble()/count < .035) return "solid"
        if(horizontal > vertical*2 || vertical > horizontal*2) return "striped"
        if(horizontal.toDouble()/count > .1 && vertical.toDouble()/count > .1) return "checked"
        return if(edges.toDouble()/count < .09) "dotted" else "printed"
    }
    /** Returns clockwise quarter-turns. Ambiguous masks stay at zero. */
    fun orientation(pixels: IntArray, width: Int, height: Int, category: String): Int {
        if(category !in listOf("top","jacket","bottom","shoes")) return 0
        fun point(x: Int,y: Int,q: Int): Pair<Int,Int> = when(q) {1 -> height-1-y to x;2 -> width-1-x to height-1-y;3 -> y to width-1-x;else -> x to y}
        fun score(q: Int): Double {
            val pts = ArrayList<Pair<Int,Int>>()
            for(y in 0 until height step maxOf(1,height/100)) for(x in 0 until width step maxOf(1,width/100)) if(pixels[y*width+x] ushr 24 >= 180) pts.add(point(x,y,q))
            if(pts.size<10) return 0.0
            val left=pts.minOf { it.first }; val right=pts.maxOf { it.first }; val top=pts.minOf { it.second }; val bottom=pts.maxOf { it.second }
            val w=(right-left+1).toDouble(); val h=(bottom-top+1).toDouble()
            if(category=="shoes") return if(w/h>1.35) 2.0 else 0.0 // toe direction is inherently ambiguous
            val upper=pts.filter { (it.second-top)/h in .12.. .42 }; val lower=pts.filter { (it.second-top)/h in .65.. .95 }
            fun span(p: List<Pair<Int,Int>>) = if(p.isEmpty()) 0.0 else (p.maxOf{it.first}-p.minOf{it.first})/w
            return if(category=="bottom") {
                val gap=lower.count { abs((it.first-left)/w-.5)<.09 }.toDouble()/lower.size.coerceAtLeast(1)
                (if(h/w>1.15) 2.0 else 0.0) + (span(upper)-span(lower))*.25 - gap
            } else (if(h/w in .65..1.8) 1.0 else 0.0) + span(upper)-span(lower)
        }
        val scores=(0..3).map(::score); val best=scores.indices.maxBy { scores[it] }
        return if(scores[best] > scores[0]+.12) best else 0
    }
}
