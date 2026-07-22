package com.powerassetintelligence.unit.ai;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.powerassetintelligence.application.dto.RiskFeatures;
import com.powerassetintelligence.application.dto.RiskScoringResult;
import com.powerassetintelligence.core.ai.RuleBasedRiskEngine;
import com.powerassetintelligence.domain.model.AssetCriticality;
import com.powerassetintelligence.domain.model.AssetStatus;
import com.powerassetintelligence.domain.model.AssetType;
import com.powerassetintelligence.domain.model.RiskLevel;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class RuleBasedRiskEngineTest {

    private final RuleBasedRiskEngine engine = new RuleBasedRiskEngine();

    @Test
    void scoreShouldApplyCriticalityBonus() {
        UUID assetId = UUID.randomUUID();
        LocalDate today = LocalDate.now();
        int age = today.minusYears(20).getYear() - today.minusYears(15).getYear();

        RiskFeatures features = new RiskFeatures(
                assetId,
                AssetType.TRANSFORMER,
                AssetStatus.ACTIVE,
                AssetCriticality.CRITICAL,
                age,
                BigDecimal.valueOf(85.0),
                BigDecimal.valueOf(80.0),
                1,
                0L
        );

        RiskScoringResult result = engine.score(features);

        assertNotNull(result);
        assertTrue(result.riskScore().compareTo(BigDecimal.ZERO) > 0);
        assertTrue(result.riskFactors().size() >= 1);
    }

    @Test
    void scoreShouldTriggerAllMatchingRules() {
        UUID assetId = UUID.randomUUID();
        RiskFeatures features = new RiskFeatures(
                assetId,
                AssetType.CIRCUIT_BREAKER,
                AssetStatus.ACTIVE,
                AssetCriticality.HIGH,
                25, // Very old
                BigDecimal.valueOf(96.0), // Critical temperature
                BigDecimal.valueOf(95.0), // High load
                5, // Frequent overheating
                5L // Frequent repairs
        );

        RiskScoringResult result = engine.score(features);

        assertNotNull(result);
        assertEquals(RiskLevel.CRITICAL, result.riskLevel());
        assertTrue(result.riskFactors().size() >= 3);
        assertTrue(result.recommendations().size() >= 1);
    }

    @Test
    void scoreShouldCapAt100() {
        UUID assetId = UUID.randomUUID();
        RiskFeatures features = new RiskFeatures(
                assetId,
                AssetType.TRANSFORMER,
                AssetStatus.ACTIVE,
                AssetCriticality.CRITICAL,
                25,
                BigDecimal.valueOf(96.0),
                BigDecimal.valueOf(95.0),
                5,
                5L
        );

        RiskScoringResult result = engine.score(features);

        assertTrue(result.riskScore().compareTo(BigDecimal.valueOf(100)) <= 0);
    }

    @Test
    void scoreShouldReturnBaselineWhenNoRulesTrigger() {
        UUID assetId = UUID.randomUUID();
        RiskFeatures features = new RiskFeatures(
                assetId,
                AssetType.CIRCUIT_BREAKER,
                AssetStatus.ACTIVE,
                AssetCriticality.LOW,
                5, // New asset
                BigDecimal.valueOf(60.0), // Normal temperature
                BigDecimal.valueOf(50.0), // Normal load
                0, // No overheating
                0L // No repairs
        );

        RiskScoringResult result = engine.score(features);

        assertNotNull(result);
        assertTrue(result.riskFactors().contains("BASELINE: No risk rules were triggered"));
        assertTrue(result.recommendations().contains("Continue routine monitoring"));
    }

    @Test
    void scoreShouldIncludeModelVersion() {
        RiskFeatures features = new RiskFeatures(
                UUID.randomUUID(),
                AssetType.TRANSFORMER,
                AssetStatus.ACTIVE,
                AssetCriticality.MEDIUM,
                10,
                BigDecimal.valueOf(70.0),
                BigDecimal.valueOf(60.0),
                0,
                0L
        );

        RiskScoringResult result = engine.score(features);

        assertEquals("rules-2026.05", result.modelVersion());
    }

    @Test
    void scoreShouldCalculateCorrectRiskLevel() {
        RiskFeatures features = new RiskFeatures(
                UUID.randomUUID(),
                AssetType.TRANSFORMER,
                AssetStatus.ACTIVE,
                AssetCriticality.MEDIUM,
                20,
                BigDecimal.valueOf(85.0),
                BigDecimal.valueOf(70.0),
                1,
                2L
        );

        RiskScoringResult result = engine.score(features);

        assertTrue(result.riskScore().compareTo(BigDecimal.valueOf(35)) >= 0);
        assertTrue(result.riskLevel() == RiskLevel.MEDIUM || result.riskLevel() == RiskLevel.HIGH);
    }

    @Test
    void scoreShouldAddCriticalityBonusForHigh() {
        RiskFeatures features = new RiskFeatures(
                UUID.randomUUID(),
                AssetType.TRANSFORMER,
                AssetStatus.ACTIVE,
                AssetCriticality.HIGH,
                10,
                BigDecimal.valueOf(70.0),
                BigDecimal.valueOf(60.0),
                0,
                0L
        );

        RiskScoringResult result = engine.score(features);

        assertNotNull(result);
        assertTrue(result.riskFactors().stream().anyMatch(f -> f.contains("ASSET_CRITICALITY")));
    }
}
