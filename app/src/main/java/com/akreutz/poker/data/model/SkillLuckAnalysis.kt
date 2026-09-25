package com.akreutz.poker.data.model

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.sqrt

/**
 * A player's per-session results (net delta in cents), used as the raw input to
 * [computeSkillLuckAnalysis].
 */
data class PlayerSessionResults(
    val playerId: String,
    val playerName: String,
    val deltasCents: List<Long>,
)

/** One player's contribution to the skill/luck breakdown. */
data class PlayerSkillSummary(
    val playerId: String,
    val playerName: String,
    val sessionsPlayed: Int,
    /** This player's per-session edge: their mean net result, in cents. */
    val meanDeltaCents: Double,
    /** Standard error of [meanDeltaCents], i.e. session std-dev / sqrt(n). */
    val standardErrorCents: Double,
    /**
     * Two-tailed p-value from a one-sample t-test of "this player's true long-run edge is zero",
     * using their own session results. Null if there aren't enough sessions (need at least 2) to
     * run the test. A small p-value (conventionally < 0.05) means the observed edge would be
     * unlikely under pure luck; it says nothing about how large the edge actually is.
     */
    val edgeSignificanceP: Double?,
    /**
     * Empirical-Bayes (James-Stein-style) shrinkage estimate of this player's true long-run edge,
     * in cents: their raw [meanDeltaCents] pulled toward the group average (0) in proportion to how
     * noisy their own estimate is relative to the group's spread of real skill differences. Null
     * unless the group-level skill variance was estimable (same condition as [SkillLuckAnalysis]
     * being non-null). A better point estimate than the raw mean for players with few sessions,
     * since it accounts for how much of their raw mean is plausibly just luck.
     */
    val shrunkEdgeCents: Double?,
    /**
     * 90% posterior credible interval (in cents) for this player's true long-run edge, under the
     * same empirical-Bayes model as [shrunkEdgeCents]: the range the true edge falls in with 90%
     * probability, combining their own results with how much real skill variation exists in the
     * group as a whole. A wide interval - or one straddling zero - means the data can't yet tell
     * a real edge apart from luck, which a single "probability of a real edge" figure would hide:
     * testing P(edge > 0) is misleadingly overconfident with few sessions, since shrinkage never
     * flips the sign of the raw mean, so that probability exceeds 50% for essentially every player
     * regardless of how uncertain the estimate actually is. Null under the same condition as
     * [shrunkEdgeCents].
     */
    val shrunkEdgeCredibleIntervalCents: ClosedFloatingPointRange<Double>?,
    /**
     * Standard deviation of the same posterior distribution [shrunkEdgeCredibleIntervalCents] is
     * derived from, in cents - i.e. the full shape of the belief about this player's true edge,
     * for rendering it (e.g. as a bell curve) rather than only its interval. Null under the same
     * condition as [shrunkEdgeCredibleIntervalCents].
     */
    val posteriorStdDevCents: Double?,
    /** True if this player met the chosen minimum-sessions threshold and was included in the ANOVA. */
    val isEligible: Boolean,
) {
    /** True if [edgeSignificanceP] clears the conventional 5% significance threshold. */
    val isStatisticallySignificant: Boolean
        get() = edgeSignificanceP != null && edgeSignificanceP < 0.05
}

/**
 * Result of a one-way ANOVA / intraclass-correlation decomposition of session results into a
 * "skill" component (persistent differences between players' average results) and a "luck"
 * component (session-to-session noise around each player's own average).
 *
 * Method: for each player i with n_i sessions, mean m_i and grand mean m:
 *   - within-player variance (luck):  pooled variance of sessions around each player's own mean
 *   - between-player variance (raw):  variance of player means around the grand mean, weighted by n_i
 *   - skill variance is the between-player variance corrected for the luck noise it inherits from
 *     finite sample sizes: skillVar = betweenVar - withinVar / meanSessionsPerPlayer (floored at 0)
 *
 * skillProportion = skillVar / (skillVar + withinVar) is the intraclass correlation coefficient (ICC):
 * the fraction of total result variance explained by which player it is, i.e. skill.
 */
