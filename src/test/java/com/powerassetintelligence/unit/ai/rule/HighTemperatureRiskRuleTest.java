package com.powerassetintelligence.unit.ai.rule;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.powerassetintelligence.core.ai.RiskFeatures;
import com.powerassetintelligence.core.ai.RiskRuleResult;
import com.powerassetintelligence.core.ai.rule.HighTemperatureRiskRule;
import com.powerassetintelligence.domain.model.AssetCriticality;
import com.powerassetintelligence.domain.model.AssetStatus;
import com.powerassetintelligence.domain.model.AssetType;
import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class HighTemperatureRiskRuleTest {

    private final HighTemperatureRiskRule rule = new HighTemperatureRiskRule();

    @Test
    void evaluateShouldTriggerCriticalTemperature() {
        RiskFeatures features = new RiskFeatures(
                java.util.UUID.randomUUID(),
                AssetType.TRANSFORMER,
                AssetStatus.ACTIVE,
                AssetCriticality.HIGH,
                10,
                BigDecimal.valueOf(96.0), // >= 95°C
                BigDecimal.valueOf(70.0),
                1,
                0L, null, null, null, null, 0L
        );

        Optional<RiskRuleResult> result = rule.evaluate(features);

        assertTrue(result.isPresent());
        assertEquals("CRITICAL_TEMPERATURE", result.get().ruleCode());
        assertEquals(BigDecimal.valueOf(35), result.get().scoreContribution());
    }

    @Test
    void evaluateShouldTriggerHighTemperature() {
        RiskFeatures features = new RiskFeatures(
                java.util.UUID.randomUUID(),
                AssetType.TRANSFORMER,
                AssetStatus.ACTIVE,
                AssetCriticality.HIGH,
                10,
                BigDecimal.valueOf(81.0), // > 80°C
                BigDecimal.valueOf(70.0),
                1,
                0L, null, null, null, null, 0L
        );

        Optional<RiskRuleResult> result = rule.evaluate(features);

        assertTrue(result.isPresent());
        assertEquals("HIGH_TEMPERATURE", result.get().ruleCode());
        assertEquals(BigDecimal.valueOf(20), result.get().scoreContribution());
    }

    @Test
    void evaluateShouldNotTriggerWhenTemperatureBelowThreshold() {
        RiskFeatures features = new RiskFeatures(
                java.util.UUID.randomUUID(),
                AssetType.TRANSFORMER,
                AssetStatus.ACTIVE,
                AssetCriticality.HIGH,
                10,
                BigDecimal.valueOf(80.0), // <= 80°C
                BigDecimal.valueOf(70.0),
                1,
                0L, null, null, null, null, 0L
        );

        Optional<RiskRuleResult> result = rule.evaluate(features);

        assertTrue(result.isEmpty());
    }

    @Test
    void evaluateShouldNotTriggerWhenTemperatureIsNull() {
        RiskFeatures features = new RiskFeatures(
                java.util.UUID.randomUUID(),
                AssetType.TRANSFORMER,
                AssetStatus.ACTIVE,
                AssetCriticality.HIGH,
                10,
                null,
                BigDecimal.valueOf(70.0),
                1,
                0L, null, null, null, null, 0L
        );

        Optional<RiskRuleResult> result = rule.evaluate(features);

        assertTrue(result.isEmpty());
    }
}
