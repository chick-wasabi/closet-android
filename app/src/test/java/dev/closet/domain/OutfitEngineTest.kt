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

    @Test fun strongPatternsAndFormalSportswearClashesArePenalized() {
        val shirt=piece("a","top").copy(subcategory="shirt",pattern="striped")
        val pants=piece("b","bottom").copy(subcategory="joggers",pattern="checked")
        assertTrue(engine.score(shirt,pants)<engine.score(shirt,pants.copy(subcategory="trousers",pattern="solid")))
    }
    @Test fun summerHasNoJacketsAndLockedPiecesSurviveShuffle() {
        val lock=basics.first()
        val looks=engine.options(basics+piece("t2","top")+piece("j","jacket"),"summer",locked=listOf(lock),jacket=1.0)
        assertTrue(looks.all{look->look.any{it.id==lock.id}&&look.none{it.category=="jacket"}})
    }
    @Test fun sleepUsesPajamasAndNeedsNoShoes() {
        val pajama=piece("p","pajamas")
        assertEquals(listOf(pajama),engine.options(listOf(pajama),occasion="Sleep").single())
    }
    @Test fun wearRecencyAndLocalFeedbackChangeRanking() {
        val a=basics.first();val b=basics[1]
        val fresh=listOf(a,b);val worn=listOf(a.copy(lastWorn=1000),b)
        assertTrue(engine.total(fresh,"all",now=2000)>engine.total(worn,"all",now=2000))
        assertTrue(engine.total(fresh,"all",mapOf(OutfitEngine.pairKey(a.id,b.id) to 2))>engine.total(fresh,"all"))
    }
    @Test fun optionsAreUniqueAndBoundedByFive() {
        val pool=basics+(1..8).map{piece("t$it","top")}
        val options=engine.options(pool)
        assertEquals(5,options.size);assertEquals(5,options.distinct().size)
        assertTrue(engine.explain(options.first()).isNotBlank())
    }
}
