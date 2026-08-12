package com.powerassetintelligence.unit.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.powerassetintelligence.application.dto.RiskTrendPoint;
import com.powerassetintelligence.application.dto.RiskTrendResponse;
import com.powerassetintelligence.application.port.out.RiskAssessmentRepositoryPort;
import com.powerassetintelligence.application.service.AssetService;
import com.powerassetintelligence.application.service.ResourceNotFoundException;
import com.powerassetintelligence.application.service.RiskHistoryService;
import com.powerassetintelligence.core.ai.RiskFactor;
import com.powerassetintelligence.core.ai.TrendDirection;
import com.powerassetintelligence.domain.model.RiskAssessment;
import com.powerassetintelligence.domain.model.RiskLevel;
import com.powerassetintelligence.domain.model.Asset;
import com.powerassetintelligence.domain.model.AssetStatus;
import com.powerassetintelligence.domain.model.AssetType;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RiskHistoryServiceTest {

    private static final UUID ASSET_ID = UUID.randomUUID();
    private static final Instant NOW = Instant.now();

    @Mock
    private AssetService assetService;

    @Mock
    private RiskAssessmentRepositoryPort riskAssessmentRepositoryPort;

    @InjectMocks
    private RiskHistoryService service;

    private Asset asset;

    @BeforeEach
    void setUp() {
        asset = new Asset(
                ASSET_ID,
                AssetType.TRANSFORMER,
                "Test Asset",
                java.time.LocalDate.of(2020, 1, 1),
                AssetStatus.ACTIVE,
                "Moscow",
                "Siemens",
                com.powerassetintelligence.domain.model.AssetCriticality.HIGH,
                20,
                Map.of()
        );
    }

    // ---------------------------------------------------------------------
    // 1. Empty history
    // ---------------------------------------------------------------------
    @Test
    void getTrend_emptyHistory_shouldReturnStableWithNulls() {
        when(assetService.getAsset(ASSET_ID)).thenReturn(asset);
        when(riskAssessmentRepositoryPort.findByAssetIdOrderByAssessedAtAsc(ASSET_ID))
                .thenReturn(List.of());

        RiskTrendResponse response = service.getTrend(ASSET_ID, 10);

        assertEquals(ASSET_ID, response.assetId());
        assertEquals(List.of(), response.points());
        assertNull(response.currentScore());
        assertNull(response.previousScore());
        assertNull(response.totalChange());
        assertNull(response.averageChange());
        assertEquals(TrendDirection.STABLE, response.direction());
    }

    // ---------------------------------------------------------------------
    // 2. One assessment
    // ---------------------------------------------------------------------
    @Test
    void getTrend_oneAssessment_shouldReturnSinglePointWithNullChanges() {
        RiskAssessment assessment = createAssessment(1, ASSET_ID, 42, RiskLevel.MEDIUM);

        when(assetService.getAsset(ASSET_ID)).thenReturn(asset);
        when(riskAssessmentRepositoryPort.findByAssetIdOrderByAssessedAtAsc(ASSET_ID))
                .thenReturn(List.of(assessment));

        RiskTrendResponse response = service.getTrend(ASSET_ID, 10);

        assertEquals(1, response.points().size());
        RiskTrendPoint p0 = response.points().get(0);
        assertNull(p0.scoreChange());
        assertEquals(TrendDirection.STABLE, p0.trend());

        assertEquals(new BigDecimal("42"), response.currentScore());
        assertNull(response.previousScore());
        assertEquals(BigDecimal.ZERO, response.totalChange());
        assertNull(response.averageChange());
        assertEquals(TrendDirection.STABLE, response.direction());
    }

    // ---------------------------------------------------------------------
    // 3. Increasing trend
    // ---------------------------------------------------------------------
    @Test
    void getTrend_increasingScores_shouldReturnRisingDirection() {
        List<RiskAssessment> assessments = List.of(
                createAssessment(1, ASSET_ID, 42, RiskLevel.MEDIUM),
                createAssessment(2, ASSET_ID, 51, RiskLevel.MEDIUM),
                createAssessment(3, ASSET_ID, 63, RiskLevel.HIGH),
                createAssessment(4, ASSET_ID, 78, RiskLevel.HIGH),
                createAssessment(5, ASSET_ID, 84, RiskLevel.CRITICAL)
        );

        when(assetService.getAsset(ASSET_ID)).thenReturn(asset);
        when(riskAssessmentRepositoryPort.findByAssetIdOrderByAssessedAtAsc(ASSET_ID))
                .thenReturn(assessments);

        RiskTrendResponse response = service.getTrend(ASSET_ID, 10);

        assertEquals(TrendDirection.RISING, response.direction());

        List<RiskTrendPoint> points = response.points();
        assertEquals(TrendDirection.STABLE,  points.get(0).trend());
        assertEquals(TrendDirection.RISING,  points.get(1).trend());
        assertEquals(TrendDirection.RISING,  points.get(2).trend());
        assertEquals(TrendDirection.RISING,  points.get(3).trend());
        assertEquals(TrendDirection.RISING,  points.get(4).trend());
    }

    // ---------------------------------------------------------------------
    // 4. Decreasing trend
    // ---------------------------------------------------------------------
    @Test
    void getTrend_decreasingScores_shouldReturnFallingDirection() {
        List<RiskAssessment> assessments = List.of(
                createAssessment(1, ASSET_ID, 84, RiskLevel.CRITICAL),
                createAssessment(2, ASSET_ID, 78, RiskLevel.HIGH),
                createAssessment(3, ASSET_ID, 63, RiskLevel.HIGH),
                createAssessment(4, ASSET_ID, 51, RiskLevel.MEDIUM),
                createAssessment(5, ASSET_ID, 42, RiskLevel.MEDIUM)
        );

        when(assetService.getAsset(ASSET_ID)).thenReturn(asset);
        when(riskAssessmentRepositoryPort.findByAssetIdOrderByAssessedAtAsc(ASSET_ID))
                .thenReturn(assessments);

        RiskTrendResponse response = service.getTrend(ASSET_ID, 10);

        assertEquals(TrendDirection.FALLING, response.direction());
    }

    // ---------------------------------------------------------------------
    // 5. Stable trend (constant score)
    // ---------------------------------------------------------------------
    @Test
    void getTrend_constantScores_shouldReturnStableDirectionWithZeroChanges() {
        List<RiskAssessment> assessments = List.of(
                createAssessment(1, ASSET_ID, 50, RiskLevel.MEDIUM),
                createAssessment(2, ASSET_ID, 50, RiskLevel.MEDIUM),
                createAssessment(3, ASSET_ID, 50, RiskLevel.MEDIUM)
        );

        when(assetService.getAsset(ASSET_ID)).thenReturn(asset);
        when(riskAssessmentRepositoryPort.findByAssetIdOrderByAssessedAtAsc(ASSET_ID))
                .thenReturn(assessments);

        RiskTrendResponse response = service.getTrend(ASSET_ID, 10);

        assertEquals(TrendDirection.STABLE, response.direction());

        // all non-first scoreChanges should be 0
        for (int i = 1; i < response.points().size(); i++) {
            assertEquals(BigDecimal.ZERO, response.points().get(i).scoreChange());
        }
    }

    // ---------------------------------------------------------------------
    // 6. Score changes
    // ---------------------------------------------------------------------
    @Test
    void getTrend_scores42_51_63_shouldComputeCorrectChanges() {
        List<RiskAssessment> assessments = List.of(
                createAssessment(1, ASSET_ID, 42, RiskLevel.MEDIUM),
                createAssessment(2, ASSET_ID, 51, RiskLevel.MEDIUM),
                createAssessment(3, ASSET_ID, 63, RiskLevel.HIGH)
        );

        when(assetService.getAsset(ASSET_ID)).thenReturn(asset);
        when(riskAssessmentRepositoryPort.findByAssetIdOrderByAssessedAtAsc(ASSET_ID))
                .thenReturn(assessments);

        RiskTrendResponse response = service.getTrend(ASSET_ID, 10);

        List<RiskTrendPoint> points = response.points();

        assertNull(points.get(0).scoreChange());
        assertEquals(new BigDecimal("9"),  points.get(1).scoreChange());
        assertEquals(new BigDecimal("12"), points.get(2).scoreChange());
    }

    // ---------------------------------------------------------------------
    // 7. Total change
    // ---------------------------------------------------------------------
    @Test
    void getTrend_scores42_51_63_78_84_shouldHaveTotalChange42() {
        List<RiskAssessment> assessments = List.of(
                createAssessment(1, ASSET_ID, 42, RiskLevel.MEDIUM),
                createAssessment(2, ASSET_ID, 51, RiskLevel.MEDIUM),
                createAssessment(3, ASSET_ID, 63, RiskLevel.HIGH),
                createAssessment(4, ASSET_ID, 78, RiskLevel.HIGH),
                createAssessment(5, ASSET_ID, 84, RiskLevel.CRITICAL)
        );

        when(assetService.getAsset(ASSET_ID)).thenReturn(asset);
        when(riskAssessmentRepositoryPort.findByAssetIdOrderByAssessedAtAsc(ASSET_ID))
                .thenReturn(assessments);

        RiskTrendResponse response = service.getTrend(ASSET_ID, 10);

        assertEquals(new BigDecimal("42"), response.totalChange());
    }

    // ---------------------------------------------------------------------
    // 8. Average change
    // ---------------------------------------------------------------------
    @Test
    void getTrend_scores78_84_91_shouldHaveAverageChange6_50() {
        List<RiskAssessment> assessments = List.of(
                createAssessment(1, ASSET_ID, 78, RiskLevel.HIGH),
                createAssessment(2, ASSET_ID, 84, RiskLevel.CRITICAL),
                createAssessment(3, ASSET_ID, 91, RiskLevel.CRITICAL)
        );

        when(assetService.getAsset(ASSET_ID)).thenReturn(asset);
        when(riskAssessmentRepositoryPort.findByAssetIdOrderByAssessedAtAsc(ASSET_ID))
                .thenReturn(assessments);

        RiskTrendResponse response = service.getTrend(ASSET_ID, 10);

        assertEquals(new BigDecimal("6.50"), response.averageChange());
    }

    // ---------------------------------------------------------------------
    // 9. Limit — last N only
    // ---------------------------------------------------------------------
    @Test
    void getTrend_withLimit_shouldReturnLatestNAssessments() {
        List<RiskAssessment> assessments = List.of(
                createAssessment(1, ASSET_ID, 42, RiskLevel.MEDIUM),
                createAssessment(2, ASSET_ID, 51, RiskLevel.MEDIUM),
                createAssessment(3, ASSET_ID, 63, RiskLevel.HIGH),
                createAssessment(4, ASSET_ID, 78, RiskLevel.HIGH),
                createAssessment(5, ASSET_ID, 84, RiskLevel.CRITICAL),
                createAssessment(6, ASSET_ID, 91, RiskLevel.CRITICAL)
        );

        when(assetService.getAsset(ASSET_ID)).thenReturn(asset);
        when(riskAssessmentRepositoryPort.findByAssetIdOrderByAssessedAtAsc(ASSET_ID))
                .thenReturn(assessments);

        RiskTrendResponse response = service.getTrend(ASSET_ID, 3);

        // Only last 3: 78, 84, 91
        assertEquals(3, response.points().size());

        assertEquals(new BigDecimal("78"), response.points().get(0).riskScore());
        assertEquals(new BigDecimal("84"), response.points().get(1).riskScore());
        assertEquals(new BigDecimal("91"), response.points().get(2).riskScore());

        assertEquals(TrendDirection.RISING, response.direction());
        assertEquals(new BigDecimal("91"), response.currentScore());
        assertEquals(new BigDecimal("84"), response.previousScore());
        assertEquals(new BigDecimal("13"), response.totalChange());
    }

    // ---------------------------------------------------------------------
    // 10. Chronological order preserved
    // ---------------------------------------------------------------------
    @Test
    void getTrend_assessmentsInChronologicalOrder_shouldPreserveOrder() {
        Instant t1 = Instant.parse("2024-01-01T00:00:00Z");
        Instant t2 = Instant.parse("2024-01-02T00:00:00Z");
        Instant t3 = Instant.parse("2024-01-03T00:00:00Z");

        List<RiskAssessment> assessments = List.of(
                createAssessmentAt(1, ASSET_ID, t1, 10, RiskLevel.LOW),
                createAssessmentAt(2, ASSET_ID, t2, 20, RiskLevel.MEDIUM),
                createAssessmentAt(3, ASSET_ID, t3, 30, RiskLevel.HIGH)
        );

        when(assetService.getAsset(ASSET_ID)).thenReturn(asset);
        when(riskAssessmentRepositoryPort.findByAssetIdOrderByAssessedAtAsc(ASSET_ID))
                .thenReturn(assessments);

        RiskTrendResponse response = service.getTrend(ASSET_ID, 10);

        List<RiskTrendPoint> points = response.points();
        assertEquals(BigDecimal.valueOf(10), points.get(0).riskScore());
        assertEquals(BigDecimal.valueOf(20), points.get(1).riskScore());
        assertEquals(BigDecimal.valueOf(30), points.get(2).riskScore());

        // also verify assessedAt ordering
        assertEquals(t1, points.get(0).assessedAt());
        assertEquals(t2, points.get(1).assessedAt());
        assertEquals(t3, points.get(2).assessedAt());
    }

    // ---------------------------------------------------------------------
    // 11. Asset not found
    // ---------------------------------------------------------------------
    @Test
    void getTrend_assetNotFound_shouldThrowResourceNotFoundException() {
        when(assetService.getAsset(ASSET_ID))
                .thenThrow(new ResourceNotFoundException("Asset not found: " + ASSET_ID));

        org.junit.jupiter.api.Assertions.assertThrows(
                ResourceNotFoundException.class,
                () -> service.getTrend(ASSET_ID, 10)
        );

        verify(riskAssessmentRepositoryPort, never())
                .findByAssetIdOrderByAssessedAtAsc(any(UUID.class));
    }

    // ---------------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------------

    private static RiskAssessment createAssessment(int index, UUID assetId, int score, RiskLevel level) {
        Instant assessedAt = NOW.plusSeconds(index);
        UUID id = UUID.randomUUID();
        RiskFactor factor = RiskFactor.of(
                "AGE_FACTOR",
                "AGE",
                "Asset age factor",
                BigDecimal.valueOf(score),
                BigDecimal.valueOf(50),
                "SCORE"
        );
        return new RiskAssessment(
                id,
                assetId,
                assessedAt,
                BigDecimal.valueOf(score),
                level,
                List.of(factor),
                List.of("Check asset"),
                "v1.0",
                "Explanation for " + score
        );
    }

    private static RiskAssessment createAssessmentAt(int index, UUID assetId, Instant assessedAt, int score, RiskLevel level) {
        UUID id = UUID.randomUUID();
        RiskFactor factor = RiskFactor.of(
                "AGE_FACTOR",
                "AGE",
                "Asset age factor",
                BigDecimal.valueOf(score),
                BigDecimal.valueOf(50),
                "SCORE"
        );
        return new RiskAssessment(
                id,
                assetId,
                assessedAt,
                BigDecimal.valueOf(score),
                level,
                List.of(factor),
                List.of("Check asset"),
                "v1.0",
                "Explanation for " + score
        );
    }
}
