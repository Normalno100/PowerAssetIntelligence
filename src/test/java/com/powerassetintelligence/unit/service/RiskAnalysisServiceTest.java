package com.powerassetintelligence.unit.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.powerassetintelligence.application.dto.RiskAssessmentDetailsResponse;
import com.powerassetintelligence.application.dto.RiskAssessmentResponse;
import com.powerassetintelligence.application.port.out.RiskAssessmentRepositoryPort;
import com.powerassetintelligence.application.service.AssetService;
import com.powerassetintelligence.application.service.RiskAnalysisService;
import com.powerassetintelligence.application.service.RiskFeaturesExtractor;
import com.powerassetintelligence.core.ai.CoreRiskScoringPort;
import com.powerassetintelligence.core.ai.RiskAssessmentSnapshot;
import com.powerassetintelligence.core.ai.RiskExplanationResult;
import com.powerassetintelligence.core.ai.RiskExplanationService;
import com.powerassetintelligence.core.ai.RiskFeatures;
import com.powerassetintelligence.core.ai.RiskScoringResult;
import com.powerassetintelligence.domain.model.Asset;
import com.powerassetintelligence.domain.model.AssetCriticality;
import com.powerassetintelligence.domain.model.AssetStatus;
import com.powerassetintelligence.domain.model.AssetType;
import com.powerassetintelligence.domain.model.RiskAssessment;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RiskAnalysisServiceTest {

    @Mock
    private AssetService assetService;

    @Mock
    private RiskFeaturesExtractor riskFeaturesExtractor;

    @Mock
    private RiskAssessmentRepositoryPort riskAssessmentRepository;

    @Mock
    private CoreRiskScoringPort riskEngine;

    @Mock
    private RiskExplanationService riskExplanationService;

    @Captor
    private ArgumentCaptor<RiskAssessment> assessmentCaptor;

    private Clock fixedClock;
    private RiskAnalysisService service;

    private final UUID testAssetId = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
    private final Instant now = Instant.now();

    @BeforeEach
    void setUp() {
        fixedClock = Clock.fixed(now, ZoneId.systemDefault());
        service = new RiskAnalysisService(
                assetService,
                riskFeaturesExtractor,
                riskAssessmentRepository,
                riskEngine,
                riskExplanationService,
                fixedClock
        );
    }

    @Test
    void assessShouldCallScoreThenExplainThenPersist() {
        // Arrange
        Asset asset = createAsset(testAssetId);
        RiskFeatures features = createFeatures(testAssetId);
        RiskAssessmentSnapshot snapshot = RiskAssessmentSnapshot.from(features);

        RiskScoringResult scoreResult = new RiskScoringResult(
                BigDecimal.valueOf(72.5),
                com.powerassetintelligence.domain.model.RiskLevel.HIGH,
                List.of()
        );

        RiskExplanationResult explanationResult = new RiskExplanationResult(
                List.of("Prioritize asset in the next maintenance planning window"),
                "Rule-based assessment evaluated 0 contributing factor(s) for asset " + testAssetId
                        + " (level=HIGH, score=72.50) age=10y, temp=65.0°C, load=70.0%, repairs=1 model=rules-2026.05",
                "rules-2026.05"
        );

        when(assetService.getAsset(testAssetId)).thenReturn(asset);
        when(riskFeaturesExtractor.extractWithSnapshot(asset))
                .thenReturn(new RiskFeaturesExtractor.ExtractResult(features, snapshot));
        when(riskEngine.score(features)).thenReturn(scoreResult);
        when(riskExplanationService.explain(scoreResult, features)).thenReturn(explanationResult);
        when(riskAssessmentRepository.save(any(RiskAssessment.class))).thenAnswer(invocation -> {
            RiskAssessment saved = invocation.getArgument(0);
            saved = new RiskAssessment(
                    saved.id(), saved.assetId(), saved.assessedAt(),
                    saved.riskScore(), saved.riskLevel(), saved.riskFactors(),
                    saved.recommendations(), saved.modelVersion(), saved.explanation(),
                    saved.createdAt(), saved.snapshot()
            );
            return saved;
        });

        // Act
        RiskAssessmentDetailsResponse response = service.assess(testAssetId);

        // Assert — phase 1: scoring was called
        verify(riskEngine).score(features);

        // Assert — phase 2: explanation was called with scoring result
        verify(riskExplanationService).explain(scoreResult, features);

        // Assert — response contains data from both phases
        assertNotNull(response);
        assertNotNull(response.assessment());
        assertEquals(BigDecimal.valueOf(72.5), response.assessment().riskScore());
        assertEquals(com.powerassetintelligence.domain.model.RiskLevel.HIGH, response.assessment().riskLevel());
        assertEquals(1, response.assessment().recommendations().size());
        assertEquals("Prioritize asset in the next maintenance planning window",
                response.assessment().recommendations().get(0));
        assertEquals("rules-2026.05", response.assessment().modelVersion());
        assertNotNull(response.assessment().explanation());
    }

    @Test
    void assessShouldUseExplanationResultRecommendationsNotScoreResult() {
        // After refactoring, recommendations come from RiskExplanationResult, not RiskScoringResult
        RiskScoringResult scoreResult = new RiskScoringResult(
                BigDecimal.valueOf(45.0),
                com.powerassetintelligence.domain.model.RiskLevel.MEDIUM,
                List.of()
        );

        RiskExplanationResult explanationResult = new RiskExplanationResult(
                List.of("Custom recommendation from explanation service"),
                "Test explanation",
                "rules-2026.05"
        );

        Asset asset = createAsset(testAssetId);
        RiskFeatures features = createFeatures(testAssetId);
        RiskAssessmentSnapshot snapshot = RiskAssessmentSnapshot.from(features);

        when(assetService.getAsset(testAssetId)).thenReturn(asset);
        when(riskFeaturesExtractor.extractWithSnapshot(asset))
                .thenReturn(new RiskFeaturesExtractor.ExtractResult(features, snapshot));
        when(riskEngine.score(features)).thenReturn(scoreResult);
        when(riskExplanationService.explain(scoreResult, features)).thenReturn(explanationResult);
        when(riskAssessmentRepository.save(any(RiskAssessment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        RiskAssessmentDetailsResponse response = service.assess(testAssetId);

        // Assert — recommendations come from explanation service
        assertEquals(1, response.assessment().recommendations().size());
        assertEquals("Custom recommendation from explanation service",
                response.assessment().recommendations().get(0));

        // Assert — scoring result values are still used for score/level/factors
        assertEquals(BigDecimal.valueOf(45.0), response.assessment().riskScore());
        assertEquals(com.powerassetintelligence.domain.model.RiskLevel.MEDIUM,
                response.assessment().riskLevel());
    }

    @Test
    void assessShouldPersistAllFieldsFromBothPhases() {
        RiskScoringResult scoreResult = new RiskScoringResult(
                BigDecimal.valueOf(88.0),
                com.powerassetintelligence.domain.model.RiskLevel.CRITICAL,
                List.of()
        );

        RiskExplanationResult explanationResult = new RiskExplanationResult(
                List.of("Create immediate maintenance work order"),
                "Critical risk explanation",
                "rules-2026.05"
        );

        Asset asset = createAsset(testAssetId);
        RiskFeatures features = createFeatures(testAssetId);
        RiskAssessmentSnapshot snapshot = RiskAssessmentSnapshot.from(features);

        when(assetService.getAsset(testAssetId)).thenReturn(asset);
        when(riskFeaturesExtractor.extractWithSnapshot(asset))
                .thenReturn(new RiskFeaturesExtractor.ExtractResult(features, snapshot));
        when(riskEngine.score(features)).thenReturn(scoreResult);
        when(riskExplanationService.explain(scoreResult, features)).thenReturn(explanationResult);
        when(riskAssessmentRepository.save(assessmentCaptor.capture())).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        service.assess(testAssetId);

        // Assert — captured assessment has correct values
        RiskAssessment captured = assessmentCaptor.getValue();
        assertEquals(BigDecimal.valueOf(88.0), captured.riskScore());
        assertEquals(com.powerassetintelligence.domain.model.RiskLevel.CRITICAL, captured.riskLevel());
        assertEquals(List.of(), captured.riskFactors());
        assertEquals(List.of("Create immediate maintenance work order"), captured.recommendations());
        assertEquals("rules-2026.05", captured.modelVersion());
        assertEquals("Critical risk explanation", captured.explanation());
        assertEquals(testAssetId, captured.assetId());
        assertNotNull(captured.assessedAt());
    }

    private Asset createAsset(UUID id) {
        Asset asset = new Asset(
                id,
                AssetType.TRANSFORMER,
                "Test Transformer",
                LocalDate.now(fixedClock).minusYears(10),
                AssetStatus.ACTIVE,
                "Substation A",
                "Test Manufacturer",
                AssetCriticality.HIGH,
                25,
                Map.of()
        );
        return asset;
    }

    private RiskFeatures createFeatures(UUID assetId) {
        return new RiskFeatures(
                assetId,
                AssetType.TRANSFORMER,
                AssetStatus.ACTIVE,
                AssetCriticality.HIGH,
                10,
                BigDecimal.valueOf(65.0),
                BigDecimal.valueOf(70.0),
                1,
                1L,
                BigDecimal.valueOf(64.0),
                BigDecimal.valueOf(70.0),
                BigDecimal.valueOf(68.0),
                BigDecimal.valueOf(75.0),
                2L,
                BigDecimal.valueOf(0.5),
                BigDecimal.valueOf(0.3)
        );
    }
}