data class SkillLuckAnalysis(
    val playerSummaries: List<PlayerSkillSummary>,
    val totalSessions: Int,
    val minSessionsPerPlayer: Int,
    val eligiblePlayerCount: Int,
    val withinPlayerVarianceCents2: Double,
    val betweenPlayerVarianceCents2: Double,
    val skillVarianceCents2: Double,
    val skillProportion: Double,
    val luckProportion: Double,
    /** Bootstrap 90% confidence interval for [skillProportion], or null if too little data to estimate. */
    val skillProportionConfidenceInterval: ClosedFloatingPointRange<Double>?,
) {
    companion object {
        /** Default minimum sessions a player needs before being included in the ANOVA; user-adjustable in the UI. */
        const val DEFAULT_MIN_SESSIONS_PER_PLAYER = 10

        /** Need at least this many eligible players for a between-player variance to be meaningful. */
        const val MIN_ELIGIBLE_PLAYERS = 2
    }
}

/**
 * Computes the skill/luck variance decomposition from raw per-player session results.
 * Players with fewer than [minSessionsPerPlayer] sessions are excluded from the ANOVA (too few
 * points to separate their own noise from their mean, and their few sessions add finite-sample
 * noise to the between-player comparison) but still reported in [SkillLuckAnalysis.playerSummaries]
 * for context. Raising this threshold trades away statistical power (fewer players contribute to
 * the between-player comparison) for a less noisy per-player mean/variance estimate.
 *
 * Returns null if there isn't enough data (fewer than [SkillLuckAnalysis.MIN_ELIGIBLE_PLAYERS]
 * eligible players) to attempt a decomposition at all.
 */
fun computeSkillLuckAnalysis(
    results: List<PlayerSessionResults>,
    minSessionsPerPlayer: Int = SkillLuckAnalysis.DEFAULT_MIN_SESSIONS_PER_PLAYER,
    bootstrapSamples: Int = 1_000,
    randomSeed: Long = 42L,
): SkillLuckAnalysis? {
    val totalSessions = results.sumOf { it.deltasCents.size }

    val eligible = results.filter { it.deltasCents.size >= minSessionsPerPlayer }
    if (eligible.size < SkillLuckAnalysis.MIN_ELIGIBLE_PLAYERS) return null

    val decomposition = decomposeVariance(eligible)
    // Empirical-Bayes prior variance for a player's true edge: the group-level skill variance
    // (tau^2) already estimated by the ANOVA. Shared across all players' shrinkage below.
    val priorVariance = decomposition.skillVariance

    val summaries = results.map { player ->
        val n = player.deltasCents.size
        val mean = player.deltasCents.average()
        val variance = if (n > 1) {
            player.deltasCents.sumOf { (it - mean) * (it - mean) } / (n - 1)
        } else {
            0.0
        }
        val stdError = if (n > 0) sqrt(variance / n) else 0.0
        val pValue = if (n > 1 && stdError > 0) {
            val tStatistic = mean / stdError
            twoTailedTTestPValue(tStatistic = tStatistic, degreesOfFreedom = n - 1)
        } else {
            null
        }
        val shrinkage = if (n > 0) shrinkEdge(sampleMean = mean, sampleVariance = stdError * stdError, priorVariance = priorVariance) else null
        PlayerSkillSummary(
            playerId = player.playerId,
            playerName = player.playerName,
            sessionsPlayed = n,
            meanDeltaCents = mean,
            standardErrorCents = stdError,
            edgeSignificanceP = pValue,
            shrunkEdgeCents = shrinkage?.shrunkMean,
            shrunkEdgeCredibleIntervalCents = shrinkage?.let { it.credibleIntervalLow..it.credibleIntervalHigh },
            posteriorStdDevCents = shrinkage?.posteriorStdDev,
            isEligible = n >= minSessionsPerPlayer,
        )
    }.sortedByDescending { it.meanDeltaCents }

    val confidenceInterval = bootstrapSkillProportionCI(
        eligible = eligible,
        samples = bootstrapSamples,
        seed = randomSeed,
    )

    return SkillLuckAnalysis(
        playerSummaries = summaries,
        totalSessions = totalSessions,
        minSessionsPerPlayer = minSessionsPerPlayer,
        eligiblePlayerCount = eligible.size,
        withinPlayerVarianceCents2 = decomposition.withinVariance,
        betweenPlayerVarianceCents2 = decomposition.betweenVariance,
        skillVarianceCents2 = decomposition.skillVariance,
        skillProportion = decomposition.skillProportion,
        luckProportion = 1.0 - decomposition.skillProportion,
        skillProportionConfidenceInterval = confidenceInterval,
    )
}

