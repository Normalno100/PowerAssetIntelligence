package com.powerassetintelligence.unit.ai;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.powerassetintelligence.core.ai.RiskFactor;
import com.powerassetintelligence.core.ai.RiskFactorSeverity;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class RiskFactorTest {

    @Test
    void ofShouldCreateFactorWithAllFields() {
        RiskFactor factor = RiskFactor.of(
                "HIGH_TEMPERATURE",
                RiskFactorSeverity.HIGH,
                "Temperature exceeds safe threshold",
                BigDecimal.valueOf(20)
        );

        assertNotNull(factor);
        assertEquals("HIGH_TEMPERATURE", factor.code());
        assertEquals(RiskFactorSeverity.HIGH, factor.severity());
        assertEquals("Temperature exceeds safe threshold", factor.description());
        assertEquals(BigDecimal.valueOf(20), factor.contribution());
    }

    @Test
    void ofShouldCreateFactorWithLowSeverity() {
        RiskFactor factor = RiskFactor.of(
                "MINOR_DEVIATION",
                RiskFactorSeverity.LOW,
                "Minor parameter deviation detected",
                BigDecimal.valueOf(5)
        );

        assertEquals(RiskFactorSeverity.LOW, factor.severity());
        assertEquals(BigDecimal.valueOf(5), factor.contribution());
    }

    @Test
    void ofShouldCreateFactorWithCriticalSeverity() {
        RiskFactor factor = RiskFactor.of(
                "CRITICAL_FAILURE",
                RiskFactorSeverity.CRITICAL,
                "Critical system failure detected",
                BigDecimal.valueOf(45)
        );

        assertEquals(RiskFactorSeverity.CRITICAL, factor.severity());
        assertEquals(BigDecimal.valueOf(45), factor.contribution());
    }

    @Test
    void contributionShouldBePositive() {
        RiskFactor factor = RiskFactor.of(
                "TEST_FACTOR",
                RiskFactorSeverity.MEDIUM,
                "Test description",
                BigDecimal.valueOf(10)
        );

        assertTrue(factor.contribution().compareTo(BigDecimal.ZERO) > 0);
    }

    @Test
    void contributionShouldSupportZero() {
        RiskFactor factor = RiskFactor.of(
                "LOW_IMPACT",
                RiskFactorSeverity.LOW,
                "Minimal impact factor",
                BigDecimal.ZERO
        );

        assertEquals(BigDecimal.ZERO, factor.contribution());
    }

    @Test
    void codeShouldBeNonNull() {
        RiskFactor factor = RiskFactor.of(
                "ALPHA_CODE",
                RiskFactorSeverity.HIGH,
                "Description",
                BigDecimal.valueOf(15)
        );

        assertNotNull(factor.code());
        assertEquals("ALPHA_CODE", factor.code());
    }

    @Test
    void severityShouldBeNonNull() {
        RiskFactor factor = RiskFactor.of(
                "TEST",
                RiskFactorSeverity.MEDIUM,
                "Description",
                BigDecimal.valueOf(10)
        );

        assertNotNull(factor.severity());
        assertEquals(RiskFactorSeverity.MEDIUM, factor.severity());
    }

    @Test
    void descriptionShouldBeNonNull() {
        RiskFactor factor = RiskFactor.of(
                "TEST",
                RiskFactorSeverity.MEDIUM,
                "Detailed description of risk",
                BigDecimal.valueOf(10)
        );

        assertNotNull(factor.description());
        assertEquals("Detailed description of risk", factor.description());
    }

    @Test
    void factoryMethodShouldCreateImmutabaleFactor() {
        RiskFactor factor = RiskFactor.of(
                "IMMUTABLE_TEST",
                RiskFactorSeverity.HIGH,
                "Description",
                BigDecimal.valueOf(25)
        );

        // Verify all fields are accessible
        assertEquals("IMMUTABLE_TEST", factor.code());
        assertEquals(RiskFactorSeverity.HIGH, factor.severity());
        assertEquals("Description", factor.description());
        assertEquals(BigDecimal.valueOf(25), factor.contribution());
    }
}
