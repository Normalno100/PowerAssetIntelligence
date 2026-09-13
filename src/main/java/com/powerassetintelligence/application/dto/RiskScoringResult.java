package com.powerassetintelligence.application.dto;

import com.powerassetintelligence.domain.model.RiskLevel;
import java.math.BigDecimal;
import java.util.List;

/**
 * Result of a deterministic risk scoring pass.
 * <p>
 * This record contains only the numeric and structured scoring output.
 * Human-readable explanations and recommendations are generated separately
 * via {@link com.powerassetintelligence.core.ai.RiskExplanationService} to enable different explanation strategies.
 * <p>
 * <pre>
 * Example:
 *   new RiskScoringResult(
 *       BigDecimal.valueOf(82),
 *       RiskLevel.HIGH,
 *       List.of(factor1, factor2)
 *   )
 * </pre>
 *
 * @see com.powerassetintelligence.core.ai.RiskExplanationService
 */
public record RiskScoringResult(

        /**
         * Numeric risk score, 0-100.
         */
        BigDecimal riskScore,

        /**
         * Risk level derived from score thresholds.
         */
        com.powerassetintelligence.domain.model.RiskLevel riskLevel,

        /**
         * Ordered list of contributing risk factors.
         */
        List<com.powerassetintelligence.core.ai.RiskFactor> riskFactors
) {
}
