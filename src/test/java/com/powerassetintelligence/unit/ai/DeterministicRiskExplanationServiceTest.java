package com.powerassetintelligence.unit.ai;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.powerassetintelligence.core.ai.DeterministicRiskExplanationService;
import com.powerassetintelligence.core.ai.RiskFeatures;
import com.powerassetintelligence.core.ai.RiskScoringResult;
import com.powerassetintelligence.core.ai.RuleBasedRiskEngine;
import com.powerassetintelligence.domain.model.AssetCriticality;
import com.powerassetintelligence.domain.model.AssetStatus;
import com.powerassetintelligence.domain.model.AssetType;
import com.powerassetintelligence.domain.model.RiskLevel;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class DeterministicRiskExplanationServiceTest {

    private final DeterministicRiskExplanationService service = new DeterministicRiskExplanationService();
    private final RuleBasedRiskEngine engine = new RuleBasedRiskEngine();

    private RiskFeatures highRiskFeatures;
    private RiskFeatures lowRiskFeatures;

    @BeforeEach
    void setUp() {
        highRiskFeatures = new RiskFeatures(
                UUID.randomUUID(),
                AssetType.TRANSFORMER,
                AssetStatus.ACTIVE,
                AssetCriticality.CRITICAL,
                25,
                BigDecimal.valueOf(96.0),
                BigDecimal.valueOf(95.0),
                5,
                5L, null, null, null, null, 0L, null, null
        );

        lowRiskFeatures = new RiskFeatures(
                UUID.randomUUID(),
                AssetType.CIRCUIT_BREAKER,
                AssetStatus.ACTIVE,
                AssetCriticality.LOW,
                5,
                BigDecimal.valueOf(60.0),
                BigDecimal.valueOf(50.0),
                0,
                0L, null, null, null, null, 0L, null, null
        );
    }

    @Test
    void explainShouldProduceValidResultForHighRisk() {
        RiskScoringResult score = engine.score(highRiskFeatures);
        var result = service.explain(score, highRiskFeatures);

        assertNotNull(result);
        assertNotNull(result.explanation());
        assertNotNull(result.recommendations());
        assertNotNull(result.modelVersion());
        assertEquals(RuleBasedRiskEngine.MODEL_VERSION, result.modelVersion());
        assertTrue(result.explanation().contains(highRiskFeatures.assetId().toString()));
        assertTrue(result.recommendations().size() >= 1);
    }

    @Test
    void explainShouldProduceValidResultForLowRisk() {
        RiskScoringResult score = engine.score(lowRiskFeatures);
        var result = service.explain(score, lowRiskFeatures);

        assertNotNull(result);
        assertNotNull(result.explanation());
        assertNotNull(result.recommendations());
        assertNotNull(result.modelVersion());
        assertEquals(RuleBasedRiskEngine.MODEL_VERSION, result.modelVersion());
        assertTrue(result.recommendations().size() >= 1);
        assertTrue(result.recommendations().contains("Keep standard preventive maintenance schedule"));
    }

    @Test
    void explainShouldContainScoreInExplanation() {
        RiskScoringResult score = engine.score(highRiskFeatures);
        var result = service.explain(score, highRiskFeatures);

        String explanation = result.explanation();
        assertTrue(explanation.contains(score.riskLevel().toString()),
                "Explanation should contain the risk level");
        assertTrue(explanation.contains("score="),
                "Explanation should contain the score field");
    }

    @Test
    void explainShouldIncludeFeatureContextInExplanation() {
        RiskScoringResult score = engine.score(highRiskFeatures);
        var result = service.explain(score, highRiskFeatures);

        String explanation = result.explanation();
        assertTrue(explanation.contains("age=25y"),
                "Explanation should include asset age");
        assertTrue(explanation.contains("temp=96.0"),
                "Explanation should include temperature");
        assertTrue(explanation.contains("load=95.0"),
                "Explanation should include load percentage");
        assertTrue(explanation.contains("model=" + RuleBasedRiskEngine.MODEL_VERSION),
                "Explanation should include model version");
    }

    @Test
    void explainShouldReturnImmutableRecommendations() {
        RiskScoringResult score = engine.score(highRiskFeatures);
        var result = service.explain(score, highRiskFeatures);

        List<String> recommendations = result.recommendations();
        try {
            recommendations.add("injected");
            throw new AssertionError("Should not be able to modify recommendations");
        } catch (UnsupportedOperationException e) {
            // Expected
        }
    }

    @Test
    void explainShouldHandleNullTelemetryFeatures() {
        RiskFeatures sparseFeatures = new RiskFeatures(
                UUID.randomUUID(),
                AssetType.TRANSFORMER,
                AssetStatus.ACTIVE,
                AssetCriticality.MEDIUM,
                10,
                null, // no temperature
                null, // no load
                null, // no overheating
                0L, null, null, null, null, 0L, null, null
        );

        RiskScoringResult score = engine.score(sparseFeatures);
        var result = service.explain(score, sparseFeatures);

        assertNotNull(result);
        assertNotNull(result.explanation());
        assertNotNull(result.modelVersion());
    }

    @Test
    void explainShouldProduceDifferentRecommendationsForDifferentLevels() {
        RiskScoringResult criticalScore = engine.score(highRiskFeatures);
        RiskScoringResult lowScore = engine.score(lowRiskFeatures);

        var criticalResult = service.explain(criticalScore, highRiskFeatures);
        var lowResult = service.explain(lowScore, lowRiskFeatures);

        // Different risk levels should produce different recommendations
        if (criticalScore.riskLevel() != lowScore.riskLevel()) {
            String criticalRec = criticalResult.recommendations().get(0);
            String lowRec = lowResult.recommendations().get(0);
            assertNotEquals(criticalRec, lowRec,
                    "Different risk levels should produce different recommendations");
        }
    }
}
