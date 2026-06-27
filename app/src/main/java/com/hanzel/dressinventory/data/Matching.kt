package com.hanzel.dressinventory.data

import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.math.abs

private val chartByName = COLOR_CHART.associateBy { it.name }

fun hsl(hex: Long): FloatArray {
    val r = ((hex shr 16) and 0xFF) / 255f
    val g = ((hex shr 8) and 0xFF) / 255f
    val b = (hex and 0xFF) / 255f
    val max = maxOf(r, g, b)
    val min = minOf(r, g, b)
    val l = (max + min) / 2f
    val d = max - min
    val s = if (d == 0f) 0f else d / (1f - abs(2 * l - 1)).coerceAtLeast(1e-5f)
    val h = when {
        d == 0f -> 0f
        max == r -> 60f * (((g - b) / d + 6f) % 6f)
        max == g -> 60f * ((b - r) / d + 2f)
        else -> 60f * ((r - g) / d + 4f)
    }
    return floatArrayOf(h, s, l)
}

fun Dress.isNeutral(): Boolean {
    chartByName[colorName]?.let { return it.neutral }
    val (_, s, l) = hsl(colorHex)
    return s < 0.16f || l > 0.93f || l < 0.12f
}

private operator fun FloatArray.component1() = this[0]
private operator fun FloatArray.component2() = this[1]
private operator fun FloatArray.component3() = this[2]

/** 0..100 score for how well a top and bottom go together (colour + pattern). */
fun matchScore(top: Dress, bottom: Dress): Int {
    val (h1, _, l1) = hsl(top.colorHex)
    val (h2, _, l2) = hsl(bottom.colorHex)
    val nT = top.isNeutral()
    val nB = bottom.isNeutral()
    var score = when {
        nT && nB -> 72 + ((abs(l1 - l2) * 60).toInt()).coerceAtMost(18)
        nT || nB -> 86
        else -> {
            val diff = abs(h1 - h2).let { minOf(it, 360f - it) }
            when {
                diff <= 18f -> if (abs(l1 - l2) >= 0.18f) 84 else 68
                diff <= 40f -> 78
                diff in 150f..210f -> 80
                diff in 100f..150f -> 62
                else -> 42
            }
        }
    }
    score += when {
        top.pattern == Pattern.PATTERNED && bottom.pattern == Pattern.PATTERNED -> -20
        top.pattern != bottom.pattern -> 8
        else -> 2
    }
    return score.coerceIn(0, 100)
}

fun matchLabel(score: Int): String = when {
    score >= 85 -> "Perfect match"
    score >= 72 -> "Great match"
    score >= 58 -> "Good match"
    else -> "Bold choice"
}

fun matchReason(top: Dress, bottom: Dress): String {
    val colour = run {
        val nT = top.isNeutral()
        val nB = bottom.isNeutral()
        val (h1, _, _) = hsl(top.colorHex)
        val (h2, _, _) = hsl(bottom.colorHex)
        when {
            nT && nB -> "Two neutrals — a clean, classic combination"
            nT -> "${top.colorName} is a neutral that lets ${bottom.colorName} shine"
            nB -> "${bottom.colorName} grounds the ${top.colorName} nicely"
            else -> {
                val diff = abs(h1 - h2).let { minOf(it, 360f - it) }
                when {
                    diff <= 18f -> "Tone-on-tone ${top.colorName} and ${bottom.colorName}"
                    diff <= 40f -> "${top.colorName} and ${bottom.colorName} sit close on the colour wheel"
                    diff in 150f..210f -> "${top.colorName} pops against ${bottom.colorName} — complementary tones"
                    else -> "${top.colorName} with ${bottom.colorName} is an adventurous mix"
                }
            }
        }
    }
    val pattern = when {
        top.pattern == Pattern.PATTERNED && bottom.pattern == Pattern.PATTERNED ->
            "Two patterns compete — wear with confidence!"
        top.pattern == Pattern.PATTERNED -> "The patterned top stands out over a solid bottom"
        bottom.pattern == Pattern.PATTERNED -> "The patterned bottom pairs well with a solid top"
        else -> null
    }
    return if (pattern != null) "$colour. $pattern." else "$colour."
}

fun lastWornBefore(data: AppData, id: String, date: LocalDate): LocalDate? =
    data.wearLog
        .filter { (d, ids) -> id in ids && LocalDate.parse(d).isBefore(date) }
        .keys.maxOfOrNull { LocalDate.parse(it) }

