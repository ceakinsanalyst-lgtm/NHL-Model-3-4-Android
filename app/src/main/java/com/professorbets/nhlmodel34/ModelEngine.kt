package com.professorbets.nhlmodel34

import kotlin.math.exp
import kotlin.math.ln

object ModelEngine {
    // Exact V3.4 core exported from the trained scikit-learn pipeline.
    private val means = doubleArrayOf(
        1.0,
        0.2344648591733233,
        -0.12405185491656323,
        -0.0007781842025506093,
        -0.00040279938256707477,
        -0.0009550127349703253,
        -0.004428654568699534
    )

    private val scales = doubleArrayOf(
        1.0,
        96.42379585363966,
        0.4714896501606361,
        0.053781890066003646,
        0.046888509162391616,
        0.06500698367292918,
        0.5418176048729508
    )

    private val coefficients = doubleArrayOf(
        0.0,
        0.2766229888285177,
        -0.10225755066815974,
        0.05430786977080224,
        0.12268880441468336,
        0.05911433037440709,
        0.01063978593156592
    )

    private const val INTERCEPT = 0.21585770334262508
    private const val CAL_COEF = 1.110652770290715
    private const val CAL_INTERCEPT = -0.07073806950591524

    data class Inputs(
        val homeElo: Double,
        val awayElo: Double,
        val homeB2B: Boolean,
        val awayB2B: Boolean,
        val homeXg20: Double,
        val awayXg20: Double,
        val homeCorsi20: Double,
        val awayCorsi20: Double,
        val homeXg10: Double,
        val awayXg10: Double,
        val homeGsax20: Double,
        val awayGsax20: Double
    )

    data class Prediction(
        val homeProbability: Double,
        val awayProbability: Double,
        val favoriteProbability: Double,
        val signal: String
    )

    fun predict(input: Inputs): Prediction {
        val features = doubleArrayOf(
            1.0,
            input.homeElo - input.awayElo,
            (if (input.homeB2B) 1.0 else 0.0) - (if (input.awayB2B) 1.0 else 0.0),
            input.homeXg20 - input.awayXg20,
            input.homeCorsi20 - input.awayCorsi20,
            input.homeXg10 - input.awayXg10,
            input.homeGsax20 - input.awayGsax20
        )

        var z = INTERCEPT
        for (i in features.indices) {
            val standardized = (features[i] - means[i]) / scales[i]
            z += coefficients[i] * standardized
        }
        val raw = logistic(z)
        val calibratedOnly = logistic(CAL_INTERCEPT + CAL_COEF * logit(raw))
        val calibrated = 0.85 * calibratedOnly + 0.15 * raw
        val favorite = maxOf(calibrated, 1.0 - calibrated)

        return Prediction(
            homeProbability = calibrated,
            awayProbability = 1.0 - calibrated,
            favoriteProbability = favorite,
            signal = signalTier(favorite)
        )
    }

    fun signalTier(probability: Double): String = when {
        probability < .55 -> "PASS"
        probability < .60 -> "LEAN"
        probability < .65 -> "PLAY"
        probability < .70 -> "PLAY+"
        else -> "STRONG"
    }

    fun americanImplied(odds: Double): Double =
        if (odds < 0) (-odds) / ((-odds) + 100.0) else 100.0 / (odds + 100.0)

    fun noVig(homeOdds: Double, awayOdds: Double): Pair<Double, Double> {
        val h = americanImplied(homeOdds)
        val a = americanImplied(awayOdds)
        val total = h + a
        return Pair(h / total, a / total)
    }

    fun decimalOdds(american: Double): Double =
        if (american < 0) 1.0 + 100.0 / -american else 1.0 + american / 100.0

    fun expectedValue(probability: Double, americanOdds: Double): Double =
        probability * decimalOdds(americanOdds) - 1.0

    private fun logistic(x: Double): Double = 1.0 / (1.0 + exp(-x))

    private fun logit(p: Double): Double {
        val bounded = p.coerceIn(1e-5, 1.0 - 1e-5)
        return ln(bounded / (1.0 - bounded))
    }
}
