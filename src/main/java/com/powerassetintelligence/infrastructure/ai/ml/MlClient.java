package com.powerassetintelligence.infrastructure.ai.ml;

import com.powerassetintelligence.application.dto.RiskScoringResult;
import com.powerassetintelligence.domain.model.RiskFactor;
import com.powerassetintelligence.domain.model.RiskFactorSeverity;
import com.powerassetintelligence.domain.model.RiskFeatures;
import com.powerassetintelligence.domain.model.RiskLevel;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import java.math.BigDecimal;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

/**
 * HTTP client for the ML prediction service.
 * <p>
 * Sends telemetry features to {@code POST /v1/predict} and maps the
 * response into a {@link RiskScoringResult}.
 * </p>
 * <p>
 * Protected by Resilience4j:
 * <ul>
 *   <li>timeout via Spring timeout config</li>
 *   <li>circuit breaker (50 % failure threshold, 10 s open window)</li>
 *   <li>one retry on failure</li>
 * </ul>
 *
 * @see <a href=
 *      "https://resilience4j.readme.io/docs/spring_boot_3">Resilience4j Spring Boot 3</a>
 */
@Component
public class MlClient {

    private static final Logger log = LoggerFactory.getLogger(MlClient.class);

    private final WebClient webClient;

    public MlClient(@Value("${app.ml.base-url:http://ml-service:8000}") String baseUrl) {
        this.webClient = WebClient.builder()
                .baseUrl(baseUrl)
                .build();
    }

    /**
     * Call the ML prediction endpoint for the given features.
     *
     * @param features domain features extracted from telemetry
     * @return scoring result from the ML model
     * @throws MlServiceUnavailableException when the circuit breaker is open,
     *             the call times out, or the service returns 5xx
     */
    @CircuitBreaker(name = "mlClient", fallbackMethod = "fallback")
    @Retry(name = "mlClient")
    public RiskScoringResult predict(RiskFeatures features) {
        log.debug("Calling ML predict for asset {}", features.assetId());

        MlPredictRequest request = new MlPredictRequest(
                features.assetId().toString(),
                toFloat(features.latestTemperatureCelsius()),
                toFloat(features.latestLoadPercent()),
                0.0f, // vibration_mm_s — not collected yet
                0.0f, // voltage_kv — not collected yet
                0.0f, // humidity_pct — not collected yet
                toFloat(BigDecimal.valueOf(features.assetAgeYears())),
                (int) features.repairsLastYear() // approximation of failure_count_12m
        );

        MlPredictResponse response = webClient.post()
                .uri("/v1/predict")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(MlPredictResponse.class)
                .block();

        return toScoringResult(response);
    }

    /**
     * Fallback when the circuit breaker is open or the call is otherwise
     * blocked. Throws a runtime exception so that the caller
     * (MlScoringService) can delegate to the rule-based engine.
     */
    public RiskScoringResult fallback(RiskFeatures features, Exception ex) {
        log.warn("ML predict fallback triggered for asset {}: {}",
                features.assetId(), ex.getMessage());
        throw new MlServiceUnavailableException(
                "ML service unavailable (circuit open / timeout / retry exhausted): "
                        + ex.getMessage(), ex);
    }

    /** Package-private for testing. */
    RiskScoringResult toScoringResult(MlPredictResponse resp) {
        RiskLevel level = RiskLevel.fromCode(resp.risk_level());
        Float prob = resp.failure_probability() != null ? resp.failure_probability() : 0.0f;
        RiskFactor factor = RiskFactor.of(
                "ml_failure_probability",
                RiskFactorSeverity.MEDIUM,
                "ML failure probability",
                BigDecimal.valueOf(prob.doubleValue())
        );
        return new RiskScoringResult(
                BigDecimal.valueOf(resp.risk_score()),
                level,
                List.of(factor),
                null // scoringSource is set by MlScoringService
        );
    }

    private static Float toFloat(BigDecimal value) {
        return value != null ? value.floatValue() : 0.0f;
    }

    // --- DTOs ---

    public record MlPredictRequest(
            String asset_id,
            Float temperature_c,
            Float load_pct,
            Float vibration_mm_s,
            Float voltage_kv,
            Float humidity_pct,
            Float age_years,
            Integer failure_count_12m
    ) {}

    public record MlPredictResponse(
            String asset_id,
            Float failure_probability,
            Integer risk_score,
            String risk_level
    ) {}
}
