package com.powerassetintelligence.unit.ai.rule;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.powerassetintelligence.core.ai.RiskFeatures;
import com.powerassetintelligence.core.ai.RiskRuleResult;
import com.powerassetintelligence.core.ai.rule.SustainedHighTemperatureRiskRule;
import com.powerassetintelligence.domain.model.AssetCriticality;
import com.powerassetintelligence.domain.model.AssetStatus;
import com.powerassetintelligence.domain.model.AssetType;
import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class SustainedHighTemperatureRiskRuleTest {

    private final SustainedHighTemperatureRiskRule rule = new SustainedHighTemperatureRiskRule();

    @Test
    void evaluateShouldTriggerWithSeverePeak() {
        RiskFeatures features = new RiskFeatures(
                java.util.UUID.randomUUID(),
                AssetType.TRANSFORMER,
                AssetStatus.ACTIVE,
                AssetCriticality.HIGH,
                10,
                BigDecimal.valueOf(90.0),
                BigDecimal.valueOf(70.0),
                1,
                0L,
                BigDecimal.valueOf(80.0),
                BigDecimal.valueOf(92.0),
                null, null, 0L, null, null
        );

        Optional<RiskRuleResult> result = rule.evaluate(features);

        assertTrue(result.isPresent());
        assertEquals("SUSTAINED_HIGH_TEMPERATURE", result.get().ruleCode());
        assertEquals(BigDecimal.valueOf(15), result.get().scoreContribution());
    }

    @Test
    void evaluateShouldTriggerSustainedWithoutPeak() {
        RiskFeatures features = new RiskFeatures(
                java.util.UUID.randomUUID(),
                AssetType.TRANSFORMER,
                AssetStatus.ACTIVE,
                AssetCriticality.HIGH,
                10,
                BigDecimal.valueOf(86.0),
                BigDecimal.valueOf(70.0),
                1,
                0L,
                BigDecimal.valueOf(76.0),
                BigDecimal.valueOf(88.0),
                null, null, 0L, null, null
        );

        Optional<RiskRuleResult> result = rule.evaluate(features);

        assertTrue(result.isPresent());
        assertEquals("SUSTAINED_HIGH_TEMPERATURE", result.get().ruleCode());
        assertEquals(BigDecimal.valueOf(12), result.get().scoreContribution());
    }

    @Test
    void evaluateShouldNotTriggerWithHighPeakButLowAverage() {
        RiskFeatures features = new RiskFeatures(
                java.util.UUID.randomUUID(),
                AssetType.TRANSFORMER,
                AssetStatus.ACTIVE,
                AssetCriticality.HIGH,
                10,
                BigDecimal.valueOf(90.0),
                BigDecimal.valueOf(70.0),
                1,
                0L,
                BigDecimal.valueOf(70.0),
                BigDecimal.valueOf(95.0),
                null, null, 0L, null, null
        );

        Optional<RiskRuleResult> result = rule.evaluate(features);

        assertTrue(result.isEmpty());
    }

    @Test
    void evaluateShouldNotTriggerBelowThresholds() {
        RiskFeatures features = new RiskFeatures(
                java.util.UUID.randomUUID(),
                AssetType.TRANSFORMER,
                AssetStatus.ACTIVE,
                AssetCriticality.HIGH,
                10,
                BigDecimal.valueOf(70.0),
                BigDecimal.valueOf(70.0),
                1,
                0L,
                BigDecimal.valueOf(65.0),
                BigDecimal.valueOf(80.0),
                null, null, 0L, null, null
        );

        Optional<RiskRuleResult> result = rule.evaluate(features);

        assertTrue(result.isEmpty());
    }

    @Test
    void evaluateShouldNotTriggerWhenTelemetryIsNull() {
        RiskFeatures features = new RiskFeatures(
                java.util.UUID.randomUUID(),
                AssetType.TRANSFORMER,
                AssetStatus.ACTIVE,
                AssetCriticality.HIGH,
                10,
                null,
                BigDecimal.valueOf(70.0),
                1,
                0L,
                null,
                null,
                null, null, 0L, null, null
        );

        Optional<RiskRuleResult> result = rule.evaluate(features);

        assertTrue(result.isEmpty());
    }

    @Test
    void evaluateShouldTriggerAtBoundaryThresholds() {
        RiskFeatures features = new RiskFeatures(
                java.util.UUID.randomUUID(),
                AssetType.TRANSFORMER,
                AssetStatus.ACTIVE,
                AssetCriticality.HIGH,
                10,
                BigDecimal.valueOf(85.0),
                BigDecimal.valueOf(70.0),
                1,
                0L,
                BigDecimal.valueOf(75.0),
                BigDecimal.valueOf(85.0),
                null, null, 0L, null, null
        );

        Optional<RiskRuleResult> result = rule.evaluate(features);

        assertTrue(result.isPresent());
        assertEquals("SUSTAINED_HIGH_TEMPERATURE", result.get().ruleCode());
        assertEquals(BigDecimal.valueOf(12), result.get().scoreContribution());
    }

    @Test
    void evaluateShouldNotTriggerJustBelowBoundaryThresholds() {
        RiskFeatures features = new RiskFeatures(
                java.util.UUID.randomUUID(),
                AssetType.TRANSFORMER,
                AssetStatus.ACTIVE,
                AssetCriticality.HIGH,
                10,
                BigDecimal.valueOf(84.0),
                BigDecimal.valueOf(70.0),
                1,
                0L,
                BigDecimal.valueOf(74.0),
                BigDecimal.valueOf(84.0),
                null, null, 0L, null, null
        );

        Optional<RiskRuleResult> result = rule.evaluate(features);

        assertTrue(result.isEmpty());
    }

    @Test
    void evaluateShouldNotTriggerWhenOnlyLatestIsNull() {
        RiskFeatures features = new RiskFeatures(
                java.util.UUID.randomUUID(),
                AssetType.TRANSFORMER,
                AssetStatus.ACTIVE,
                AssetCriticality.HIGH,
                10,
                null,
                BigDecimal.valueOf(70.0),
                1,
                0L,
                BigDecimal.valueOf(80.0),
                BigDecimal.valueOf(92.0),
                null, null, 0L, null, null
        );

        Optional<RiskRuleResult> result = rule.evaluate(features);

        assertTrue(result.isEmpty());
    }

    @Test
    void evaluateShouldNotTriggerWhenOnlyAverageIsNull() {
        RiskFeatures features = new RiskFeatures(
                java.util.UUID.randomUUID(),
                AssetType.TRANSFORMER,
                AssetStatus.ACTIVE,
                AssetCriticality.HIGH,
                10,
                BigDecimal.valueOf(90.0),
                BigDecimal.valueOf(70.0),
                1,
                0L,
                null,
                BigDecimal.valueOf(92.0),
                null, null, 0L, null, null
        );

        Optional<RiskRuleResult> result = rule.evaluate(features);

        assertTrue(result.isEmpty());
    }

    @Test
    void evaluateShouldHandleSeverePeakWhenMaxIsNull() {
        RiskFeatures features = new RiskFeatures(
                java.util.UUID.randomUUID(),
                AssetType.TRANSFORMER,
                AssetStatus.ACTIVE,
                AssetCriticality.HIGH,
                10,
                BigDecimal.valueOf(90.0),
                BigDecimal.valueOf(70.0),
                1,
                0L,
                BigDecimal.valueOf(80.0),
                null,
                null, null, 0L, null, null
        );

        Optional<RiskRuleResult> result = rule.evaluate(features);

        assertTrue(result.isPresent());
        assertEquals("SUSTAINED_HIGH_TEMPERATURE", result.get().ruleCode());
        assertEquals(BigDecimal.valueOf(12), result.get().scoreContribution());
    }
}
