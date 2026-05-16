package com.powerassetintelligence.core.ai;

import java.math.BigDecimal;
import java.util.List;

public record RiskRuleResult(
        String ruleCode,
        BigDecimal scoreContribution,
        String riskFactor,
        List<String> recommendations
) {

    public static RiskRuleResult of(
            String ruleCode,
            BigDecimal scoreContribution,
            String riskFactor,
            List<String> recommendations
    ) {
        return new RiskRuleResult(ruleCode, scoreContribution, riskFactor, List.copyOf(recommendations));
    }
}
