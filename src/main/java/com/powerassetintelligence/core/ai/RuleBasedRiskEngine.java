package com.powerassetintelligence.core.ai;

import com.powerassetintelligence.core.ai.rule.AgeRiskRule;
import com.powerassetintelligence.core.ai.rule.AgingOverheatRepairRiskRule;
import com.powerassetintelligence.core.ai.rule.HighLoadCoolingRiskRule;
import com.powerassetintelligence.core.ai.rule.HighTemperatureRiskRule;
import com.powerassetintelligence.core.ai.rule.MissingTelemetryRiskRule;
import com.powerassetintelligence.core.ai.rule.RepairHistoryRiskRule;
import com.powerassetintelligence.core.ai.rule.SustainedHighTemperatureRiskRule;
import com.powerassetintelligence.core.ai.rule.TemperatureTrendRiskRule;
import com.powerassetintelligence.domain.model.AssetCriticality;
import com.powerassetintelligence.domain.model.RiskLevel;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Deterministic rule-based risk scoring engine.
 * <p>
 * Implements {@link CoreRiskScoringPort} to produce lean scoring results
 * ({@code riskScore}, {@code riskLevel}, {@code riskFactors}). Human-readable
 * explanations are generated separately by {@link RiskExplanationService}.
 *
 * <pre>
 * Example:
 *   CoreRiskScoringPort engine = new RuleBasedRiskEngine();
 *   RiskScoringResult result = engine.score(features);
 * </pre>
 *
 * @see CoreRiskScoringPort
 * @see RiskExplanationService
 */
@Component
public class RuleBasedRiskEngine implements CoreRiskScoringPort {

    public static final String MODEL_VERSION = "rules-2026.05";
    private static final BigDecimal MAX_SCORE = BigDecimal.valueOf(100);

    private final List<RiskRule> rules;

    public RuleBasedRiskEngine() {
        this.rules = List.of(
                new AgingOverheatRepairRiskRule(),
                new HighTemperatureRiskRule(),
                new SustainedHighTemperatureRiskRule(),
                new TemperatureTrendRiskRule(),
                new HighLoadCoolingRiskRule(),
                new AgeRiskRule(),
                new RepairHistoryRiskRule(),
                new MissingTelemetryRiskRule()
        );
    }

    @Override
    public RiskScoringResult score(RiskFeatures features) {
        List<RiskRuleResult> matchedRules = evaluateRules(features);

        BigDecimal baseScore = sumContributions(matchedRules);
        BigDecimal criticalityBonus = criticalityBonus(features.criticality());
        BigDecimal score = baseScore.add(criticalityBonus).min(MAX_SCORE).setScale(2, RoundingMode.HALF_UP);
        RiskLevel level = toRiskLevel(score);

        List<RiskFactor> riskFactors = matchedRules.stream()
                .map(RiskRuleResult::riskFactor)
                .toList();

        return new RiskScoringResult(score, level, riskFactors);
    }

    private List<RiskRuleResult> evaluateRules(RiskFeatures features) {
        return rules.stream()
                .map(rule -> rule.evaluate(features))
                .flatMap(optional -> optional.stream())
                .toList();
    }

    private BigDecimal sumContributions(List<RiskRuleResult> matchedRules) {
        return matchedRules.stream()
                .map(r -> r.riskFactor().contribution())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal criticalityBonus(AssetCriticality criticality) {
        if (criticality == null) {
            return BigDecimal.ZERO;
        }
        return switch (criticality) {
            case CRITICAL -> BigDecimal.valueOf(15);
            case HIGH -> BigDecimal.valueOf(10);
            case MEDIUM -> BigDecimal.valueOf(5);
            case LOW -> BigDecimal.ZERO;
        };
    }

    private RiskLevel toRiskLevel(BigDecimal score) {
        if (score.compareTo(BigDecimal.valueOf(85)) >= 0) {
            return RiskLevel.CRITICAL;
        }
        if (score.compareTo(BigDecimal.valueOf(65)) >= 0) {
            return RiskLevel.HIGH;
        }
        if (score.compareTo(BigDecimal.valueOf(35)) >= 0) {
            return RiskLevel.MEDIUM;
        }
        return RiskLevel.LOW;
    }
}
