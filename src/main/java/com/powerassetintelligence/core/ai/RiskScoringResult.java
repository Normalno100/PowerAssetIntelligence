package com.powerassetintelligence.core.ai;

import com.powerassetintelligence.domain.model.RiskLevel;
import java.math.BigDecimal;
import java.util.List;

/**
 * Result of a deterministic risk scoring pass.
 * <p>
 * This record contains only the numeric and structured scoring output.
 * Human-readable explanations and recommendations are generated separately
 * via {@link RiskExplanationService} to enable different explanation strategies.
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
 * @see RiskExplanationService
 */
public record RiskScoringResult(

        /**
         * Numeric risk score, 0-100.
         */
        BigDecimal riskScore,

        /**
         * Risk level derived from score thresholds.
         */
        RiskLevel riskLevel,

        /**
         * Ordered list of contributing risk factors.
         */
        List<RiskFactor> riskFactors
) {
}
