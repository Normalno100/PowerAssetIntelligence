package com.powerassetintelligence.unit.core.ai;

import com.powerassetintelligence.core.ai.RiskTrendCalculator;
import com.powerassetintelligence.core.ai.TrendDirection;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class RiskTrendCalculatorTest {

    private RiskTrendCalculator.TrendResult result;

    // Helper for BigDecimal comparison ignoring scale
    private static void assertBigDecimalEquals(BigDecimal expected, BigDecimal actual) {
        assertEquals(0, expected.compareTo(actual),
                "expected: " + expected + " but was: " + actual);
    }

    // -----------------------------------------------------------------
    // Empty history
    // -----------------------------------------------------------------
    @Nested
    @DisplayName("Empty history")
    class EmptyHistory {

        @Test
        @DisplayName("returns nulls for all fields and STABLE direction")
        void emptyList() {
            result = RiskTrendCalculator.calculate(List.of());

            assertNull(result.currentScore());
            assertNull(result.previousScore());
            assertNull(result.totalChange());
            assertNull(result.averageChange());
            assertEquals(TrendDirection.STABLE, result.direction());
            assertTrue(result.scoreChanges().isEmpty());
        }

        @Test
        @DisplayName("handles null input same as empty")
        void nullInput() {
            result = RiskTrendCalculator.calculate(null);

            assertNull(result.currentScore());
            assertNull(result.previousScore());
            assertNull(result.totalChange());
            assertNull(result.averageChange());
            assertEquals(TrendDirection.STABLE, result.direction());
            assertTrue(result.scoreChanges().isEmpty());
        }
    }

    // -----------------------------------------------------------------
    // One assessment
    // -----------------------------------------------------------------
    @Nested
    @DisplayName("One assessment")
    class OneAssessment {

        @Test
        @DisplayName("returns currentScore, zero totalChange, null averageChange, STABLE direction")
        void singleScore() {
            result = RiskTrendCalculator.calculate(List.of(new BigDecimal("42")));

            assertEquals(new BigDecimal("42"), result.currentScore());
            assertNull(result.previousScore());
            assertBigDecimalEquals(BigDecimal.ZERO, result.totalChange());
            assertNull(result.averageChange());
            assertEquals(TrendDirection.STABLE, result.direction());
            assertEquals(1, result.scoreChanges().size());
            assertNull(result.scoreChanges().get(0).delta());
            assertEquals(TrendDirection.STABLE, result.scoreChanges().get(0).trend());
        }
    }

    // -----------------------------------------------------------------
    // Increasing trend
    // -----------------------------------------------------------------
    @Nested
    @DisplayName("Increasing trend")
    class IncreasingTrend {

        @Test
        @DisplayName("42 → 51 → 63 → 78 → 84: RISING, totalChange=42, averageChange=10.50")
        void classicIncreasing() {
            result = RiskTrendCalculator.calculate(List.of(
                    new BigDecimal("42"),
                    new BigDecimal("51"),
                    new BigDecimal("63"),
                    new BigDecimal("78"),
                    new BigDecimal("84")
            ));

            assertEquals(new BigDecimal("84"), result.currentScore());
            assertEquals(new BigDecimal("78"), result.previousScore());
            assertBigDecimalEquals(new BigDecimal("42"), result.totalChange());
            assertBigDecimalEquals(new BigDecimal("10.50"), result.averageChange());
            assertEquals(TrendDirection.RISING, result.direction());
            assertEquals(5, result.scoreChanges().size());

            // Per-point changes
            assertNull(result.scoreChanges().get(0).delta());
            assertBigDecimalEquals(new BigDecimal("9"), result.scoreChanges().get(1).delta());
            assertBigDecimalEquals(new BigDecimal("12"), result.scoreChanges().get(2).delta());
            assertBigDecimalEquals(new BigDecimal("15"), result.scoreChanges().get(3).delta());
            assertBigDecimalEquals(new BigDecimal("6"), result.scoreChanges().get(4).delta());

            // All non-first points should be RISING
            for (int i = 1; i < result.scoreChanges().size(); i++) {
                assertEquals(TrendDirection.RISING, result.scoreChanges().get(i).trend());
            }
        }

        @Test
        @DisplayName("minimal increase 10 → 11")
        void minimalIncrease() {
            result = RiskTrendCalculator.calculate(List.of(
                    new BigDecimal("10"),
                    new BigDecimal("11")
            ));

            assertEquals(new BigDecimal("11"), result.currentScore());
            assertEquals(new BigDecimal("10"), result.previousScore());
            assertBigDecimalEquals(new BigDecimal("1"), result.totalChange());
            assertBigDecimalEquals(new BigDecimal("1.00"), result.averageChange());
            assertEquals(TrendDirection.RISING, result.direction());
        }
    }

    // -----------------------------------------------------------------
    // Decreasing trend
    // -----------------------------------------------------------------
    @Nested
    @DisplayName("Decreasing trend")
    class DecreasingTrend {

        @Test
        @DisplayName("84 → 70 → 55 → 55 → 40: FALLING with one flat step")
        void mixedDecreaseWithFlatStep() {
            result = RiskTrendCalculator.calculate(List.of(
                    new BigDecimal("84"),
                    new BigDecimal("70"),
                    new BigDecimal("55"),
                    new BigDecimal("55"),
                    new BigDecimal("40")
            ));

            assertEquals(new BigDecimal("40"), result.currentScore());
            assertEquals(new BigDecimal("55"), result.previousScore());
            assertBigDecimalEquals(new BigDecimal("-44"), result.totalChange());
            assertBigDecimalEquals(new BigDecimal("-11.00"), result.averageChange());
            assertEquals(TrendDirection.FALLING, result.direction());
            assertEquals(5, result.scoreChanges().size());

            // Per-point: -14, -15, 0, -15
            assertBigDecimalEquals(new BigDecimal("-14"), result.scoreChanges().get(1).delta());
            assertEquals(TrendDirection.FALLING, result.scoreChanges().get(1).trend());

            assertBigDecimalEquals(new BigDecimal("-15"), result.scoreChanges().get(2).delta());
            assertEquals(TrendDirection.FALLING, result.scoreChanges().get(2).trend());

            assertBigDecimalEquals(BigDecimal.ZERO, result.scoreChanges().get(3).delta());
            assertEquals(TrendDirection.STABLE, result.scoreChanges().get(3).trend());

            assertBigDecimalEquals(new BigDecimal("-15"), result.scoreChanges().get(4).delta());
            assertEquals(TrendDirection.FALLING, result.scoreChanges().get(4).trend());
        }

        @Test
        @DisplayName("91 → 84 → 78 → 63 → 51 → 42: pure decrease")
        void pureDecrease() {
            result = RiskTrendCalculator.calculate(List.of(
                    new BigDecimal("91"),
                    new BigDecimal("84"),
                    new BigDecimal("78"),
                    new BigDecimal("63"),
                    new BigDecimal("51"),
                    new BigDecimal("42")
            ));

            assertEquals(new BigDecimal("42"), result.currentScore());
            assertEquals(new BigDecimal("51"), result.previousScore());
            assertBigDecimalEquals(new BigDecimal("-49"), result.totalChange());
            assertBigDecimalEquals(new BigDecimal("-9.80"), result.averageChange());
            assertEquals(TrendDirection.FALLING, result.direction());

            for (int i = 1; i < result.scoreChanges().size(); i++) {
                assertEquals(TrendDirection.FALLING, result.scoreChanges().get(i).trend());
                assertNotNull(result.scoreChanges().get(i).delta());
                assertTrue(result.scoreChanges().get(i).delta().signum() < 0);
            }
        }
    }

    // -----------------------------------------------------------------
    // Stable trend
    // -----------------------------------------------------------------
    @Nested
    @DisplayName("Stable trend")
    class StableTrend {

        @Test
        @DisplayName("50 → 50 → 50: all changes are zero, direction STABLE")
        void constantScores() {
            result = RiskTrendCalculator.calculate(List.of(
                    new BigDecimal("50"),
                    new BigDecimal("50"),
                    new BigDecimal("50")
            ));

            assertEquals(new BigDecimal("50"), result.currentScore());
            assertEquals(new BigDecimal("50"), result.previousScore());
            assertBigDecimalEquals(BigDecimal.ZERO, result.totalChange());
            assertBigDecimalEquals(BigDecimal.ZERO, result.averageChange());
            assertEquals(TrendDirection.STABLE, result.direction());

            for (int i = 1; i < result.scoreChanges().size(); i++) {
                assertBigDecimalEquals(BigDecimal.ZERO, result.scoreChanges().get(i).delta());
                assertEquals(TrendDirection.STABLE, result.scoreChanges().get(i).trend());
            }
        }
    }

    // -----------------------------------------------------------------
    // Decimal scores
    // -----------------------------------------------------------------
    @Nested
    @DisplayName("Decimal scores")
    class DecimalScores {

        @Test
        @DisplayName("10.5 → 10.75 → 11.005: correct decimal arithmetic")
        void decimalArithmetic() {
            result = RiskTrendCalculator.calculate(List.of(
                    new BigDecimal("10.5"),
                    new BigDecimal("10.75"),
                    new BigDecimal("11.005")
            ));

            assertEquals(new BigDecimal("11.005"), result.currentScore());
            assertEquals(new BigDecimal("10.75"), result.previousScore());
            assertBigDecimalEquals(new BigDecimal("0.505"), result.totalChange());
            // averageChange = 0.505 / 2 = 0.2525 → rounded to 0.25
            assertBigDecimalEquals(new BigDecimal("0.25"), result.averageChange());
            assertEquals(TrendDirection.RISING, result.direction());

            assertBigDecimalEquals(new BigDecimal("0.25"), result.scoreChanges().get(1).delta());
            assertBigDecimalEquals(new BigDecimal("0.255"), result.scoreChanges().get(2).delta());
        }

        @Test
        @DisplayName("negative change with decimals: 5.5 → 3.25 → 1.75")
        void decimalDecrease() {
            result = RiskTrendCalculator.calculate(List.of(
                    new BigDecimal("5.5"),
                    new BigDecimal("3.25"),
                    new BigDecimal("1.75")
            ));

            assertEquals(new BigDecimal("1.75"), result.currentScore());
            assertBigDecimalEquals(new BigDecimal("-3.75"), result.totalChange());
            // averageChange = -3.75 / 2 = -1.875 → rounded to -1.88 (HALF_UP)
            assertBigDecimalEquals(new BigDecimal("-1.88"), result.averageChange());
            assertEquals(TrendDirection.FALLING, result.direction());
        }
    }

    // -----------------------------------------------------------------
    // Mixed changes
    // -----------------------------------------------------------------
    @Nested
    @DisplayName("Mixed changes")
    class MixedChanges {

        @Test
        @DisplayName("10 → 20 → 15 → 25 → 5: ups and downs, but net FALLING")
        void volatileUpsAndDowns() {
            result = RiskTrendCalculator.calculate(List.of(
                    new BigDecimal("10"),
                    new BigDecimal("20"),
                    new BigDecimal("15"),
                    new BigDecimal("25"),
                    new BigDecimal("5")
            ));

            assertEquals(new BigDecimal("5"), result.currentScore());
            assertEquals(new BigDecimal("25"), result.previousScore());
            assertBigDecimalEquals(new BigDecimal("-5"), result.totalChange());
            // averageChange = -5 / 4 = -1.25
            assertBigDecimalEquals(new BigDecimal("-1.25"), result.averageChange());
            assertEquals(TrendDirection.FALLING, result.direction());

            // Per-point: +10 (RISING), -5 (FALLING), +10 (RISING), -20 (FALLING)
            assertBigDecimalEquals(new BigDecimal("10"), result.scoreChanges().get(1).delta());
            assertEquals(TrendDirection.RISING, result.scoreChanges().get(1).trend());

            assertBigDecimalEquals(new BigDecimal("-5"), result.scoreChanges().get(2).delta());
            assertEquals(TrendDirection.FALLING, result.scoreChanges().get(2).trend());

            assertBigDecimalEquals(new BigDecimal("10"), result.scoreChanges().get(3).delta());
            assertEquals(TrendDirection.RISING, result.scoreChanges().get(3).trend());

            assertBigDecimalEquals(new BigDecimal("-20"), result.scoreChanges().get(4).delta());
            assertEquals(TrendDirection.FALLING, result.scoreChanges().get(4).trend());
        }

        @Test
        @DisplayName("20 → 10 → 30 → 20: net STABLE (equal first and last)")
        void netStableWithFluctuations() {
            result = RiskTrendCalculator.calculate(List.of(
                    new BigDecimal("20"),
                    new BigDecimal("10"),
                    new BigDecimal("30"),
                    new BigDecimal("20")
            ));

            assertEquals(new BigDecimal("20"), result.currentScore());
            assertBigDecimalEquals(BigDecimal.ZERO, result.totalChange());
            assertBigDecimalEquals(BigDecimal.ZERO, result.averageChange());
            assertEquals(TrendDirection.STABLE, result.direction());
        }

        @Test
        @DisplayName("large numbers: 999 → 1001 → 998 → 1002")
        void largeNumbers() {
            result = RiskTrendCalculator.calculate(List.of(
                    new BigDecimal("999"),
                    new BigDecimal("1001"),
                    new BigDecimal("998"),
                    new BigDecimal("1002")
            ));

            assertEquals(new BigDecimal("1002"), result.currentScore());
            assertBigDecimalEquals(new BigDecimal("3"), result.totalChange());
            // averageChange = 3 / 4 = 0.75
            // averageChange = 3 / 3 = 1.00
            assertBigDecimalEquals(new BigDecimal("1.00"), result.averageChange());
            assertEquals(TrendDirection.RISING, result.direction());
        }
    }
}