private data class VarianceDecomposition(
    val withinVariance: Double,
    val betweenVariance: Double,
    val skillVariance: Double,
    val skillProportion: Double,
)

/**
 * One-way random-effects ANOVA (method-of-moments / ICC estimator).
 * See [SkillLuckAnalysis] doc for the formulas.
 */
private fun decomposeVariance(eligible: List<PlayerSessionResults>): VarianceDecomposition {
    val playerMeans = eligible.map { it.deltasCents.average() }
    val playerCounts = eligible.map { it.deltasCents.size }
    val totalN = playerCounts.sum()
    val k = eligible.size

    // Grand mean, weighted by each player's session count.
    val grandMean = eligible.sumOf { player -> player.deltasCents.sumOf { it.toDouble() } } / totalN

    // Within-player variance: pooled (sum of squared deviations from each player's own mean) / (N - k).
    val withinSumSquares = eligible.sumOf { player ->
        val mean = player.deltasCents.average()
        player.deltasCents.sumOf { (it - mean) * (it - mean) }
    }
    val withinDegreesOfFreedom = max(totalN - k, 1)
    val withinVariance = withinSumSquares / withinDegreesOfFreedom

    // Between-player mean square, weighted by session count (standard unbalanced one-way ANOVA).
    val betweenSumSquares = eligible.indices.sumOf { i ->
        val diff = playerMeans[i] - grandMean
        playerCounts[i] * diff * diff
    }
    val betweenDegreesOfFreedom = max(k - 1, 1)
    val betweenMeanSquare = betweenSumSquares / betweenDegreesOfFreedom

    // Average group size correction term (standard unbalanced-ANOVA n0), so that:
    //   betweenMeanSquare = skillVariance * n0 + withinVariance
    val sumN = totalN.toDouble()
    val sumN2 = playerCounts.sumOf { it.toDouble() * it }
    val n0 = if (k > 1) (sumN - sumN2 / sumN) / (k - 1) else 1.0

    val skillVariance = if (n0 > 0) {
        max((betweenMeanSquare - withinVariance) / n0, 0.0)
    } else {
        0.0
    }

    val skillProportion = if (skillVariance + withinVariance > 0) {
        skillVariance / (skillVariance + withinVariance)
    } else {
        0.0
    }

    return VarianceDecomposition(
        withinVariance = withinVariance,
        betweenVariance = betweenMeanSquare,
        skillVariance = skillVariance,
        skillProportion = skillProportion,
    )
}

/**
 * Bootstrap confidence interval for the skill proportion: resamples each player's sessions
 * with replacement (keeping each player's session count fixed), recomputes the ICC each time,
 * and returns the 5th-95th percentile range (90% CI). This captures how noisy the skill/luck
 * split estimate is given the actual sample sizes, which matters a lot for small poker datasets.
 */
