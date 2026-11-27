package com.powerassetintelligence.infrastructure.ai.ml;

import com.powerassetintelligence.application.dto.RiskScoringResult;
import com.powerassetintelligence.core.ai.CoreRiskScoringPort;
import com.powerassetintelligence.core.ai.RuleBasedRiskEngine;
import com.powerassetintelligence.domain.model.RiskFeatures;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Decorator that attempts ML scoring and falls back to the deterministic
 * rule-based engine when the ML service is unavailable.
 * <p>
 * The decision to use ML or fallback is transparent to the caller — the
 * {@code modelVersion} field on the returned {@link RiskScoringResult} (via
 * {@code scoringSource}) indicates which path was taken:
 * <ul>
 *   <li>{@code "ML"} — ML result</li>
 *   <li>{@code "FALLBACK"} — rule-based fallback</li>
 * </ul>
 *
 * <p>
 * This class is the single place where ML and rule-based scoring are
 * combined, keeping {@link com.powerassetintelligence.application.service.RiskAnalysisService}
 * clean and focused on orchestration.
 * </p>
 */
@Component
public class MlScoringService implements CoreRiskScoringPort {

    private static final Logger log = LoggerFactory.getLogger(MlScoringService.class);

    private static final String SCORING_SOURCE_ML = "ML";
    private static final String SCORING_SOURCE_FALLBACK = "FALLBACK";

    private final MlClient mlClient;
    private final CoreRiskScoringPort ruleEngine;

    public MlScoringService(MlClient mlClient, RuleBasedRiskEngine ruleEngine) {
        this.mlClient = mlClient;
        this.ruleEngine = ruleEngine;
    }

    @Override
    public RiskScoringResult score(RiskFeatures features) {
        try {
            RiskScoringResult mlResult = mlClient.predict(features);
            log.debug("ML scoring succeeded for asset {}", features.assetId());
            return new RiskScoringResult(
                    mlResult.riskScore(),
                    mlResult.riskLevel(),
                    mlResult.riskFactors(),
                    SCORING_SOURCE_ML);
        } catch (MlServiceUnavailableException e) {
            log.warn("ML scoring failed for asset {}, falling back to rule-based engine: {}",
                    features.assetId(), e.getMessage());
            RiskScoringResult fallbackResult = ruleEngine.score(features);
            return new RiskScoringResult(
                    fallbackResult.riskScore(),
                    fallbackResult.riskLevel(),
                    fallbackResult.riskFactors(),
                    SCORING_SOURCE_FALLBACK);
        }
    }
}
