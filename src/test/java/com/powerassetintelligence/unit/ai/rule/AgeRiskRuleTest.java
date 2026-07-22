package com.powerassetintelligence.unit.ai.rule;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.powerassetintelligence.application.dto.RiskFeatures;
import com.powerassetintelligence.core.ai.RiskRuleResult;
import com.powerassetintelligence.core.ai.rule.AgeRiskRule;
import com.powerassetintelligence.domain.model.AssetCriticality;
import com.powerassetintelligence.domain.model.AssetStatus;
import com.powerassetintelligence.domain.model.AssetType;
import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class AgeRiskRuleTest {

    private final AgeRiskRule rule = new AgeRiskRule();

    @Test
    void evaluateShouldTriggerVeryOldAsset() {
        RiskFeatures features = new RiskFeatures(
                java.util.UUID.randomUUID(),
                AssetType.TRANSFORMER,
                AssetStatus.ACTIVE,
                AssetCriticality.HIGH,
                25, // >= 25
                BigDecimal.valueOf(70.0),
                BigDecimal.valueOf(60.0),
                0,
                0L
        );

        Optional<RiskRuleResult> result = rule.evaluate(features);

        assertTrue(result.isPresent());
        assertEquals("VERY_OLD_ASSET", result.get().ruleCode());
        assertEquals(BigDecimal.valueOf(20), result.get().scoreContribution());
    }

    @Test
    void evaluateShouldTriggerAgingAsset() {
        RiskFeatures features = new RiskFeatures(
                java.util.UUID.randomUUID(),
                AssetType.TRANSFORMER,
                AssetStatus.ACTIVE,
                AssetCriticality.HIGH,
                16, // > 15
                BigDecimal.valueOf(70.0),
                BigDecimal.valueOf(60.0),
                0,
                0L
        );

        Optional<RiskRuleResult> result = rule.evaluate(features);

        assertTrue(result.isPresent());
        assertEquals("AGING_ASSET", result.get().ruleCode());
        assertEquals(BigDecimal.valueOf(10), result.get().scoreContribution());
    }

    @Test
    void evaluateShouldNotTriggerWhenAgeBelowThreshold() {
        RiskFeatures features = new RiskFeatures(
                java.util.UUID.randomUUID(),
                AssetType.TRANSFORMER,
                AssetStatus.ACTIVE,
                AssetCriticality.HIGH,
                15, // <= 15
                BigDecimal.valueOf(70.0),
                BigDecimal.valueOf(60.0),
                0,
                0L
        );

        Optional<RiskRuleResult> result = rule.evaluate(features);

        assertTrue(result.isEmpty());
    }
}
