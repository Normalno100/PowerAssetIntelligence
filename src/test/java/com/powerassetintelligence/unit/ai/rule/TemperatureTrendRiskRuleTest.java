package com.powerassetintelligence.unit.ai.rule;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.powerassetintelligence.core.ai.RiskFactor;
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

    private static final int DEFAULT_ASSET_AGE = 10;

    private final TemperatureTrendRiskRule rule = new TemperatureTrendRiskRule();

    private RiskFeatures features(
            BigDecimal latestTemp,
            BigDecimal temperatureTrend,
            BigDecimal latestLoad,
            BigDecimal avgTemp,
            BigDecimal maxTemp,
            Integer overheatingCount
    ) {
        return new RiskFeatures(
                java.util.UUID.randomUUID(),
                AssetType.TRANSFORMER,
                AssetStatus.ACTIVE,
                AssetCriticality.HIGH,
                DEFAULT_ASSET_AGE,
                latestTemp,
                latestLoad,
                overheatingCount,
                0L,
                avgTemp,
                maxTemp,
                null, null, 0L,
                temperatureTrend,
                null
        );
    }

    // ===== Severity level: Critical =====
    @Test
    void evaluateShouldTriggerCriticalTemperatureTrend() {
        // trend >= 3.0 AND latest >= 70
        RiskFeatures features = features(
                BigDecimal.valueOf(75),   // latest = 75
                BigDecimal.valueOf(3.5),  // trend = 3.5
                null, null, null, null);

        Optional<RiskRuleResult> result = rule.evaluate(features);

        assertTrue(result.isPresent());
        assertEquals("CRITICAL_TEMPERATURE_TREND", result.get().ruleCode());
        assertEquals(BigDecimal.valueOf(30), result.get().scoreContribution());
        assertNotNull(result.get().structuredRiskFactor());
        assertEquals("CRITICAL_TEMPERATURE_TREND", result.get().structuredRiskFactor().code());
        assertEquals("TEMPERATURE", result.get().structuredRiskFactor().category());
    }

    // ===== Severity level: High =====
    @Test
    void evaluateShouldTriggerHighTemperatureTrend() {
        // trend >= 2.0 AND latest >= 75
        RiskFeatures features = features(
                BigDecimal.valueOf(80),   // latest = 80
                BigDecimal.valueOf(2.5),  // trend = 2.5
                null, null, null, null);

        Optional<RiskRuleResult> result = rule.evaluate(features);

        assertTrue(result.isPresent());
        assertEquals("HIGH_TEMPERATURE_TREND", result.get().ruleCode());
        assertEquals(BigDecimal.valueOf(22), result.get().scoreContribution());
    }

    // ===== Severity level: Elevated =====
    @Test
    void evaluateShouldTriggerElevatedTemperatureTrend() {
        // trend >= 1.0 AND latest >= 80
        RiskFeatures features = features(
                BigDecimal.valueOf(82),   // latest = 82
                BigDecimal.valueOf(1.2),  // trend = 1.2
                null, null, null, null);

        Optional<RiskRuleResult> result = rule.evaluate(features);

        assertTrue(result.isPresent());
        assertEquals("ELEVATED_TEMPERATURE_TREND", result.get().ruleCode());
        assertEquals(BigDecimal.valueOf(15), result.get().scoreContribution());
    }

    // ===== No trigger: trend too low (below noise threshold) =====
    @Test
    void evaluateShouldNotTriggerWhenTrendBelowNoiseThreshold() {
        // trend = 0.2 < 0.3 (noise threshold)
        RiskFeatures features = features(
                BigDecimal.valueOf(85),
                BigDecimal.valueOf(0.2),
                null, null, null, null);

        Optional<RiskRuleResult> result = rule.evaluate(features);

        assertTrue(result.isEmpty());
    }

    // ===== No trigger: zero trend =====
    @Test
    void evaluateShouldNotTriggerWhenTrendIsZero() {
        RiskFeatures features = features(
                BigDecimal.valueOf(85),
                BigDecimal.ZERO,
                null, null, null, null);

        Optional<RiskRuleResult> result = rule.evaluate(features);

        assertTrue(result.isEmpty());
    }

    // ===== No trigger: negative trend =====
    @Test
    void evaluateShouldNotTriggerWhenTrendIsNegative() {
        RiskFeatures features = features(
                BigDecimal.valueOf(85),
                BigDecimal.valueOf(-1.0),
                null, null, null, null);

        Optional<RiskRuleResult> result = rule.evaluate(features);

        assertTrue(result.isEmpty());
    }

    // ===== No trigger: trend positive but latest temp too low for CRITICAL level =====
    @Test
    void evaluateShouldNotTriggerCriticalWhenLatestTempBelowThreshold() {
        // trend = 3.5 but latest = 65 < 70 → no CRITICAL
        // trend = 3.5 but latest = 65 < 75 → no HIGH
        // trend = 3.5 but latest = 65 < 80 → no ELEVATED
        RiskFeatures features = features(
                BigDecimal.valueOf(65),  // below all thresholds
                BigDecimal.valueOf(3.5),
                null, null, null, null);

        Optional<RiskRuleResult> result = rule.evaluate(features);

        assertTrue(result.isEmpty());
    }

    // ===== No trigger: null trend =====
    @Test
    void evaluateShouldNotTriggerWhenTrendIsNull() {
        RiskFeatures features = features(
                BigDecimal.valueOf(90),
                null,  // no trend data
                null, null, null, null);

        Optional<RiskRuleResult> result = rule.evaluate(features);

        assertTrue(result.isEmpty());
    }

    // ===== No trigger: null latest temp =====
    @Test
    void evaluateShouldNotTriggerWhenLatestTempIsNull() {
        RiskFeatures features = features(
                null,  // no latest temp
                BigDecimal.valueOf(2.0),
                null, null, null, null);

        Optional<RiskRuleResult> result = rule.evaluate(features);

        assertTrue(result.isEmpty());
    }

    // ===== Severity level boundaries =====
    @Test
    void evaluateShouldChooseHighestMatchingSeverityLevel() {
        // trend = 4.0, latest = 85 → matches all three levels, should pick CRITICAL (highest)
        RiskFeatures features = features(
                BigDecimal.valueOf(85),
                BigDecimal.valueOf(4.0),
                null, null, null, null);

        Optional<RiskRuleResult> result = rule.evaluate(features);

        assertTrue(result.isPresent());
        assertEquals("CRITICAL_TEMPERATURE_TREND", result.get().ruleCode());
        assertEquals(BigDecimal.valueOf(30), result.get().scoreContribution());
    }

    @Test
    void evaluateShouldMatchHighLevelWhenCriticalThresholdNotMet() {
        // trend = 2.5, latest = 76 → trend >= 2.0, latest >= 75 → HIGH
        // but latest < 70 for critical? No, latest=76 >= 70, so critical also matches...
        // Actually: critical requires trend >= 3.0, so 2.5 doesn't match critical
        RiskFeatures features = features(
                BigDecimal.valueOf(76),
                BigDecimal.valueOf(2.5),
                null, null, null, null);

        Optional<RiskRuleResult> result = rule.evaluate(features);

        assertTrue(result.isPresent());
        assertEquals("HIGH_TEMPERATURE_TREND", result.get().ruleCode());
        assertEquals(BigDecimal.valueOf(22), result.get().scoreContribution());
    }

    @Test
    void evaluateShouldMatchElevatedLevelWhenHighThresholdNotMet() {
        // trend = 1.5, latest = 82 → trend >= 1.0, latest >= 80 → ELEVATED
        // but trend < 2.0, so no HIGH
        // latest < 75? No, 82 >= 75, but trend < 3.0 and < 2.0, so no CRITICAL/HIGH
        RiskFeatures features = features(
                BigDecimal.valueOf(82),
                BigDecimal.valueOf(1.5),
                null, null, null, null);

        Optional<RiskRuleResult> result = rule.evaluate(features);

        assertTrue(result.isPresent());
        assertEquals("ELEVATED_TEMPERATURE_TREND", result.get().ruleCode());
        assertEquals(BigDecimal.valueOf(15), result.get().scoreContribution());
    }

    // ===== RiskFactor structured object validation =====
    @Test
    void evaluateShouldContainStructuredRiskFactor() {
        RiskFeatures features = features(
                BigDecimal.valueOf(82),
                BigDecimal.valueOf(1.5),
                null, null, null, null);

        Optional<RiskRuleResult> result = rule.evaluate(features);

        assertTrue(result.isPresent());
        RiskFactor structuredFactor = result.get().structuredRiskFactor();
        assertNotNull(structuredFactor);
        assertEquals("ELEVATED_TEMPERATURE_TREND", structuredFactor.code());
        assertEquals("TEMPERATURE", structuredFactor.category());
        assertEquals("Temperature is rising rapidly", structuredFactor.description());
        assertTrue(structuredFactor.value().compareTo(BigDecimal.ZERO) > 0);
        assertEquals("CELSIUS_PER_HOUR", structuredFactor.unit());
    }

    @Test
    void evaluateShouldHaveLegacyAndStructuredRiskFactor() {
        RiskFeatures features = features(
                BigDecimal.valueOf(82),
                BigDecimal.valueOf(1.5),
                null, null, null, null);

        Optional<RiskRuleResult> result = rule.evaluate(features);

        assertTrue(result.isPresent());
        // Legacy string
        assertTrue(result.get().riskFactor().contains("Temperature trend"));
        assertTrue(result.get().riskFactor().contains("1.5"));
        // Structured
        assertNotNull(result.get().structuredRiskFactor());
    }

    // ===== No trigger: critical level not met (latest temp below 70) =====
    @Test
    void evaluateShouldNotTriggerWhenLatestTempBelowAllLevels() {
        // trend = 3.0 (meets critical trend), but latest = 68 < 70 (critical fails)
        // Also latest < 75 (high fails), latest < 80 (elevated fails)
        RiskFeatures features = features(
                BigDecimal.valueOf(68),
                BigDecimal.valueOf(3.0),
                null, null, null, null);

        Optional<RiskRuleResult> result = rule.evaluate(features);

        assertTrue(result.isEmpty());
    }

    // ===== Recommendations present =====
    @Test
    void evaluateShouldIncludeRecommendations() {
        RiskFeatures features = features(
                BigDecimal.valueOf(85),
                BigDecimal.valueOf(2.0),
                null, null, null, null);

        Optional<RiskRuleResult> result = rule.evaluate(features);

        assertTrue(result.isPresent());
        assertTrue(result.get().recommendations().size() >= 2);
    }

    // ===== Edge: exact threshold values =====
    @Test
    void evaluateShouldTriggerWhenValuesExactlyAtThreshold() {
        // trend = exactly 1.0, latest = exactly 80 → should match ELEVATED
        RiskFeatures features = features(
                BigDecimal.valueOf(80),
                BigDecimal.valueOf(1.0),
                null, null, null, null);

        Optional<RiskRuleResult> result = rule.evaluate(features);

        assertTrue(result.isPresent());
        assertEquals("ELEVATED_TEMPERATURE_TREND", result.get().ruleCode());
    }

    // ===== Edge: trend just below threshold =====
    @Test
    void evaluateShouldNotTriggerWhenTrendJustBelowThreshold() {
        // trend = 0.99 < 1.0 → should NOT trigger even with high temp
        RiskFeatures features = features(
                BigDecimal.valueOf(90),
                new BigDecimal("0.99"),
                null, null, null, null);

        Optional<RiskRuleResult> result = rule.evaluate(features);

        assertTrue(result.isEmpty());
    }
}
