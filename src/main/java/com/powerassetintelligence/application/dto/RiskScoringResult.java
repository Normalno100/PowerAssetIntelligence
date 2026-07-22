package com.powerassetintelligence.application.dto;

import com.powerassetintelligence.domain.model.RiskLevel;
import java.math.BigDecimal;
import java.util.List;

public record RiskScoringResult(
        BigDecimal riskScore,
        RiskLevel riskLevel,
        List<String> riskFactors,
        List<String> recommendations,
        String explanation,
        String modelVersion
) {
}
