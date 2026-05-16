package com.powerassetintelligence.application.dto;

import com.powerassetintelligence.domain.model.RiskLevel;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record RiskAssessmentResponse(
        UUID id,
        UUID assetId,
        Instant assessedAt,
        BigDecimal riskScore,
        RiskLevel riskLevel,
        List<String> riskFactors,
        List<String> recommendations,
        String modelVersion,
        String explanation,
        Instant createdAt
) {
}
