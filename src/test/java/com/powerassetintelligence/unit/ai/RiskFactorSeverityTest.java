package com.powerassetintelligence.unit.ai;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.powerassetintelligence.core.ai.RiskFactorSeverity;
import org.junit.jupiter.api.Test;

class RiskFactorSeverityTest {

    @Test
    void enumShouldHaveAllSeverityLevels() {
        RiskFactorSeverity[] values = RiskFactorSeverity.values();
        assertEquals(4, values.length);
    }

    @Test
    void valuesShouldIncludeAllLevels() {
        RiskFactorSeverity[] values = RiskFactorSeverity.values();

        RiskFactorSeverity[] expected = {
                RiskFactorSeverity.LOW,
                RiskFactorSeverity.MEDIUM,
                RiskFactorSeverity.HIGH,
                RiskFactorSeverity.CRITICAL
        };

        assertEquals(expected.length, values.length);
        for (int i = 0; i < expected.length; i++) {
            assertEquals(expected[i], values[i]);
        }
    }

    @Test
    void valueOfShouldReturnCorrectSeverity() {
        assertEquals(RiskFactorSeverity.LOW, RiskFactorSeverity.valueOf("LOW"));
        assertEquals(RiskFactorSeverity.MEDIUM, RiskFactorSeverity.valueOf("MEDIUM"));
        assertEquals(RiskFactorSeverity.HIGH, RiskFactorSeverity.valueOf("HIGH"));
        assertEquals(RiskFactorSeverity.CRITICAL, RiskFactorSeverity.valueOf("CRITICAL"));
    }

    @Test
    void toStringShouldReturnUpperCaseName() {
        assertEquals("LOW", RiskFactorSeverity.LOW.name());
        assertEquals("MEDIUM", RiskFactorSeverity.MEDIUM.name());
        assertEquals("HIGH", RiskFactorSeverity.HIGH.name());
        assertEquals("CRITICAL", RiskFactorSeverity.CRITICAL.name());
    }

    @Test
    void nullValueShouldThrowException() {
        try {
            RiskFactorSeverity.valueOf(null);
            org.junit.jupiter.api.Assertions.fail("Expected NullPointerException");
        } catch (NullPointerException e) {
            // Expected
        }
    }

    @Test
    void unknownValueShouldThrowException() {
        try {
            RiskFactorSeverity.valueOf("UNKNOWN");
            org.junit.jupiter.api.Assertions.fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            // Expected
        }
    }

    @Test
    void eachSeverityShouldBeDistinct() {
        assertNotNull(RiskFactorSeverity.LOW);
        assertNotNull(RiskFactorSeverity.MEDIUM);
        assertNotNull(RiskFactorSeverity.HIGH);
        assertNotNull(RiskFactorSeverity.CRITICAL);

        assertEquals(4,
                java.util.Set.of(RiskFactorSeverity.LOW, RiskFactorSeverity.MEDIUM,
                        RiskFactorSeverity.HIGH, RiskFactorSeverity.CRITICAL).size());
    }
}