/** Days since last worn before [date], capped at 15; 16 if never worn. */
fun freshness(data: AppData, id: String, date: LocalDate): Int {
    val last = lastWornBefore(data, id, date) ?: return 16
    return ChronoUnit.DAYS.between(last, date).toInt().coerceAtMost(15)
}

fun freshnessText(data: AppData, id: String, date: LocalDate): String {
    val last = lastWornBefore(data, id, date) ?: return "Never worn yet"
    val days = ChronoUnit.DAYS.between(last, date).toInt()
    return when {
        days <= 1 -> "Worn yesterday"
        days >= 15 -> "Last worn over 2 weeks ago"
        else -> "Last worn $days days ago"
    }
}

data class Suggestion(
    val top: Dress,
    val bottom: Dress,
    val score: Int,
    val matchScore: Int,
)

/** Ranked outfit suggestions for [date], preferring fresh items and matching pairs. */
fun suggestOutfits(data: AppData, date: LocalDate): List<Suggestion> {
    val tops = data.dresses.filter { it.category == Category.TOP }
    val bottoms = data.dresses.filter { it.category == Category.BOTTOM }
    if (tops.isEmpty() || bottoms.isEmpty()) return emptyList()

    fun build(minFresh: Int): List<Suggestion> = buildList {
        for (t in tops) for (b in bottoms) {
            val fT = freshness(data, t.id, date)
            val fB = freshness(data, b.id, date)
            if (fT < minFresh || fB < minFresh) continue
            val m = matchScore(t, b)
            val score = (0.55f * m + 0.45f * ((fT + fB) / 32f * 100f)).toInt()
            add(Suggestion(t, b, score, m))
        }
    }

    // Prefer pairs not worn in the last 3 days; relax if the closet is small.
    val strict = build(minFresh = 3)
    val pool = if (strict.isNotEmpty()) strict else build(minFresh = 0)
    return pool.sortedByDescending { it.score }.take(15)
}

/** All top+bottom combinations ranked purely by colour/pattern harmony. */
fun allPairs(data: AppData): List<Suggestion> {
    val tops = data.dresses.filter { it.category == Category.TOP }
    val bottoms = data.dresses.filter { it.category == Category.BOTTOM }
    return buildList {
        for (t in tops) for (b in bottoms) {
            val m = matchScore(t, b)
            add(Suggestion(t, b, m, m))
        }
    }.sortedByDescending { it.matchScore }
}

data class ColorRecommendation(
    val color: ChartColor,
    val topMatchScore: Int?,
    val topMatchDress: Dress?,
    val bottomMatchScore: Int?,
    val bottomMatchDress: Dress?,
)

fun getShoppingRecommendations(data: AppData): List<ColorRecommendation> {
    val ownedColors = data.dresses.map { it.colorName }.toSet()
    val missingColors = COLOR_CHART.filter { it.name !in ownedColors }

    val tops = data.dresses.filter { it.category == Category.TOP }
    val bottoms = data.dresses.filter { it.category == Category.BOTTOM }

    return missingColors.map { c ->
        val mockTop = Dress(
            id = "mock_top",
            name = "A ${c.name} Top",
            category = Category.TOP,
            type = "",
            colorName = c.name,
            colorHex = c.hex
        )
        val mockBottom = Dress(
            id = "mock_bottom",
            name = "A ${c.name} Bottom",
            category = Category.BOTTOM,
            type = "",
            colorName = c.name,
            colorHex = c.hex
        )

        val bestTopMatch = bottoms.map { b -> b to matchScore(mockTop, b) }.maxByOrNull { it.second }
        val bestBottomMatch = tops.map { t -> t to matchScore(t, mockBottom) }.maxByOrNull { it.second }

        ColorRecommendation(
            color = c,
            topMatchScore = bestTopMatch?.second,
            topMatchDress = bestTopMatch?.first,
            bottomMatchScore = bestBottomMatch?.second,
            bottomMatchDress = bestBottomMatch?.first
        )
    }.sortedWith { a, b ->
        val scoreA = maxOf(a.topMatchScore ?: 0, a.bottomMatchScore ?: 0)
        val scoreB = maxOf(b.topMatchScore ?: 0, b.bottomMatchScore ?: 0)
        scoreB.compareTo(scoreA)
    }
}

