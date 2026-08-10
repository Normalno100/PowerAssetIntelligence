package com.powerassetintelligence.application.dto;

import com.powerassetintelligence.core.ai.RiskFactor;
import java.math.BigDecimal;

/**
 * API representation of a structured risk factor.
 */
public record RiskFactorResponse(
        String code,
        String category,
        String description,
        BigDecimal value,
        BigDecimal threshold,
        String unit
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
                factor.category(),
                factor.description(),
                factor.value(),
                factor.threshold(),
                factor.unit()
        );
    }
}
