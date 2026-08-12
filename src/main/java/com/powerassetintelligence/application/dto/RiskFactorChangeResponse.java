package com.powerassetintelligence.application.dto;

import java.math.BigDecimal;

/**
 * Represents the change of a single risk factor between two assessments.
 */
public record RiskFactorChangeResponse(
        String code,
        RiskFactorResponse previous,
        RiskFactorResponse current,
        BigDecimal contributionDelta
) {
}
