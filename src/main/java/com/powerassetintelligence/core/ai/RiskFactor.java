package com.powerassetintelligence.core.ai;

import java.math.BigDecimal;

/**
 * Structured representation of a single risk factor.
 * <p>
 * This record is the building block for future AI/LLM-powered risk
 * explanations. It replaces the legacy {@code String} risk factor
 * format while maintaining backward compatibility through
 * {@link #toString()}.
 *
 * <pre>
 * Example usage:
 *   RiskFactor factor = RiskFactor.of(
 *       "TEMP_TREND",
 *       "TEMPERATURE",
 *       "Temperature is increasing rapidly",
 *       BigDecimal.valueOf(1.4),
 *       BigDecimal.valueOf(1.0),
 *       "CELSIUS_PER_HOUR"
 *   );
 * </pre>
 *
 * @see RiskRuleResult
 * @see RiskScoringResult
 */
public record RiskFactor(
        /**
         * Unique machine-readable code for this factor.
         * Example: "TEMP_HIGH", "TEMP_TREND", "LOAD_HIGH", "FREQUENT_REPAIRS"
         */
        String code,

        /**
         * High-level category grouping related factors.
         * Example: "TEMPERATURE", "LOAD", "MAINTENANCE", "AGE"
         */
        String category,

        /**
         * Human-readable description of the risk.
         */
        String description,

        /**
         * The actual measured value that triggered the risk.
         * Example: 91 (for temperature in °C), 87 (for load in %), 4 (for repair count)
         */
        BigDecimal value,

        /**
         * The threshold that was exceeded to trigger this risk.
         * Example: 85 (for temperature threshold), 80 (for load threshold), 3 (for repairs threshold)
         */
        BigDecimal threshold,

        /**
         * Unit of measurement for the value.
         * Example: "CELSIUS", "PERCENT", "COUNT", "CELSIUS_PER_HOUR"
         */
        String unit
) {

    /**
     * Factory method for creating a RiskFactor.
     */
    public static RiskFactor of(
            String code,
            String category,
            String description,
            BigDecimal value,
            BigDecimal threshold,
            String unit
    ) {
        return new RiskFactor(code, category, description, value, threshold, unit);
    }

    /**
     * Converts this RiskFactor to a legacy-compatible String format.
     * <p>
     * Example output: {@code "TEMP_HIGH: Temperature is too high (value=91, threshold=85)"}
     * <p>
     * This method ensures backward compatibility with existing
     * {@code List<String>} APIs.
     */
    @Override
    public String toString() {
        if (value != null && threshold != null && unit != null) {
            return String.format("%s: %s (value=%s %s, threshold=%s %s)",
                    code, description, value, unit, threshold, unit);
        }
        if (value != null) {
            return String.format("%s: %s (value=%s)", code, description, value);
        }
        return String.format("%s: %s", code, description);
    }
}
