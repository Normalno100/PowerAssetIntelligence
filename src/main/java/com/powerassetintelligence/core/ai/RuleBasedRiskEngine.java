package com.powerassetintelligence.core.ai;

import com.powerassetintelligence.core.ai.rule.AgeRiskRule;
import com.powerassetintelligence.core.ai.rule.AgingOverheatRepairRiskRule;
import com.powerassetintelligence.core.ai.rule.HighLoadCoolingRiskRule;
import com.powerassetintelligence.core.ai.rule.HighTemperatureRiskRule;
import com.powerassetintelligence.core.ai.rule.MissingTelemetryRiskRule;
import com.powerassetintelligence.core.ai.rule.RepairHistoryRiskRule;
import com.powerassetintelligence.domain.model.AssetCriticality;
import com.powerassetintelligence.domain.model.RiskLevel;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public class RuleBasedRiskEngine {

    public static final String MODEL_VERSION = "rules-2026.05";
    private static final BigDecimal MAX_SCORE = BigDecimal.valueOf(100);

    private final List<RiskRule> rules;

    public RuleBasedRiskEngine() {
        this.rules = List.of(
                new AgingOverheatRepairRiskRule(),
                new HighTemperatureRiskRule(),
                new HighLoadCoolingRiskRule(),
                new AgeRiskRule(),
                new RepairHistoryRiskRule(),
                new MissingTelemetryRiskRule()
        );
    }

    public RiskScoringResult score(RiskFeatures features) {
        List<RiskRuleResult> matchedRules = rules.stream()
                .map(rule -> rule.evaluate(features))
                .flatMap(optional -> optional.stream())
                .toList();

        BigDecimal baseScore = matchedRules.stream()
                .map(RiskRuleResult::scoreContribution)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal criticalityBonus = criticalityBonus(features.criticality());
        BigDecimal score = baseScore.add(criticalityBonus).min(MAX_SCORE).setScale(2, RoundingMode.HALF_UP);
        RiskLevel level = toRiskLevel(score);

        Set<String> recommendations = new LinkedHashSet<>();
        List<String> riskFactors = new ArrayList<>();
        for (RiskRuleResult result : matchedRules) {
            riskFactors.add(result.ruleCode() + ": " + result.riskFactor());
            recommendations.addAll(result.recommendations());
        }
        addLevelRecommendation(level, recommendations);

        if (riskFactors.isEmpty()) {
            riskFactors.add("BASELINE: No risk rules were triggered");
            recommendations.add("Continue routine monitoring");
        }
        if (criticalityBonus.compareTo(BigDecimal.ZERO) > 0) {
            riskFactors.add("ASSET_CRITICALITY: Criticality bonus applied: " + criticalityBonus);
        }

        return new RiskScoringResult(
                score,
                level,
                List.copyOf(riskFactors),
                List.copyOf(recommendations),
                buildExplanation(features, matchedRules.size(), criticalityBonus),
                MODEL_VERSION
        );
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

    private void addLevelRecommendation(RiskLevel level, Set<String> recommendations) {
        switch (level) {
            case CRITICAL -> recommendations.add("Create immediate maintenance work order and notify dispatcher");
            case HIGH -> recommendations.add("Prioritize asset in the next maintenance planning window");
            case MEDIUM -> recommendations.add("Increase monitoring frequency and schedule diagnostics");
            case LOW -> recommendations.add("Keep standard preventive maintenance schedule");
        }
    }

    private String buildExplanation(RiskFeatures features, int matchedRuleCount, BigDecimal criticalityBonus) {
        return "Rule-based assessment evaluated " + matchedRuleCount
                + " triggered rules for asset " + features.assetId()
                + "; ageYears=" + features.assetAgeYears()
                + "; latestTemperatureCelsius=" + features.latestTemperatureCelsius()
                + "; latestLoadPercent=" + features.latestLoadPercent()
                + "; repairsLastYear=" + features.repairsLastYear()
                + "; criticalityBonus=" + criticalityBonus
                + "; modelVersion=" + MODEL_VERSION;
    }
}
