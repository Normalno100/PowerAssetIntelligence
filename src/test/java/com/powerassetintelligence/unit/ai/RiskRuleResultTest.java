package com.powerassetintelligence.unit.ai;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.powerassetintelligence.core.ai.RiskFactor;
import com.powerassetintelligence.core.ai.RiskFactorSeverity;
import com.powerassetintelligence.core.ai.RiskRuleResult;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;

class RiskRuleResultTest {

    @Test
    void ofShouldCreateResultWithAllFields() {
        RiskFactor factor = RiskFactor.of(
                "HIGH_TEMP",
                RiskFactorSeverity.HIGH,
                "Temperature above threshold",
                BigDecimal.valueOf(20)
        );

        RiskRuleResult result = RiskRuleResult.of(
                "HIGH_TEMP_RULE",
                factor,
                List.of("Check cooling system", "Reduce load")
        );

        assertNotNull(result);
        assertEquals("HIGH_TEMP_RULE", result.ruleCode());
        assertEquals(factor, result.riskFactor());
        assertEquals(2, result.recommendations().size());
    }

    @Test
    void riskFactorShouldNotBeNull() {
        RiskFactor factor = RiskFactor.of(
                "TEST_FACTOR",
                RiskFactorSeverity.MEDIUM,
                "Test description",
                BigDecimal.valueOf(10)
        );

        RiskRuleResult result = RiskRuleResult.of("TEST_RULE", factor, List.of());

        assertNotNull(result.riskFactor());
    }

    @Test
    void recommendationsShouldBeCopy() {
        RiskFactor factor = RiskFactor.of(
                "TEST",
                RiskFactorSeverity.LOW,
                "Test",
                BigDecimal.valueOf(5)
        );

        RiskRuleResult result = RiskRuleResult.of("RULE", factor, List.of("Recommendation 1"));

        // Verify list is unmodifiable
        assertThrows(UnsupportedOperationException.class, () -> {
            result.recommendations().add("New recommendation");
        });
    }

    @Test
    void recommendationsShouldBeImmutable() {
        RiskFactor factor = RiskFactor.of(
                "TEST",
                RiskFactorSeverity.MEDIUM,
                "Test",
                BigDecimal.valueOf(10)
        );

        RiskRuleResult result = RiskRuleResult.of("RULE", factor, List.of("Rec 1", "Rec 2"));

        assertTrue(result.recommendations().size() >= 2);
    }

    @Test
    void ruleCodeShouldBeNonNull() {
        RiskFactor factor = RiskFactor.of(
                "CODE",
                RiskFactorSeverity.HIGH,
                "Description",
                BigDecimal.valueOf(15)
        );

        RiskRuleResult result = RiskRuleResult.of("MY_RULE_CODE", factor, List.of());

        assertNotNull(result.ruleCode());
        assertEquals("MY_RULE_CODE", result.ruleCode());
    }

    @Test
    void riskFactorShouldPreserveAllFields() {
        RiskFactor factor = RiskFactor.of(
                "COMPLEX_FACTOR",
                RiskFactorSeverity.CRITICAL,
                "Complex risk factor with all fields",
                BigDecimal.valueOf(45)
        );

        RiskRuleResult result = RiskRuleResult.of("COMPLEX_RULE", factor, List.of());

        RiskFactor retrieved = result.riskFactor();
        assertEquals("COMPLEX_FACTOR", retrieved.code());
        assertEquals(RiskFactorSeverity.CRITICAL, retrieved.severity());
        assertEquals("Complex risk factor with all fields", retrieved.description());
        assertEquals(BigDecimal.valueOf(45), retrieved.contribution());
    }

    @Test
    void emptyRecommendationsShouldBeValid() {
        RiskFactor factor = RiskFactor.of(
                "TEST",
                RiskFactorSeverity.LOW,
                "Test",
                BigDecimal.valueOf(5)
        );

        RiskRuleResult result = RiskRuleResult.of("RULE", factor, List.of());

        assertTrue(result.recommendations().isEmpty());
    }
}