private fun bootstrapSkillProportionCI(
    eligible: List<PlayerSessionResults>,
    samples: Int,
    seed: Long,
): ClosedFloatingPointRange<Double>? {
    if (samples <= 0) return null
    val random = kotlin.random.Random(seed)

    val proportions = (0 until samples).map {
        val resampled = eligible.map { player ->
            val n = player.deltasCents.size
            val resampledDeltas = List(n) { player.deltasCents[random.nextInt(n)] }
            player.copy(deltasCents = resampledDeltas)
        }
        decomposeVariance(resampled).skillProportion
    }.sorted()

    if (proportions.isEmpty()) return null
    val lowIndex = (0.05 * proportions.size).toInt().coerceIn(0, proportions.size - 1)
    val highIndex = (0.95 * proportions.size).toInt().coerceIn(0, proportions.size - 1)
    return proportions[lowIndex]..proportions[highIndex]
}

private data class ShrunkEdge(
    val shrunkMean: Double,
    val credibleIntervalLow: Double,
    val credibleIntervalHigh: Double,
    val posteriorStdDev: Double,
)

/** z-score for a 90% two-sided credible/confidence interval, i.e. the 5th/95th percentile of a standard normal. */
private const val Z_SCORE_90_PERCENT_CI = 1.6448536269514722

/**
 * Empirical-Bayes (James-Stein-style) shrinkage of one player's sample mean toward zero (the
 * group average), using a Normal-Normal conjugate model:
 *   - prior:      true edge theta ~ N(0, priorVariance), i.e. real skill edges in this group are
 *                 centered on "no edge" with the spread the ANOVA found across all players
 *   - likelihood: sample mean ~ N(theta, sampleVariance), sampleVariance being this player's own
 *                 sampling variance (standardError^2)
 *
 * The posterior for theta is then Normal with:
 *   posteriorVariance = 1 / (1/priorVariance + 1/sampleVariance)
 *   posteriorMean     = posteriorVariance * (sampleMean / sampleVariance)
 *
 * [ShrunkEdge] reports that posterior mean plus its 90% credible interval (posteriorMean +/-
 * 1.645 * posteriorStdDev), rather than a single "probability of a real edge" figure: testing
 * P(theta > 0) would be misleadingly overconfident here, since shrinkage never flips the sign of
 * the raw mean, so that probability exceeds 50% for essentially every player regardless of sample
 * size - it answers "which side of zero", not "how confident should we be". A wide interval, or
 * one straddling zero, correctly shows the data isn't enough to tell a real edge from luck.
 *
 * Returns null if there's no usable prior (priorVariance <= 0, i.e. the ANOVA found no detectable
 * group-level skill variance) or no usable sample variance for this player.
 */
private fun shrinkEdge(sampleMean: Double, sampleVariance: Double, priorVariance: Double): ShrunkEdge? {
    if (priorVariance <= 0.0 || sampleVariance <= 0.0) return null

    val posteriorVariance = 1.0 / (1.0 / priorVariance + 1.0 / sampleVariance)
    val posteriorMean = posteriorVariance * (sampleMean / sampleVariance)
    val posteriorStdDev = sqrt(posteriorVariance)
    if (posteriorStdDev <= 0.0) return null

    val margin = Z_SCORE_90_PERCENT_CI * posteriorStdDev
    return ShrunkEdge(
        shrunkMean = posteriorMean,
        credibleIntervalLow = posteriorMean - margin,
        credibleIntervalHigh = posteriorMean + margin,
        posteriorStdDev = posteriorStdDev,
    )
}

/**
 * Two-tailed p-value for a t-statistic with the given degrees of freedom, i.e.
 * P(|T| >= |tStatistic|) under a Student's t distribution. Computed via the regularized
 * incomplete beta function, the standard closed-form relationship between the t and beta
 * distributions - there being no stats library available in this project.
 */
