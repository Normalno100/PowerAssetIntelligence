package com.powerassetintelligence.core.ai;

import java.math.BigDecimal;
import java.util.List;

/**
 * Result of a single {@link RiskRule} evaluation.
 *
 * <pre>
 * Example:
 *   RiskRuleResult.of("HIGH_TEMPERATURE",
 *       RiskFactor.of("HIGH_TEMPERATURE", RiskFactorSeverity.HIGH,
 *           "Temperature above 80°C", BigDecimal.valueOf(20)),
 *       List.of("Inspect thermal condition"))
 * </pre>
 */
public record RiskRuleResult(
        String ruleCode,
        RiskFactor riskFactor,
        List<String> recommendations
) {

    /**
     * Factory method for creating a RiskRuleResult.
     */
    public static RiskRuleResult of(
            String ruleCode,
            RiskFactor riskFactor,
            List<String> recommendations
    ) {
        return new RiskRuleResult(ruleCode, riskFactor, List.copyOf(recommendations));
    }
}
