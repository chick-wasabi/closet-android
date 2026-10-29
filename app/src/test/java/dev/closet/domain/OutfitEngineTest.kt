package dev.closet.domain

import dev.closet.data.ClothingItem
import org.junit.Assert.*
import org.junit.Test
import kotlin.random.Random

class OutfitEngineTest {
    private val json = requireNotNull(javaClass.classLoader?.getResource("style_rules.json")).readText()
    private val engine = OutfitEngine(StyleRules(json), Random(42))
    private fun piece(id: String, category: String, color: String = "blue", formality: String = "casual", season: String = "all") =
        ClothingItem(id, category = category, color = color, formality = formality, season = season, imagePath = "unused", originalPath = "unused")
    private val basics = listOf(piece("t", "top"), piece("b", "bottom"), piece("s", "shoes"))

    @Test fun completeOutfitsHaveExactlyOneOfEachRequiredSlot() {
        repeat(50) {
            val outfit = engine.generate(basics + piece("t2", "top") + piece("h", "hat") + piece("j", "jacket") + piece("p", "pajamas"))
            for(c in listOf("top", "bottom", "shoes")) assertEquals(1, outfit.count { it.category == c })
            assertFalse(outfit.any { it.category == "pajamas" })
            assertEquals(outfit, OutfitEngine.ordered(outfit))
        }
    }
    @Test(expected = IllegalArgumentException::class) fun missingShoesCannotProduceIncompleteLook() { engine.generate(basics.dropLast(1)) }
    @Test fun requestedSeasonAndFormalityAreRespected() {
        val pool = basics + piece("winter", "top", season = "winter") + piece("formal", "shoes", formality = "formal")
        repeat(30) {
            val result = engine.generate(pool, "summer", "casual")
            assertFalse(result.any { it.id == "winter" || it.id == "formal" })
        }
    }
    @Test(expected = IllegalArgumentException::class) fun filtersExplainInsufficientWardrobe() { engine.generate(basics, formality = "formal") }
    @Test fun neutralAndConfiguredColorPairsScoreAboveClashes() {
        val blue = piece("b", "top")
        val pink = piece("p", "bottom", "pink")
        assertTrue(engine.score(blue, piece("n", "bottom", "cream")) > engine.score(blue, pink))
        assertTrue(engine.score(blue, piece("m", "bottom", "brown")) > engine.score(blue, pink))
    }
    @Test fun exactFormalityAndCompatibleSeasonImproveScores() {
        val top = piece("a", "top", season = "summer")
        assertTrue(engine.score(top, piece("b", "bottom", season = "summer")) > engine.score(top, piece("c", "bottom", formality = "formal", season = "winter")))
    }
    @Test fun optionalProbabilitiesAreReadFromEditableRules() {
        val rules = StyleRules(json.replace("0.4", "1.0").replace("0.5", "1.0"))
        val result = OutfitEngine(rules, Random(1)).generate(basics + piece("h", "hat") + piece("j", "jacket"))
        assertEquals(listOf("hat", "top", "jacket", "bottom", "shoes"), result.map { it.category })
    }
    @Test(expected = IllegalArgumentException::class) fun invalidProbabilitiesAreRejected() { StyleRules(json.replace("0.4", "1.5")) }
    @Test fun colorPairMatchingIsSymmetric() {
        val a = piece("a", "top", "blue"); val b = piece("b", "bottom", "brown")
        assertEquals(engine.score(a, b), engine.score(b, a))
    }
}
