package com.powerassetintelligence.unit.ai.rule;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.powerassetintelligence.core.ai.RiskFeatures;
import com.powerassetintelligence.core.ai.RiskRuleResult;
import com.powerassetintelligence.core.ai.rule.AgingOverheatRepairRiskRule;
import com.powerassetintelligence.domain.model.AssetCriticality;
import com.powerassetintelligence.domain.model.AssetStatus;
import com.powerassetintelligence.domain.model.AssetType;
import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class AgingOverheatRepairRiskRuleTest {

    private final AgingOverheatRepairRiskRule rule = new AgingOverheatRepairRiskRule();

    @Test
    void evaluateShouldTriggerWhenAllConditionsMet() {
        RiskFeatures features = new RiskFeatures(
                java.util.UUID.randomUUID(),
                AssetType.TRANSFORMER,
                AssetStatus.ACTIVE,
                AssetCriticality.HIGH,
                20, // > 15 years
                BigDecimal.valueOf(85.0), // > 80°C
                BigDecimal.valueOf(70.0),
                1,
                5L // > 3 repairs
        );

        Optional<RiskRuleResult> result = rule.evaluate(features);

        assertTrue(result.isPresent());
        assertEquals("AGING_OVERHEAT_REPAIR_HISTORY", result.get().ruleCode());
        assertEquals(BigDecimal.valueOf(45), result.get().scoreContribution());
        assertTrue(result.get().riskFactor().contains("older than 15 years"));
    }

    @Test
    void evaluateShouldNotTriggerWhenAgeBelowThreshold() {
        RiskFeatures features = new RiskFeatures(
                java.util.UUID.randomUUID(),
                AssetType.TRANSFORMER,
                AssetStatus.ACTIVE,
                AssetCriticality.HIGH,
                10, // <= 15 years
                BigDecimal.valueOf(85.0),
                BigDecimal.valueOf(70.0),
                1,
                5L
        );

        Optional<RiskRuleResult> result = rule.evaluate(features);

        assertTrue(result.isEmpty());
    }

    @Test
    void evaluateShouldNotTriggerWhenTemperatureBelowThreshold() {
        RiskFeatures features = new RiskFeatures(
                java.util.UUID.randomUUID(),
                AssetType.TRANSFORMER,
                AssetStatus.ACTIVE,
                AssetCriticality.HIGH,
                20,
                BigDecimal.valueOf(75.0), // <= 80°C
                BigDecimal.valueOf(70.0),
                1,
                5L
        );

        Optional<RiskRuleResult> result = rule.evaluate(features);

        assertTrue(result.isEmpty());
    }

    @Test
    void evaluateShouldNotTriggerWhenRepairsBelowThreshold() {
        RiskFeatures features = new RiskFeatures(
                java.util.UUID.randomUUID(),
                AssetType.TRANSFORMER,
                AssetStatus.ACTIVE,
                AssetCriticality.HIGH,
                20,
                BigDecimal.valueOf(85.0),
                BigDecimal.valueOf(70.0),
                1,
                2L // <= 3 repairs
        );

        Optional<RiskRuleResult> result = rule.evaluate(features);

        assertTrue(result.isEmpty());
    }
}
