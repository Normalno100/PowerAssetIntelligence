package com.powerassetintelligence.core.ai;

/**
 * Direction of risk trend over a period of multiple assessments.
 * <p>
 * Uses a tolerance band to avoid flagging minor fluctuations as trends.
 */
public enum RiskTrendDirection {
    /** Risk score is generally increasing (current > first + tolerance) */
    INCREASING,
    /** Risk score is generally decreasing (current < first - tolerance) */
    DECREASING,
    /** Risk score is stable within tolerance band */
    STABLE,
    /** Not enough data to determine a trend (0 or 1 assessment) */
    INSUFFICIENT_DATA
}
