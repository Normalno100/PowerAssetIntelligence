package com.powerassetintelligence.core.ai;

import com.powerassetintelligence.domain.model.RiskLevel;
import java.math.BigDecimal;
import java.util.List;

/**
 * Result of a complete risk scoring pass.
 * <p>
 * <pre>
 * Example:
 *   new RiskScoringResult(
 *       BigDecimal.valueOf(82),
 *       RiskLevel.HIGH,
 *       List.of(factor1, factor2),
 *       List.of("Inspect thermal condition"),
 *       "Rule-based assessment evaluated 2 triggered rules...",
 *       "rules-2026.05"
 *   )
 * </pre>
 */
public record RiskScoringResult(
        BigDecimal riskScore,
        RiskLevel riskLevel,
        List<RiskFactor> riskFactors,
        List<String> recommendations,
        String explanation,
        String modelVersion
) {
}
