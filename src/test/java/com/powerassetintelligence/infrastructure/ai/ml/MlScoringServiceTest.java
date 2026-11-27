package com.powerassetintelligence.infrastructure.ai.ml;

import com.powerassetintelligence.application.dto.RiskScoringResult;
import com.powerassetintelligence.core.ai.RuleBasedRiskEngine;
import com.powerassetintelligence.domain.model.AssetStatus;
import com.powerassetintelligence.domain.model.AssetType;
import com.powerassetintelligence.domain.model.RiskFeatures;
import com.powerassetintelligence.domain.model.RiskFactor;
import com.powerassetintelligence.domain.model.RiskFactorSeverity;
import com.powerassetintelligence.domain.model.RiskLevel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link MlScoringService} covering:
 * <ul>
 *   <li>ML success — scoringSource = "ML"</li>
 *   <li>ML failure (timeout, 500, CB open) — scoringSource = "FALLBACK"</li>
 *   <li>Rule engine is never called when ML succeeds</li>
 *   <li>Rule engine is called exactly once when ML fails</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
class MlScoringServiceTest {

    private static final UUID TEST_ASSET_ID = UUID.randomUUID();

    @Mock
    private MlClient mlClient;

    @Mock
    private RuleBasedRiskEngine ruleEngine;

    private MlScoringService service;

    private RiskFeatures testFeatures;

    @BeforeEach
    void setUp() {
        service = new MlScoringService(mlClient, ruleEngine);
        testFeatures = new RiskFeatures(
                TEST_ASSET_ID,
                AssetType.SUBSTATION,
                AssetStatus.ACTIVE,
                com.powerassetintelligence.domain.model.AssetCriticality.HIGH,
                10,
                BigDecimal.valueOf(45.5),
                BigDecimal.valueOf(72.0),
                3,
                2L,
                BigDecimal.valueOf(40.0),
                BigDecimal.valueOf(55.0),
                BigDecimal.valueOf(65.0),
                BigDecimal.valueOf(80.0),
                5L,
                BigDecimal.valueOf(2.5),
                BigDecimal.valueOf(1.2)
        );
    }

    @Nested
    @DisplayName("ML success scenario")
    class MlSuccess {

        @Test
        @DisplayName("should return ML result with scoringSource=ML when ML succeeds")
        void shouldReturnMlResultWhenMlSucceeds() {
            RiskScoringResult mlResult = new RiskScoringResult(
                    BigDecimal.valueOf(75),
                    RiskLevel.HIGH,
                    List.of(RiskFactor.of("ml_prob", RiskFactorSeverity.HIGH, "ML probability", BigDecimal.valueOf(0.8))),
                    "ML"
            );

            when(mlClient.predict(testFeatures)).thenReturn(mlResult);

            RiskScoringResult result = service.score(testFeatures);

            assertThat(result.riskScore()).isEqualTo(BigDecimal.valueOf(75));
            assertThat(result.riskLevel()).isEqualTo(RiskLevel.HIGH);
            assertThat(result.scoringSource()).isEqualTo("ML");
            // Rule engine should NOT be called
            verify(ruleEngine, never()).score(any());
        }
    }

    @Nested
    @DisplayName("ML failure — fallback to rule-based")
    class MlFailureFallback {

        @Test
        @DisplayName("should fallback to rule-based when ML throws MlServiceUnavailableException")
        void shouldFallbackOnMlServiceUnavailable() {
            RiskScoringResult fallbackResult = new RiskScoringResult(
                    BigDecimal.valueOf(60),
                    RiskLevel.HIGH,
                    List.of(RiskFactor.of("high_temp", RiskFactorSeverity.HIGH, "High temperature", BigDecimal.valueOf(25))),
                    null
            );

            when(mlClient.predict(testFeatures))
                    .thenThrow(new MlServiceUnavailableException("ML service timeout", new java.util.concurrent.TimeoutException()));
            when(ruleEngine.score(testFeatures)).thenReturn(fallbackResult);

            RiskScoringResult result = service.score(testFeatures);

            assertThat(result.riskScore()).isEqualTo(BigDecimal.valueOf(60));
            assertThat(result.riskLevel()).isEqualTo(RiskLevel.HIGH);
            assertThat(result.scoringSource()).isEqualTo("FALLBACK");
            // Rule engine should be called once
            verify(ruleEngine).score(testFeatures);
        }

        @Test
        @DisplayName("should fallback when ML returns 500 error")
        void shouldFallbackOnMl500() {
            RiskScoringResult fallbackResult = new RiskScoringResult(
                    BigDecimal.valueOf(45),
                    RiskLevel.MEDIUM,
                    List.of(),
                    null
            );

            when(mlClient.predict(testFeatures))
                    .thenThrow(new MlServiceUnavailableException(
                            "ML service unavailable (circuit open / timeout / retry exhausted): 500 Internal Server Error",
                            new org.springframework.web.reactive.function.client.WebClientResponseException(
                                    500, "Internal Server Error",
                                    org.springframework.http.HttpHeaders.EMPTY,
                                    new byte[0],
                                    java.nio.charset.StandardCharsets.UTF_8)));
            when(ruleEngine.score(testFeatures)).thenReturn(fallbackResult);

            RiskScoringResult result = service.score(testFeatures);

            assertThat(result.scoringSource()).isEqualTo("FALLBACK");
            assertThat(result.riskScore()).isEqualTo(BigDecimal.valueOf(45));
        }

        @Test
        @DisplayName("should fallback when circuit breaker is open")
        void shouldFallbackWhenCircuitBreakerOpen() {
            RiskScoringResult fallbackResult = new RiskScoringResult(
                    BigDecimal.valueOf(30),
                    RiskLevel.LOW,
                    List.of(),
                    null
            );

            when(mlClient.predict(testFeatures))
                    .thenThrow(new MlServiceUnavailableException(
                            "ML service unavailable (circuit open / timeout / retry exhausted): CircuitBreaker 'mlClient' is open",
                            new RuntimeException("CircuitBreaker 'mlClient' is open")));
            when(ruleEngine.score(testFeatures)).thenReturn(fallbackResult);

            RiskScoringResult result = service.score(testFeatures);

            assertThat(result.scoringSource()).isEqualTo("FALLBACK");
            assertThat(result.riskLevel()).isEqualTo(RiskLevel.LOW);
        }

        @Test
        @DisplayName("should preserve rule-based risk factors in fallback")
        void shouldPreserveRiskFactorsInFallback() {
            RiskScoringResult fallbackResult = new RiskScoringResult(
                    BigDecimal.valueOf(85),
                    RiskLevel.CRITICAL,
                    List.of(
                            RiskFactor.of("high_temp", RiskFactorSeverity.HIGH, "High temperature", BigDecimal.valueOf(30)),
                            RiskFactor.of("aging", RiskFactorSeverity.MEDIUM, "Asset aging", BigDecimal.valueOf(15))
                    ),
                    null
            );

            when(mlClient.predict(testFeatures))
                    .thenThrow(new MlServiceUnavailableException("Timeout", new java.util.concurrent.TimeoutException()));
            when(ruleEngine.score(testFeatures)).thenReturn(fallbackResult);

            RiskScoringResult result = service.score(testFeatures);

            assertThat(result.riskFactors()).hasSize(2);
            assertThat(result.riskFactors().get(0).code()).isEqualTo("high_temp");
            assertThat(result.riskFactors().get(1).code()).isEqualTo("aging");
        }
    }
}
