package com.powerassetintelligence.application.dto;

import com.powerassetintelligence.domain.model.RiskFactor;
import com.powerassetintelligence.domain.model.RiskFactorSeverity;
import java.math.BigDecimal;

/**
 * API representation of a structured risk factor.
 */
public record RiskFactorResponse(
        String code,
        RiskFactorSeverity severity,
        String description,
        BigDecimal contribution
) {

    /**
     * Creates a RiskFactorResponse from a domain RiskFactor.
     */
    public static RiskFactorResponse from(RiskFactor factor) {
        if (factor == null) {
            return null;
        }
        return new RiskFactorResponse(
                factor.code(),
                factor.severity(),
                factor.description(),
                factor.contribution()
        );
    }
}
