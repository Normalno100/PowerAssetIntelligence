package com.powerassetintelligence.core.ai;

import java.math.BigDecimal;

/**
 * Structured representation of a single risk factor.
 * <p>
 * <pre>
 * Example:
 *   RiskFactor.of("HIGH_TEMPERATURE", RiskFactorSeverity.HIGH,
 *       "Temperature above 80°C", BigDecimal.valueOf(20))
 * </pre>
 *
 * @see RiskRuleResult
 * @see RiskScoringResult
 */
public record RiskFactor(

        /**
         * Unique machine-readable code for this factor.
         * Example: "TEMP_HIGH", "AGE_OLD", "LOAD_CRITICAL"
         */
        String code,

        /**
         * Severity level indicating the seriousness of this factor.
         */
        RiskFactorSeverity severity,

        /**
         * Human-readable description of the risk.
         */
        String description,

        /**
         * Contribution of this factor to the total risk score.
         */
        BigDecimal contribution
) {

    /**
     * Factory method for creating a RiskFactor.
     */
    public static RiskFactor of(
            String code,
            RiskFactorSeverity severity,
            String description,
            BigDecimal contribution
    ) {
        return new RiskFactor(code, severity, description, contribution);
    }
}
