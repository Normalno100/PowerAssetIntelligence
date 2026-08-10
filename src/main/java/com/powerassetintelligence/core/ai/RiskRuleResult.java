package com.powerassetintelligence.core.ai;

import java.math.BigDecimal;
import java.util.List;

/**
 * Result of a single {@link RiskRule} evaluation.
 * <p>
 * Contains both the legacy {@code String} riskFactor (for backward
 * compatibility) and an optional structured {@link RiskFactor} for
 * future AI/LLM-powered risk explanations.
 */
public record RiskRuleResult(
        String ruleCode,
        BigDecimal scoreContribution,
        String riskFactor,
        List<String> recommendations,
        RiskFactor structuredRiskFactor
) {

    /**
     * Legacy factory method — maintains backward compatibility.
     */
    public static RiskRuleResult of(
            String ruleCode,
            BigDecimal scoreContribution,
            String riskFactor,
            List<String> recommendations
    ) {
        return new RiskRuleResult(ruleCode, scoreContribution, riskFactor, List.copyOf(recommendations), null);
    }

    /**
     * Factory method with structured risk factor.
     */
    public static RiskRuleResult of(
            String ruleCode,
            BigDecimal scoreContribution,
            String riskFactor,
            List<String> recommendations,
            RiskFactor structuredRiskFactor
    ) {
        return new RiskRuleResult(ruleCode, scoreContribution, riskFactor, List.copyOf(recommendations), structuredRiskFactor);
    }
}
