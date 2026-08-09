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
        // Test 1: latest=90, average=80, max=92
        // Both sustained (>=85, >=75) AND peak (>=90) → score=15
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
                null, null, 0L
        );

        Optional<RiskRuleResult> result = rule.evaluate(features);

        assertTrue(result.isPresent());
        assertEquals("SUSTAINED_HIGH_TEMPERATURE", result.get().ruleCode());
        assertEquals(BigDecimal.valueOf(15), result.get().scoreContribution());
    }

    @Test
    void evaluateShouldTriggerSustainedWithoutPeak() {
        // Test 2: latest=86, average=76, max=88
        // Sustained but no peak (max < 90) → score=12
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
                null, null, 0L
        );

        Optional<RiskRuleResult> result = rule.evaluate(features);

        assertTrue(result.isPresent());
        assertEquals("SUSTAINED_HIGH_TEMPERATURE", result.get().ruleCode());
        assertEquals(BigDecimal.valueOf(12), result.get().scoreContribution());
    }

    @Test
    void evaluateShouldNotTriggerWithHighPeakButLowAverage() {
        // Test 3: latest=90, average=70, max=95
        // Peak >= 90 but average < 75 → sustained condition not met → empty
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
                null, null, 0L
        );

        Optional<RiskRuleResult> result = rule.evaluate(features);

        assertTrue(result.isEmpty());
    }

    @Test
    void evaluateShouldNotTriggerBelowThresholds() {
        // Test 4: latest=70, average=65, max=80
        // All below thresholds → empty
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
                null, null, 0L
        );

        Optional<RiskRuleResult> result = rule.evaluate(features);

        assertTrue(result.isEmpty());
    }

    @Test
    void evaluateShouldNotTriggerWhenTelemetryIsNull() {
        // Test 5: all temperature fields are null → empty
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
                null, null, 0L
        );

        Optional<RiskRuleResult> result = rule.evaluate(features);

        assertTrue(result.isEmpty());
    }

    @Test
    void evaluateShouldTriggerAtBoundaryThresholds() {
        // Test 6: latest=85, average=75 — exactly at boundary
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
                null, null, 0L
        );

        Optional<RiskRuleResult> result = rule.evaluate(features);

        assertTrue(result.isPresent());
        assertEquals("SUSTAINED_HIGH_TEMPERATURE", result.get().ruleCode());
        assertEquals(BigDecimal.valueOf(12), result.get().scoreContribution());
    }

    @Test
    void evaluateShouldNotTriggerJustBelowBoundaryThresholds() {
        // Test 6b: latest=84, average=74 — just below boundary
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
                null, null, 0L
        );

        Optional<RiskRuleResult> result = rule.evaluate(features);

        assertTrue(result.isEmpty());
    }

    @Test
    void evaluateShouldNotTriggerWhenOnlyLatestIsNull() {
        // Edge case: average and max are present but latest is null
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
                null, null, 0L
        );

        Optional<RiskRuleResult> result = rule.evaluate(features);

        assertTrue(result.isEmpty());
    }

    @Test
    void evaluateShouldNotTriggerWhenOnlyAverageIsNull() {
        // Edge case: latest and max are present but average is null
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
                null, null, 0L
        );

        Optional<RiskRuleResult> result = rule.evaluate(features);

        assertTrue(result.isEmpty());
    }

    @Test
    void evaluateShouldHandleSeverePeakWhenMaxIsNull() {
        // Edge case: sustained condition met but max is null
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
                null, null, 0L
        );

        Optional<RiskRuleResult> result = rule.evaluate(features);

        assertTrue(result.isPresent());
        assertEquals("SUSTAINED_HIGH_TEMPERATURE", result.get().ruleCode());
        assertEquals(BigDecimal.valueOf(12), result.get().scoreContribution());
    }
}
