package com.powerassetintelligence.unit.ai.rule;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.powerassetintelligence.core.ai.RiskFactor;
import com.powerassetintelligence.core.ai.RiskFactorSeverity;
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
    void evaluateShouldTriggerSustainedHighTemperature() {
        RiskFeatures features = new RiskFeatures(
                java.util.UUID.randomUUID(),
                AssetType.TRANSFORMER,
                AssetStatus.ACTIVE,
                AssetCriticality.HIGH,
                10,
                BigDecimal.valueOf(86.0), // latest >= 85
                BigDecimal.valueOf(60.0),
                1,
                0L,
                BigDecimal.valueOf(76.0), // average >= 75
                BigDecimal.valueOf(91.0), // max >= 90 (severe peak)
                BigDecimal.valueOf(70.0),
                BigDecimal.valueOf(80.0),
                0L, null, null
        );

        Optional<RiskRuleResult> result = rule.evaluate(features);

        assertTrue(result.isPresent());
        assertEquals("SUSTAINED_HIGH_TEMPERATURE", result.get().ruleCode());
        RiskFactor factor = result.get().riskFactor();
        assertEquals(BigDecimal.valueOf(15), factor.contribution());
        assertEquals(RiskFactorSeverity.HIGH, factor.severity());
    }

    @Test
    void evaluateShouldTriggerSustainedHighWithoutSeverePeak() {
        RiskFeatures features = new RiskFeatures(
                java.util.UUID.randomUUID(),
                AssetType.TRANSFORMER,
                AssetStatus.ACTIVE,
                AssetCriticality.HIGH,
                10,
                BigDecimal.valueOf(86.0), // latest >= 85
                BigDecimal.valueOf(60.0),
                1,
                0L,
                BigDecimal.valueOf(76.0), // average >= 75
                BigDecimal.valueOf(88.0), // max < 90 (no severe peak)
                BigDecimal.valueOf(70.0),
                BigDecimal.valueOf(80.0),
                0L, null, null
        );

        Optional<RiskRuleResult> result = rule.evaluate(features);

        assertTrue(result.isPresent());
        assertEquals("SUSTAINED_HIGH_TEMPERATURE", result.get().ruleCode());
        RiskFactor factor = result.get().riskFactor();
        assertEquals(BigDecimal.valueOf(12), factor.contribution());
        assertEquals(RiskFactorSeverity.MEDIUM, factor.severity());
    }

    @Test
    void evaluateShouldNotTriggerWhenAverageBelowThreshold() {
        RiskFeatures features = new RiskFeatures(
                java.util.UUID.randomUUID(),
                AssetType.TRANSFORMER,
                AssetStatus.ACTIVE,
                AssetCriticality.HIGH,
                10,
                BigDecimal.valueOf(86.0), // latest >= 85
                BigDecimal.valueOf(60.0),
                1,
                0L,
                BigDecimal.valueOf(74.0), // average < 75
                BigDecimal.valueOf(88.0),
                BigDecimal.valueOf(70.0),
                BigDecimal.valueOf(80.0),
                0L, null, null
        );

        Optional<RiskRuleResult> result = rule.evaluate(features);

        assertTrue(result.isEmpty());
    }

    @Test
    void evaluateShouldNotTriggerWhenLatestBelowThreshold() {
        RiskFeatures features = new RiskFeatures(
                java.util.UUID.randomUUID(),
                AssetType.TRANSFORMER,
                AssetStatus.ACTIVE,
                AssetCriticality.HIGH,
                10,
                BigDecimal.valueOf(84.0), // latest < 85
                BigDecimal.valueOf(60.0),
                1,
                0L,
                BigDecimal.valueOf(76.0),
                BigDecimal.valueOf(88.0),
                BigDecimal.valueOf(70.0),
                BigDecimal.valueOf(80.0),
                0L, null, null
        );

        Optional<RiskRuleResult> result = rule.evaluate(features);

        assertTrue(result.isEmpty());
    }
}
