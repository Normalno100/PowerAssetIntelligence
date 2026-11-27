package com.powerassetintelligence.infrastructure.ai.ml;

import com.powerassetintelligence.domain.model.AssetStatus;
import com.powerassetintelligence.domain.model.AssetType;
import com.powerassetintelligence.domain.model.RiskFeatures;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link MlClient} using direct method calls
 * (WebClient is not mocked — we test DTO mapping and fallback logic).
 */
class MlClientTest {

    private static final UUID TEST_ASSET_ID = UUID.randomUUID();

    private final MlClient client = new MlClient("http://ml-service:8000");

    private RiskFeatures createTestFeatures() {
        return new RiskFeatures(
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
    @DisplayName("fallback() — circuit breaker / timeout / 5xx")
    class FallbackTests {

        @Test
        @DisplayName("should throw MlServiceUnavailableException with descriptive message")
        void shouldThrowMlServiceUnavailableException() {
            Exception timeoutEx = new org.springframework.web.reactive.function.client.WebClientResponseException(
                    504, "Gateway Timeout",
                    org.springframework.http.HttpHeaders.EMPTY,
                    new byte[0],
                    java.nio.charset.StandardCharsets.UTF_8);

            assertThatThrownBy(() -> client.fallback(createTestFeatures(), timeoutEx))
                    .isInstanceOf(MlServiceUnavailableException.class)
                    .hasMessageContaining("ML service unavailable")
                    .hasMessageContaining("Gateway Timeout");
        }

        @Test
        @DisplayName("should throw for CallNotPermittedException (circuit open)")
        void shouldThrowForCircuitOpen() {
            // Create a simple RuntimeException to simulate circuit open
            Exception cbEx = new RuntimeException("CircuitBreaker 'mlClient' is open");

            assertThatThrownBy(() -> client.fallback(createTestFeatures(), cbEx))
                    .isInstanceOf(MlServiceUnavailableException.class)
                    .hasMessageContaining("circuit open");
        }

        @Test
        @DisplayName("should throw for timeout exception")
        void shouldThrowForTimeout() {
            Exception timeoutEx = new java.util.concurrent.TimeoutException("Call timed out after 3000ms");

            assertThatThrownBy(() -> client.fallback(createTestFeatures(), timeoutEx))
                    .isInstanceOf(MlServiceUnavailableException.class)
                    .hasMessageContaining("timeout");
        }
    }

    @Nested
    @DisplayName("DTO mapping")
    class DtoMappingTests {

        @Test
        @DisplayName("should map ML response to RiskScoringResult correctly")
        void shouldMapMlResponseToScoringResult() {
            MlClient.MlPredictResponse response = new MlClient.MlPredictResponse(
                    TEST_ASSET_ID.toString(),
                    0.75f,
                    82,
                    "HIGH"
            );

            com.powerassetintelligence.application.dto.RiskScoringResult result =
                    client.toScoringResult(response);

            assertThat(result.riskScore()).isEqualTo(BigDecimal.valueOf(82));
            assertThat(result.riskLevel()).isEqualTo(com.powerassetintelligence.domain.model.RiskLevel.HIGH);
            assertThat(result.riskFactors()).hasSize(1);
            assertThat(result.riskFactors().get(0).code()).isEqualTo("ml_failure_probability");
        }

        @Test
        @DisplayName("should map null/unknown risk_level to MEDIUM")
        void shouldMapNullRiskLevelToMedium() {
            MlClient.MlPredictResponse response = new MlClient.MlPredictResponse(
                    TEST_ASSET_ID.toString(),
                    0.3f,
                    30,
                    null
            );

            com.powerassetintelligence.application.dto.RiskScoringResult result =
                    client.toScoringResult(response);

            assertThat(result.riskLevel()).isEqualTo(com.powerassetintelligence.domain.model.RiskLevel.MEDIUM);
        }

        @Test
        @DisplayName("should map empty risk_level to MEDIUM")
        void shouldMapEmptyRiskLevelToMedium() {
            MlClient.MlPredictResponse response = new MlClient.MlPredictResponse(
                    TEST_ASSET_ID.toString(),
                    0.3f,
                    30,
                    ""
            );

            com.powerassetintelligence.application.dto.RiskScoringResult result =
                    client.toScoringResult(response);

            assertThat(result.riskLevel()).isEqualTo(com.powerassetintelligence.domain.model.RiskLevel.MEDIUM);
        }

        @Test
        @DisplayName("should handle null failure_probability")
        void shouldHandleNullFailureProbability() {
            MlClient.MlPredictResponse response = new MlClient.MlPredictResponse(
                    TEST_ASSET_ID.toString(),
                    null,
                    50,
                    "MEDIUM"
            );

            com.powerassetintelligence.application.dto.RiskScoringResult result =
                    client.toScoringResult(response);

            assertThat(result.riskScore()).isEqualTo(BigDecimal.valueOf(50));
            assertThat(result.riskFactors().get(0).contribution().doubleValue()).isCloseTo(0.0, org.assertj.core.data.Percentage.withPercentage(0.001));
        }
    }
}
