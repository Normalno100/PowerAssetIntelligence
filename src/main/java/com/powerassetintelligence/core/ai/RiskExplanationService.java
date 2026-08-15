package com.powerassetintelligence.core.ai;

/**
 * Port for generating human-readable risk explanations from a scoring result.
 *
 * <p>This interface separates explanation generation from score computation,
 * enabling different explanation strategies (deterministic, LLM-based, hybrid)
 * without modifying the scoring engine.
 *
 * <pre>
 * Example usage:
 *   RiskScoringResult score = scoringPort.score(features);
 *   RiskExplanationResult explanation = explanationService.explain(score, features);
 * </pre>
 */
public interface RiskExplanationService {

    /**
     * Generates an explanation for the given scoring result.
     *
     * @param scoringResult the deterministic scoring result
     * @param features the original risk features (used for detailed context)
     * @return an explanation result containing narrative text, recommendations, and model metadata
     */
    RiskExplanationResult explain(RiskScoringResult scoringResult, RiskFeatures features);
}
