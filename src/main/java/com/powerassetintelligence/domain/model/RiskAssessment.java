package com.powerassetintelligence.domain.model;

import com.powerassetintelligence.core.ai.RiskFactor;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record RiskAssessment(UUID id, UUID assetId, Instant assessedAt, BigDecimal riskScore, RiskLevel riskLevel,
        List<RiskFactor> riskFactors, List<String> recommendations, String modelVersion, String explanation,
        Instant createdAt) {
    public RiskAssessment {
        riskFactors = List.copyOf(riskFactors == null ? List.of() : riskFactors);
        recommendations = List.copyOf(recommendations == null ? List.of() : recommendations);
    }

    public RiskAssessment(UUID id, UUID assetId, Instant assessedAt, BigDecimal riskScore, RiskLevel riskLevel,
            List<RiskFactor> riskFactors, List<String> recommendations, String modelVersion, String explanation) {
        this(id, assetId, assessedAt, riskScore, riskLevel, riskFactors, recommendations, modelVersion, explanation, null);
    }
}
