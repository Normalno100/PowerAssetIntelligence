package com.powerassetintelligence.core.ai;

import java.util.List;

/**
 * Result of risk explanation generation.
 *
 * <p>Contains the human-readable components that are separated from pure
 * numeric scoring: an explanation narrative, actionable recommendations,
 * and model version metadata.
 *
 * <pre>
 * Example:
 *   new RiskExplanationResult(
 *       List.of("Inspect thermal condition", "Increase monitoring frequency"),
 *       "Rule-based assessment evaluated 2 triggered rules...",
 *       "rules-2026.05"
 *   )
 * </pre>
 */
public record RiskExplanationResult(

        /**
         * Actionable recommendations for maintenance or monitoring.
         */
        List<String> recommendations,

        /**
         * Human-readable explanation of how the risk was assessed.
         */
        String explanation,

        /**
         * Version identifier of the model/engine used for scoring.
         */
        String modelVersion
) {
}
