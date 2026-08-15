package com.powerassetintelligence.core.ai;

import com.powerassetintelligence.domain.model.RiskLevel;
import java.math.BigDecimal;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.springframework.stereotype.Service;

/**
 * Default deterministic implementation of {@link RiskExplanationService}.
 *
 * <p>Generates human-readable explanations and recommendations based on
 * the scoring result and original risk features. This is the reference
 * implementation — LLM-based alternatives can be added by implementing
 * {@link RiskExplanationService} directly.
 *
 * <pre>
 * Example:
 *   RiskScoringResult score = scoringPort.score(features);
 *   RiskExplanationResult explanation = deterministicService.explain(score, features);
 * </pre>
 */
@Service
public class DeterministicRiskExplanationService implements RiskExplanationService {

    @Override
    public RiskExplanationResult explain(RiskScoringResult scoringResult, RiskFeatures features) {
        List<String> recommendations = buildRecommendations(scoringResult);
        String explanation = buildExplanation(scoringResult, features);
        return new RiskExplanationResult(recommendations, explanation, RuleBasedRiskEngine.MODEL_VERSION);
    }

    private List<String> buildRecommendations(RiskScoringResult scoringResult) {
        RiskLevel level = scoringResult.riskLevel();

        Set<String> recommendations = new LinkedHashSet<>();

        if (scoringResult.riskFactors().isEmpty()) {
            recommendations.add("Continue routine monitoring");
        }
        recommendations.add(buildDefaultRecommendation(level));

        return List.copyOf(recommendations);
    }

    private String buildDefaultRecommendation(RiskLevel level) {
        return switch (level) {
            case CRITICAL -> "Create immediate maintenance work order and notify dispatcher";
            case HIGH -> "Prioritize asset in the next maintenance planning window";
            case MEDIUM -> "Increase monitoring frequency and schedule diagnostics";
            case LOW -> "Keep standard preventive maintenance schedule";
        };
    }

    private String buildExplanation(RiskScoringResult scoringResult, RiskFeatures features) {
        BigDecimal score = scoringResult.riskScore();
        int factorCount = scoringResult.riskFactors().size();
        String level = scoringResult.riskLevel().toString();

        return String.join(" ",
                String.format(Locale.US, "Rule-based assessment evaluated %d contributing factor(s) for asset %s",
                        factorCount, features.assetId()),
                String.format(Locale.US, "(level=%s, score=%.2f, riskFactors=%d)",
                        level, score.doubleValue(), factorCount),
                String.format(Locale.US, "age=%dy, temp=%.1f C, load=%.1f%%, repairs=%d",
                        features.assetAgeYears(),
                        safeVal(features.latestTemperatureCelsius(), 0.0),
                        safeVal(features.latestLoadPercent(), 0.0),
                        features.repairsLastYear()),
                String.format(Locale.US, "model=%s", RuleBasedRiskEngine.MODEL_VERSION)
        );
    }

    private double safeVal(BigDecimal value, double defaultValue) {
        return value != null ? value.doubleValue() : defaultValue;
    }
}
