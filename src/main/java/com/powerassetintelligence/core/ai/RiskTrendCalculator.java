package com.powerassetintelligence.core.ai;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Pure calculation component for risk trend analysis.
 * <p>
 * Handles only arithmetic: score changes, direction, averages.
 * Zero dependencies on repositories, Spring, or domain models.
 */
public final class RiskTrendCalculator {

    private static final int AVERAGE_SCALE = 2;
    private static final RoundingMode AVERAGE_ROUNDING = RoundingMode.HALF_UP;

    private RiskTrendCalculator() {
        // utility class
    }

    /**
     * Computes all trend statistics from a list of risk scores in chronological order.
     *
     * @param scores list of scores from oldest to newest (must not be null)
     * @return trend result containing per-point changes and summary statistics
     */
    public static TrendResult calculate(List<BigDecimal> scores) {
        if (scores == null || scores.isEmpty()) {
            return new TrendResult(
                    Collections.emptyList(),
                    null, null, null, null, TrendDirection.STABLE
            );
        }

        int size = scores.size();
        List<ScoreChange> changes = new ArrayList<>(size);

        // Per-point scoreChange and trend
        for (int i = 0; i < size; i++) {
            if (i == 0) {
                changes.add(ScoreChange.first());
            } else {
                BigDecimal diff = scores.get(i).subtract(scores.get(i - 1));
                TrendDirection pointTrend = computePointDirection(diff);
                changes.add(new ScoreChange(diff, pointTrend));
            }
        }

        BigDecimal currentScore = scores.get(size - 1);
        BigDecimal previousScore = size > 1 ? scores.get(size - 2) : null;
        BigDecimal totalChange = size > 1
                ? currentScore.subtract(scores.get(0))
                : BigDecimal.ZERO;

        TrendDirection direction;
        BigDecimal averageChange;

        if (size > 1) {
            BigDecimal sumChange = BigDecimal.ZERO;
            for (int i = 1; i < size; i++) {
                sumChange = sumChange.add(scores.get(i).subtract(scores.get(i - 1)));
            }
            averageChange = sumChange.divide(
                    BigDecimal.valueOf(size - 1),
                    AVERAGE_SCALE,
                    AVERAGE_ROUNDING
            );
            direction = computeOverallDirection(scores.get(0), currentScore);
        } else {
            averageChange = null;
            direction = TrendDirection.STABLE;
        }

        return new TrendResult(changes, currentScore, previousScore, totalChange, averageChange, direction);
    }

    private static TrendDirection computePointDirection(BigDecimal diff) {
        int cmp = diff.compareTo(BigDecimal.ZERO);
        if (cmp > 0) return TrendDirection.RISING;
        if (cmp < 0) return TrendDirection.FALLING;
        return TrendDirection.STABLE;
    }

    private static TrendDirection computeOverallDirection(BigDecimal first, BigDecimal last) {
        int cmp = last.compareTo(first);
        if (cmp > 0) return TrendDirection.RISING;
        if (cmp < 0) return TrendDirection.FALLING;
        return TrendDirection.STABLE;
    }

    // -----------------------------------------------------------------
    // Result record
    // -----------------------------------------------------------------

    /**
     * Immutable result of trend calculation.
     */
    public record TrendResult(
            List<ScoreChange> scoreChanges,
            BigDecimal currentScore,
            BigDecimal previousScore,
            BigDecimal totalChange,
            BigDecimal averageChange,
            TrendDirection direction
    ) {}

    /**
     * Per-point change: the delta and its direction.
     */
    public record ScoreChange(
            BigDecimal delta,
            TrendDirection trend
    ) {
        public static ScoreChange first() {
            return new ScoreChange(null, TrendDirection.STABLE);
        }
    }
}
