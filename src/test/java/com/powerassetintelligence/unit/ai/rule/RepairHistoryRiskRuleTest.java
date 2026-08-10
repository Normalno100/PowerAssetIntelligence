package com.powerassetintelligence.unit.ai.rule;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.powerassetintelligence.core.ai.RiskFeatures;
import com.powerassetintelligence.core.ai.RiskRuleResult;
import com.powerassetintelligence.core.ai.rule.RepairHistoryRiskRule;
import com.powerassetintelligence.domain.model.AssetCriticality;
import com.powerassetintelligence.domain.model.AssetStatus;
import com.powerassetintelligence.domain.model.AssetType;
import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class RepairHistoryRiskRuleTest {

    private final RepairHistoryRiskRule rule = new RepairHistoryRiskRule();

    @Test
    void evaluateShouldTriggerFrequentRepairs() {
        RiskFeatures features = new RiskFeatures(
                java.util.UUID.randomUUID(),
                AssetType.CIRCUIT_BREAKER,
                AssetStatus.ACTIVE,
                AssetCriticality.HIGH,
                10,
                BigDecimal.valueOf(70.0),
                BigDecimal.valueOf(60.0),
                0,
                5L, null, null, null, null, 0L, null, null // > 3 repairs
        );

        Optional<RiskRuleResult> result = rule.evaluate(features);

        assertTrue(result.isPresent());
        assertEquals("FREQUENT_REPAIRS", result.get().ruleCode());
        assertEquals(BigDecimal.valueOf(20), result.get().scoreContribution());
    }

    @Test
    void evaluateShouldTriggerRepeatedRepairs() {
        RiskFeatures features = new RiskFeatures(
                java.util.UUID.randomUUID(),
                AssetType.CIRCUIT_BREAKER,
                AssetStatus.ACTIVE,
                AssetCriticality.HIGH,
                10,
                BigDecimal.valueOf(70.0),
                BigDecimal.valueOf(60.0),
                0,
                2L, null, null, null, null, 0L, null, null // >= 2 repairs
        );

        Optional<RiskRuleResult> result = rule.evaluate(features);

        assertTrue(result.isPresent());
        assertEquals("REPEATED_REPAIRS", result.get().ruleCode());
        assertEquals(BigDecimal.valueOf(10), result.get().scoreContribution());
    }

    @Test
    void evaluateShouldNotTriggerWhenRepairsBelowThreshold() {
        RiskFeatures features = new RiskFeatures(
                java.util.UUID.randomUUID(),
                AssetType.CIRCUIT_BREAKER,
                AssetStatus.ACTIVE,
                AssetCriticality.HIGH,
                10,
                BigDecimal.valueOf(70.0),
                BigDecimal.valueOf(60.0),
                0,
                1L, null, null, null, null, 0L, null, null // <= 1 repair
        );

        Optional<RiskRuleResult> result = rule.evaluate(features);

        assertTrue(result.isEmpty());
    }
}
