package dev.closet.domain

import dev.closet.data.ClothingItem
import org.json.JSONObject
import kotlin.math.abs
import kotlin.random.Random

/** The only style policy source is assets/style_rules.json. */
class StyleRules(json: String) {
    private val root = JSONObject(json)
    val neutrals = root.getJSONArray("neutralColors").let { a -> (0 until a.length()).map { a.getString(it) }.toSet() }
    val pairs = root.getJSONArray("colorPairs").let { a -> (0 until a.length()).map { i ->
        a.getJSONArray(i).let { setOf(it.getString(0), it.getString(1)) }
    } }
    val colorWeight = root.getJSONObject("weights").getInt("color")
    val formalityWeight = root.getJSONObject("weights").getInt("formality")
    val seasonWeight = root.getJSONObject("weights").getInt("season")
    val adjacent = root.getBoolean("allowAdjacentFormality")
    val hatProbability = root.getJSONObject("optional").getDouble("hatProbability")
    val jacketProbability = root.getJSONObject("optional").getDouble("jacketProbability")
    init { require(root.getInt("version") == 1); require(root.getJSONArray("stackOrder").toString() == "[\"hat\",\"top\",\"jacket\",\"bottom\",\"shoes\"]") }
}

class OutfitEngine(private val rules: StyleRules, private val random: Random = Random.Default) {
    fun score(a: ClothingItem, b: ClothingItem): Int {
        var score = 0
        if (a.color == b.color || a.color in rules.neutrals || b.color in rules.neutrals || setOf(a.color, b.color) in rules.pairs) score += rules.colorWeight
        val levels = listOf("casual", "smart", "formal")
        val distance = abs(levels.indexOf(a.formality) - levels.indexOf(b.formality))
        if (distance == 0) score += rules.formalityWeight
        else if (distance == 1 && rules.adjacent) score += rules.formalityWeight / 2
        if (a.season == b.season || a.season == "all" || b.season == "all") score += rules.seasonWeight
        return score
    }

    fun generate(items: List<ClothingItem>, season: String = "all", formality: String = "any"): List<ClothingItem> {
        val pool = items.filter { (season == "all" || it.season == "all" || it.season == season) && (formality == "any" || it.formality == formality) }
        require(listOf("top", "bottom", "shoes").all { c -> pool.any { it.category == c } }) {
            "Add at least one top, bottom, and pair of shoes matching these filters."
        }
        val selected = mutableListOf(pool.filter { it.category == "top" }.random(random))
        for (category in listOf("bottom", "shoes", "hat", "jacket")) {
            if (category == "hat" && random.nextDouble() >= rules.hatProbability) continue
            if (category == "jacket" && random.nextDouble() >= rules.jacketProbability) continue
            val candidates = pool.filter { it.category == category }.shuffled(random)
            // Vary among strong choices, with jitter small enough to retain compatibility.
            candidates.maxByOrNull { candidate -> selected.sumOf { score(it, candidate) } + random.nextInt(4) }?.let(selected::add)
        }
        return ordered(selected)
    }
    companion object {
        val stack = listOf("hat", "top", "jacket", "bottom", "shoes")
        fun ordered(items: List<ClothingItem>) = items.sortedBy { stack.indexOf(it.category) }
    }
}

/** Future extension boundary. No AI implementation, network client, or credentials. */
interface OutfitSuggestionProvider {
    suspend fun suggest(items: List<ClothingItem>, season: String, formality: String): List<ClothingItem>
}
