package com.powerassetintelligence.unit.ai.rule;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.powerassetintelligence.core.ai.RiskFactor;
import com.powerassetintelligence.core.ai.RiskFactorSeverity;
import com.powerassetintelligence.core.ai.RiskFeatures;
import com.powerassetintelligence.core.ai.RiskRuleResult;
import com.powerassetintelligence.core.ai.rule.TemperatureTrendRiskRule;
import com.powerassetintelligence.domain.model.AssetCriticality;
import com.powerassetintelligence.domain.model.AssetStatus;
import com.powerassetintelligence.domain.model.AssetType;
import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class TemperatureTrendRiskRuleTest {

    private final TemperatureTrendRiskRule rule = new TemperatureTrendRiskRule();

    @Test
    void evaluateShouldTriggerCriticalTrend() {
        RiskFeatures features = new RiskFeatures(
                java.util.UUID.randomUUID(),
                AssetType.TRANSFORMER,
                AssetStatus.ACTIVE,
                AssetCriticality.HIGH,
                10,
                BigDecimal.valueOf(72.0), // latest >= 70
                BigDecimal.valueOf(60.0),
                0,
                0L, null, null, null, null, 0L,
                BigDecimal.valueOf(3.5), // trend >= 3.0
                null
        );

        Optional<RiskRuleResult> result = rule.evaluate(features);

        assertTrue(result.isPresent());
        assertEquals("CRITICAL_TEMPERATURE_TREND", result.get().ruleCode());
        RiskFactor factor = result.get().riskFactor();
        assertEquals(BigDecimal.valueOf(30), factor.contribution());
        assertEquals(RiskFactorSeverity.CRITICAL, factor.severity());
    }

    @Test
    void evaluateShouldTriggerHighTrend() {
        RiskFeatures features = new RiskFeatures(
                java.util.UUID.randomUUID(),
                AssetType.TRANSFORMER,
                AssetStatus.ACTIVE,
                AssetCriticality.HIGH,
                10,
                BigDecimal.valueOf(76.0), // latest >= 75
                BigDecimal.valueOf(60.0),
                0,
                0L, null, null, null, null, 0L,
                BigDecimal.valueOf(2.5), // trend >= 2.0
                null
        );

        Optional<RiskRuleResult> result = rule.evaluate(features);

        assertTrue(result.isPresent());
        assertEquals("HIGH_TEMPERATURE_TREND", result.get().ruleCode());
        RiskFactor factor = result.get().riskFactor();
        assertEquals(BigDecimal.valueOf(22), factor.contribution());
        assertEquals(RiskFactorSeverity.HIGH, factor.severity());
    }

    @Test
    void evaluateShouldTriggerElevatedTrend() {
        RiskFeatures features = new RiskFeatures(
                java.util.UUID.randomUUID(),
                AssetType.TRANSFORMER,
                AssetStatus.ACTIVE,
                AssetCriticality.HIGH,
                10,
                BigDecimal.valueOf(81.0), // latest >= 80
                BigDecimal.valueOf(60.0),
                0,
                0L, null, null, null, null, 0L,
                BigDecimal.valueOf(1.5), // trend >= 1.0
                null
        );

        Optional<RiskRuleResult> result = rule.evaluate(features);

        assertTrue(result.isPresent());
        assertEquals("ELEVATED_TEMPERATURE_TREND", result.get().ruleCode());
        RiskFactor factor = result.get().riskFactor();
        assertEquals(BigDecimal.valueOf(15), factor.contribution());
        assertEquals(RiskFactorSeverity.MEDIUM, factor.severity());
    }

    @Test
    void evaluateShouldNotTriggerWhenTrendBelowThreshold() {
        RiskFeatures features = new RiskFeatures(
                java.util.UUID.randomUUID(),
                AssetType.TRANSFORMER,
                AssetStatus.ACTIVE,
                AssetCriticality.HIGH,
                10,
                BigDecimal.valueOf(90.0),
                BigDecimal.valueOf(60.0),
                0,
                0L, null, null, null, null, 0L,
                BigDecimal.valueOf(0.2), // < 0.3 noise threshold
                null
        );

        Optional<RiskRuleResult> result = rule.evaluate(features);

        assertTrue(result.isEmpty());
    }

    @Test
    void evaluateShouldNotTriggerWhenTrendIsNull() {
        RiskFeatures features = new RiskFeatures(
                java.util.UUID.randomUUID(),
                AssetType.TRANSFORMER,
                AssetStatus.ACTIVE,
                AssetCriticality.HIGH,
                10,
                BigDecimal.valueOf(90.0),
                BigDecimal.valueOf(60.0),
                0,
                0L, null, null, null, null, 0L,
                null,
                null
        );

        Optional<RiskRuleResult> result = rule.evaluate(features);

        assertTrue(result.isEmpty());
    }
}
