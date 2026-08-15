package com.powerassetintelligence.domain.model;

import com.powerassetintelligence.core.ai.RiskAssessmentSnapshot;
import com.powerassetintelligence.core.ai.RiskFactor;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Immutable record representing a risk assessment result.
 * <p>
 * The {@code snapshot} field preserves the exact input features used to compute this
 * assessment, enabling full reproducibility and auditability of risk decisions.
 * For assessments created before snapshot support, this field may be null.
 * </p>
 */
public record RiskAssessment(
        UUID id,
        UUID assetId,
        Instant assessedAt,
        BigDecimal riskScore,
        RiskLevel riskLevel,
        List<RiskFactor> riskFactors,
        List<String> recommendations,
        String modelVersion,
        String explanation,
        Instant createdAt,
        RiskAssessmentSnapshot snapshot) {

    public RiskAssessment {
        riskFactors = List.copyOf(riskFactors == null ? List.of() : riskFactors);
        recommendations = List.copyOf(recommendations == null ? List.of() : recommendations);
    }

    /**
     * Convenience constructor for backward compatibility.
     * Creates an assessment with null createdAt and snapshot.
     */
    public RiskAssessment(UUID id, UUID assetId, Instant assessedAt, BigDecimal riskScore, RiskLevel riskLevel,
            List<RiskFactor> riskFactors, List<String> recommendations, String modelVersion, String explanation) {
        this(id, assetId, assessedAt, riskScore, riskLevel, riskFactors, recommendations, modelVersion, explanation,
                null, null);
    }
}
