package com.powerassetintelligence.application.dto;

import com.powerassetintelligence.core.ai.RiskChangeDirection;
import com.powerassetintelligence.domain.model.RiskLevel;
import java.math.BigDecimal;
import java.util.List;

/**
 * API response for comparing the latest risk assessment with the previous one.
 *
 * <pre>
 * Example:
 * {
 *   "currentScore": 82,
 *   "previousScore": 65,
 *   "scoreDelta": 17,
 *   "currentLevel": "HIGH",
 *   "previousLevel": "MEDIUM",
 *   "direction": "INCREASED",
 *   "factorChanges": [ ... ]
 * }
 * </pre>
 */
public record RiskAssessmentComparisonResponse(
        BigDecimal currentScore,
        BigDecimal previousScore,
        BigDecimal scoreDelta,
        RiskLevel currentLevel,
        RiskLevel previousLevel,
        RiskChangeDirection direction,
        List<RiskFactorChangeResponse> factorChanges
) {
}