private fun twoTailedTTestPValue(tStatistic: Double, degreesOfFreedom: Int): Double {
    if (degreesOfFreedom < 1) return 1.0
    val x = degreesOfFreedom / (degreesOfFreedom + tStatistic * tStatistic)
    return regularizedIncompleteBeta(x, degreesOfFreedom / 2.0, 0.5)
}

/** Regularized incomplete beta function I_x(a, b), via a continued-fraction expansion (Numerical Recipes). */
private fun regularizedIncompleteBeta(x: Double, a: Double, b: Double): Double {
    if (x <= 0.0) return 0.0
    if (x >= 1.0) return 1.0

    val logBeta = logGamma(a + b) - logGamma(a) - logGamma(b) +
        a * kotlin.math.ln(x) + b * kotlin.math.ln(1.0 - x)
    val front = kotlin.math.exp(logBeta)

    // Use the continued fraction directly for x < (a+1)/(a+b+2), and the symmetry relation otherwise,
    // as the continued fraction converges poorly on the other side.
    return if (x < (a + 1.0) / (a + b + 2.0)) {
        front * betaContinuedFraction(x, a, b) / a
    } else {
        1.0 - front * betaContinuedFraction(1.0 - x, b, a) / b
    }
}

private fun betaContinuedFraction(x: Double, a: Double, b: Double, maxIterations: Int = 200): Double {
    val epsilon = 1e-12
    val tiny = 1e-30

    val qab = a + b
    val qap = a + 1.0
    val qam = a - 1.0
    var c = 1.0
    var d = 1.0 - qab * x / qap
    if (abs(d) < tiny) d = tiny
    d = 1.0 / d
    var h = d

    for (i in 1..maxIterations) {
        val m = i
        val m2 = 2 * m

        val even = m * (b - m) * x / ((qam + m2) * (a + m2))
        d = 1.0 + even * d
        if (abs(d) < tiny) d = tiny
        c = 1.0 + even / c
        if (abs(c) < tiny) c = tiny
        d = 1.0 / d
        h *= d * c

        val odd = -(a + m) * (qab + m) * x / ((a + m2) * (qap + m2))
        d = 1.0 + odd * d
        if (abs(d) < tiny) d = tiny
        c = 1.0 + odd / c
        if (abs(c) < tiny) c = tiny
        d = 1.0 / d
        val delta = d * c
        h *= delta

        if (abs(delta - 1.0) < epsilon) break
    }
    return h
}

/** Log of the gamma function (Lanczos approximation). */
private fun logGamma(x: Double): Double {
    val coefficients = doubleArrayOf(
        76.18009172947146, -86.50532032941677, 24.01409824083091,
        -1.231739572450155, 0.1208650973866179e-2, -0.5395239384953e-5,
    )
    var y = x
    val tmp0 = x + 5.5
    val tmp = tmp0 - (x + 0.5) * kotlin.math.ln(tmp0)
    var series = 1.000000000190015
    for (coefficient in coefficients) {
        y += 1.0
        series += coefficient / y
    }
    return -tmp + kotlin.math.ln(2.5066282746310005 * series / x)
}

/** Groups a flat list of sessions into per-player chronological result lists, keyed by player id. */
fun playerSessionResultsFrom(sessions: List<SessionWithEntries>): List<PlayerSessionResults> {
    val deltasByPlayer = linkedMapOf<String, MutableList<Long>>()
    val namesByPlayer = mutableMapOf<String, String>()

    sessions.sortedBy { it.session.date }.forEach { session ->
        session.entries.forEach { entryWithPlayer ->
            val playerId = entryWithPlayer.player.id
            namesByPlayer[playerId] = entryWithPlayer.player.name
            deltasByPlayer.getOrPut(playerId) { mutableListOf() }.add(entryWithPlayer.entry.deltaCents)
        }
    }

    return deltasByPlayer.map { (playerId, deltas) ->
        PlayerSessionResults(
            playerId = playerId,
            playerName = namesByPlayer[playerId].orEmpty(),
            deltasCents = deltas,
        )
    }
}
