package dev.closet.domain

import dev.closet.data.ClothingItem
import org.json.JSONObject
import kotlin.math.abs
import kotlin.random.Random

class StyleRules(json: String) {
    val root=JSONObject(json)
    fun strings(key: String)=root.getJSONArray(key).let { a -> (0 until a.length()).map { a.getString(it) } }
    val neutrals=strings("neutralColors").toSet()
    val pairs=root.getJSONArray("colorPairs").let { a -> (0 until a.length()).map { i -> a.getJSONArray(i).let { setOf(it.getString(0),it.getString(1)) } } }
    fun weight(key: String)=root.getJSONObject("weights").getInt(key)
    val colorWeight=weight("color");val formalityWeight=weight("formality");val seasonWeight=weight("season")
    val adjacent=root.getBoolean("allowAdjacentFormality")
    val hatProbability=root.getJSONObject("optional").getDouble("hatProbability")
    val jacketProbability=root.getJSONObject("optional").getDouble("jacketProbability")
    val occasions=root.getJSONObject("occasions").keys().asSequence().toList()
    init { require(root.getInt("version")==2);require(hatProbability in 0.0..1.0 && jacketProbability in 0.0..1.0)
        require(root.getJSONObject("weights").keys().asSequence().all { weight(it)>=0 });require(strings("stackOrder")==listOf("hat","top","jacket","bottom","shoes")) }
    fun harmony(a: String,b: String): Boolean {
        if(a==b||a in neutrals||b in neutrals||setOf(a,b) in pairs) return true
        val hues=root.getJSONObject("hues");if(!hues.has(a)||!hues.has(b)) return false
        val diff=abs(hues.getInt(a)-hues.getInt(b));val angle=minOf(diff,360-diff)
        return angle<=root.getJSONObject("harmony").getInt("analogousDegrees") || abs(angle-180)<=root.getJSONObject("harmony").getInt("complementaryTolerance")
    }
}

class OutfitEngine(private val rules: StyleRules, private val random: Random = Random.Default) {
    fun score(a: ClothingItem,b: ClothingItem): Int {
        var score=if(rules.harmony(a.color,b.color)) rules.colorWeight else 0
        val levels=listOf("casual","smart","formal");val distance=abs(levels.indexOf(a.formality)-levels.indexOf(b.formality))
        score+=when {distance==0->rules.formalityWeight;distance==1&&rules.adjacent->rules.formalityWeight/2;else->-rules.formalityWeight}
        if(a.season==b.season||a.season=="all"||b.season=="all") score+=rules.seasonWeight
        if(a.pattern in rules.strings("strongPatterns") && b.pattern in rules.strings("strongPatterns")) score-=rules.weight("patternClash")
        val clashes=rules.root.getJSONArray("incoherentSubcategories")
        if((0 until clashes.length()).any { i -> val p=clashes.getJSONArray(i);setOf(a.subcategory,b.subcategory)==setOf(p.getString(0),p.getString(1)) }) score-=rules.weight("incoherence")
        return score
    }
    fun total(selection: List<ClothingItem>, season: String, preferences: Map<String,Int> = emptyMap(), repeatDays: Int = 7, now: Long = System.currentTimeMillis()): Int {
        var result=0
        for(i in selection.indices) {
            val a=selection[i]
            if(season=="all"||a.season=="all"||a.season==season) result+=rules.seasonWeight else result-=rules.seasonWeight
            if(a.lastWorn>0 && now-a.lastWorn < repeatDays*86400000L) result-=rules.weight("recentWear")
            for(j in 0 until i) result+=score(a,selection[j])+(preferences[pairKey(a.id,selection[j].id)]?:0)*rules.weight("preference")
        }
        return result
    }
    fun generate(items: List<ClothingItem>, season: String = "all", formality: String = "any"): List<ClothingItem> =
        options(items,season,formality).first()
    fun options(items: List<ClothingItem>, season: String = "all", formality: String = "any", occasion: String = "Casual",
                locked: List<ClothingItem> = emptyList(), preferences: Map<String,Int> = emptyMap(), repeatDays: Int = 7,
                hat: Double = rules.hatProbability, jacket: Double = rules.jacketProbability): List<List<ClothingItem>> {
        val mode=rules.root.getJSONObject("occasions").getJSONObject(occasion)
        fun values(key: String)=mode.getJSONArray(key).let { a->(0 until a.length()).map { a.getString(it) } }
        val required=values("required")
        val pool=items.filter { (season=="all"||it.season=="all"||it.season==season) && (formality=="any"||it.formality==formality) && it.formality in values("formalities") && it.subcategory !in values("excludedSubcategories") }
        val locks=locked.filter { it.id in items.map { p->p.id } && (it.category in required || (occasion!="Sleep"&&it.category in listOf("hat","jacket"))) }
        require(required.all { c->pool.any{it.category==c}||locks.any{it.category==c} }) {
            if(occasion=="Sleep") "Add pajamas to try Sleep outfits." else "Add a top, bottom and shoes matching these filters."
        }
        val candidates=(0 until 100).map {
            val chosen=locks.toMutableList()
            for(c in required + if(occasion=="Sleep") emptyList() else listOf("hat","jacket")) {
                if(chosen.any{it.category==c}) continue
                if(c=="hat"&&random.nextDouble()>=hat) continue
                if(c=="jacket"&&(season !in rules.strings("jacketSeasons") || random.nextDouble()>=jacket)) continue
                pool.filter { it.category==c }.randomOrNull(random)?.let(chosen::add)
            }
            ordered(chosen)
        }.distinctBy { it.joinToString { p->p.id } }.shuffled(random)
        return candidates.sortedByDescending { total(it,season,preferences,repeatDays) }.take(5)
    }
    fun explain(selection: List<ClothingItem>): String {
        val key=when {selection.any{it.category=="pajamas"}->"sleep"; selection.zipWithNext().all{rules.harmony(it.first.color,it.second.color)}->"color"; selection.map{it.formality}.distinct().size==1->"coherence";else->"mixed"}
        return rules.root.getJSONObject("explanations").getString(key)
    }
    companion object {
        val stack=listOf("hat","top","jacket","bottom","shoes")
        fun ordered(items: List<ClothingItem>)=items.sortedBy { if(it.category=="pajamas") 1 else stack.indexOf(it.category) }
        fun pairKey(a: String,b: String)=listOf(a,b).sorted().joinToString("|")
    }
}
interface OutfitSuggestionProvider { suspend fun suggest(items: List<ClothingItem>,season: String,formality: String): List<ClothingItem> }
