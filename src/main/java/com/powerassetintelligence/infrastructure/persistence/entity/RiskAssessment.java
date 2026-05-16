package com.powerassetintelligence.infrastructure.persistence.entity;

import com.powerassetintelligence.domain.model.RiskLevel;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(
        name = "risk_assessments",
        indexes = {
                @Index(name = "idx_risk_assessments_asset_assessed_at", columnList = "asset_id,assessed_at DESC"),
                @Index(name = "idx_risk_assessments_level_score", columnList = "risk_level,risk_score DESC")
        }
)
public class RiskAssessment {

    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "asset_id", nullable = false)
    private Asset asset;

    @Column(name = "assessed_at", nullable = false)
    private Instant assessedAt;

    @Column(name = "risk_score", nullable = false, precision = 5, scale = 2)
    private BigDecimal riskScore;

    @Enumerated(EnumType.STRING)
    @Column(name = "risk_level", nullable = false, length = 32)
    private RiskLevel riskLevel;

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "risk_assessment_factors", joinColumns = @JoinColumn(name = "risk_assessment_id"))
    @Column(name = "factor", nullable = false, length = 1_000)
    private List<String> riskFactors = new ArrayList<>();

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "risk_assessment_recommendations", joinColumns = @JoinColumn(name = "risk_assessment_id"))
    @Column(name = "recommendation", nullable = false, length = 1_000)
    private List<String> recommendations = new ArrayList<>();

    @Column(name = "model_version", nullable = false, length = 64)
    private String modelVersion;

    @Column(nullable = false, length = 4_000)
    private String explanation;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    protected RiskAssessment() {
    }

    public RiskAssessment(
            UUID id,
            Asset asset,
            Instant assessedAt,
            BigDecimal riskScore,
            RiskLevel riskLevel,
            List<String> riskFactors,
            List<String> recommendations,
            String modelVersion,
            String explanation
    ) {
        this.id = id;
        this.asset = asset;
        this.assessedAt = assessedAt;
        this.riskScore = riskScore;
        this.riskLevel = riskLevel;
        this.riskFactors = new ArrayList<>(riskFactors == null ? List.of() : riskFactors);
        this.recommendations = new ArrayList<>(recommendations == null ? List.of() : recommendations);
        this.modelVersion = modelVersion;
        this.explanation = explanation;
    }

    @PrePersist
    void prePersist() {
        createdAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public Asset getAsset() {
        return asset;
    }

    public Instant getAssessedAt() {
        return assessedAt;
    }

    public BigDecimal getRiskScore() {
        return riskScore;
    }

    public RiskLevel getRiskLevel() {
        return riskLevel;
    }

    public List<String> getRiskFactors() {
        return riskFactors;
    }

    public List<String> getRecommendations() {
        return recommendations;
    }

    public String getModelVersion() {
        return modelVersion;
    }

    public String getExplanation() {
        return explanation;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
