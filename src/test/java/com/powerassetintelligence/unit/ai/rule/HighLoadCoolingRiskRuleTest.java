package com.powerassetintelligence.unit.ai.rule;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.powerassetintelligence.core.ai.RiskFactor;
import com.powerassetintelligence.core.ai.RiskFactorSeverity;
import com.powerassetintelligence.core.ai.RiskFeatures;
import com.powerassetintelligence.core.ai.RiskRuleResult;
import com.powerassetintelligence.core.ai.rule.HighLoadCoolingRiskRule;
import com.powerassetintelligence.domain.model.AssetCriticality;
import com.powerassetintelligence.domain.model.AssetStatus;
import com.powerassetintelligence.domain.model.AssetType;
import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class HighLoadCoolingRiskRuleTest {

    private final HighLoadCoolingRiskRule rule = new HighLoadCoolingRiskRule();

    @Test
    void evaluateShouldTriggerHighLoadWithFrequentOverheating() {
        RiskFeatures features = new RiskFeatures(
                java.util.UUID.randomUUID(),
                AssetType.CIRCUIT_BREAKER,
                AssetStatus.ACTIVE,
                AssetCriticality.HIGH,
                10,
                BigDecimal.valueOf(70.0),
                BigDecimal.valueOf(95.0), // > 90%
                3, // >= 3 overheating
                0L, null, null, null, null, 0L, null, null
        );

        Optional<RiskRuleResult> result = rule.evaluate(features);

        assertTrue(result.isPresent());
        assertEquals("HIGH_LOAD_FREQUENT_OVERHEATING", result.get().ruleCode());
        RiskFactor factor = result.get().riskFactor();
        assertEquals(BigDecimal.valueOf(30), factor.contribution());
        assertEquals(RiskFactorSeverity.HIGH, factor.severity());
    }

    @Test
    void evaluateShouldTriggerHighLoadWithoutFrequentOverheating() {
        RiskFeatures features = new RiskFeatures(
                java.util.UUID.randomUUID(),
                AssetType.CIRCUIT_BREAKER,
                AssetStatus.ACTIVE,
                AssetCriticality.HIGH,
                10,
                BigDecimal.valueOf(70.0),
                BigDecimal.valueOf(92.0), // > 90%
                1, // < 3 overheating
                0L, null, null, null, null, 0L, null, null
        );

        Optional<RiskRuleResult> result = rule.evaluate(features);

        assertTrue(result.isPresent());
        assertEquals("HIGH_LOAD", result.get().ruleCode());
        RiskFactor factor = result.get().riskFactor();
        assertEquals(BigDecimal.valueOf(15), factor.contribution());
        assertEquals(RiskFactorSeverity.MEDIUM, factor.severity());
    }

    @Test
    void evaluateShouldNotTriggerWhenLoadBelowThreshold() {
        RiskFeatures features = new RiskFeatures(
                java.util.UUID.randomUUID(),
                AssetType.CIRCUIT_BREAKER,
                AssetStatus.ACTIVE,
                AssetCriticality.HIGH,
                10,
                BigDecimal.valueOf(70.0),
                BigDecimal.valueOf(85.0), // <= 90%
                3,
                0L, null, null, null, null, 0L, null, null
        );

        Optional<RiskRuleResult> result = rule.evaluate(features);

        assertTrue(result.isEmpty());
    }

    @Test
    void evaluateShouldNotTriggerWhenLoadIsNull() {
        RiskFeatures features = new RiskFeatures(
                java.util.UUID.randomUUID(),
                AssetType.CIRCUIT_BREAKER,
                AssetStatus.ACTIVE,
                AssetCriticality.HIGH,
                10,
                BigDecimal.valueOf(70.0),
                null,
                3,
                0L, null, null, null, null, 0L, null, null
        );

        Optional<RiskRuleResult> result = rule.evaluate(features);

        assertTrue(result.isEmpty());
    }
}
