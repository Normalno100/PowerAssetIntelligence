package com.powerassetintelligence.unit.ai;

import com.powerassetintelligence.application.dto.RiskAssessmentComparisonResponse;
import com.powerassetintelligence.application.dto.RiskFactorChangeResponse;
import com.powerassetintelligence.application.port.out.PageRequest;
import com.powerassetintelligence.application.port.out.PageResult;
import com.powerassetintelligence.application.port.out.RiskAssessmentRepositoryPort;
import com.powerassetintelligence.application.service.RiskAssessmentComparisonService;
import com.powerassetintelligence.core.ai.RiskChangeDirection;
import com.powerassetintelligence.core.ai.RiskFactor;
import com.powerassetintelligence.domain.model.RiskAssessment;
import com.powerassetintelligence.domain.model.RiskLevel;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RiskAssessmentComparisonServiceTest {

    @Mock
    private RiskAssessmentRepositoryPort repository;

    @InjectMocks
    private RiskAssessmentComparisonService service;

    private UUID assetId;
    private Instant now;

    @BeforeEach
    void setUp() {
        assetId = UUID.randomUUID();
        now = Instant.now();
    }

    @Test
    void compareShouldReturnIncreasedWhenScoreGoesUp() {
        RiskAssessment previous = createAssessment(50, RiskLevel.MEDIUM, List.of());
        RiskAssessment current = createAssessment(70, RiskLevel.HIGH, List.of());

        setupRepository(previous, current);

        RiskAssessmentComparisonResponse result = service.compareLatest(assetId);

        assertEquals(BigDecimal.valueOf(70), result.currentScore());
        assertEquals(BigDecimal.valueOf(50), result.previousScore());
        assertEquals(BigDecimal.valueOf(20), result.scoreDelta());
        assertEquals(RiskChangeDirection.INCREASED, result.direction());
    }

    @Test
    void compareShouldReturnDecreasedWhenScoreGoesDown() {
        RiskAssessment previous = createAssessment(70, RiskLevel.HIGH, List.of());
        RiskAssessment current = createAssessment(50, RiskLevel.MEDIUM, List.of());

        setupRepository(previous, current);

        RiskAssessmentComparisonResponse result = service.compareLatest(assetId);

        assertEquals(BigDecimal.valueOf(50), result.currentScore());
        assertEquals(BigDecimal.valueOf(70), result.previousScore());
        assertEquals(BigDecimal.valueOf(-20), result.scoreDelta());
        assertEquals(RiskChangeDirection.DECREASED, result.direction());
    }

    @Test
    void compareShouldReturnUnchangedWhenScoreIsSame() {
        RiskAssessment previous = createAssessment(70, RiskLevel.HIGH, List.of());
        RiskAssessment current = createAssessment(70, RiskLevel.HIGH, List.of());

        setupRepository(previous, current);

        RiskAssessmentComparisonResponse result = service.compareLatest(assetId);

        assertEquals(BigDecimal.valueOf(70), result.currentScore());
        assertEquals(BigDecimal.valueOf(70), result.previousScore());
        assertEquals(BigDecimal.valueOf(0), result.scoreDelta());
        assertEquals(RiskChangeDirection.UNCHANGED, result.direction());
    }

    @Test
    void compareShouldReturnNoPreviousWhenNoAssessmentsExist() {
        when(repository.findFirstByAssetIdOrderByAssessedAtDesc(assetId)).thenReturn(Optional.empty());

        assertThrows(IllegalStateException.class, () -> service.compareLatest(assetId));
    }

    @Test
    void compareShouldReturnNoPreviousWhenOnlyOneAssessmentExists() {
        RiskAssessment current = createAssessment(50, RiskLevel.MEDIUM, List.of());
        when(repository.findFirstByAssetIdOrderByAssessedAtDesc(assetId)).thenReturn(Optional.of(current));
        when(repository.findByAssetId(any(), any())).thenReturn(
                new PageResult<>(List.of(current), 0, 20, 1, 1)
        );

        RiskAssessmentComparisonResponse result = service.compareLatest(assetId);

        assertEquals(BigDecimal.valueOf(50), result.currentScore());
        assertNull(result.previousScore());
        assertNull(result.scoreDelta());
        assertEquals(RiskChangeDirection.NO_PREVIOUS_ASSESSMENT, result.direction());
        assertTrue(result.factorChanges().isEmpty());
    }

    @Test
    void compareShouldDetectAppearedFactor() {
        RiskFactor prevTemp = RiskFactor.of("TEMP_HIGH", "TEMPERATURE", "High temp",
                BigDecimal.valueOf(82), BigDecimal.valueOf(80), "CELSIUS");
        RiskAssessment previous = createAssessment(60, RiskLevel.MEDIUM, List.of(prevTemp));

        RiskFactor currTemp = RiskFactor.of("TEMP_HIGH", "TEMPERATURE", "High temp",
                BigDecimal.valueOf(91), BigDecimal.valueOf(80), "CELSIUS");
        RiskFactor currLoad = RiskFactor.of("LOAD_HIGH", "LOAD", "High load",
                BigDecimal.valueOf(87), BigDecimal.valueOf(80), "PERCENT");
        RiskAssessment current = createAssessment(75, RiskLevel.HIGH, List.of(currTemp, currLoad));

        setupRepository(previous, current);

        RiskAssessmentComparisonResponse result = service.compareLatest(assetId);

        assertTrue(result.factorChanges().stream()
                .anyMatch(change -> "LOAD_HIGH".equals(change.code())
                        && change.previous() == null
                        && change.current() != null));
    }

    @Test
    void compareShouldDetectDisappearedFactor() {
        RiskFactor prevTemp = RiskFactor.of("TEMP_HIGH", "TEMPERATURE", "High temp",
                BigDecimal.valueOf(82), BigDecimal.valueOf(80), "CELSIUS");
        RiskFactor prevLoad = RiskFactor.of("LOAD_HIGH", "LOAD", "High load",
                BigDecimal.valueOf(87), BigDecimal.valueOf(80), "PERCENT");
        RiskAssessment previous = createAssessment(60, RiskLevel.MEDIUM, List.of(prevTemp, prevLoad));

        RiskFactor currTemp = RiskFactor.of("TEMP_HIGH", "TEMPERATURE", "High temp",
                BigDecimal.valueOf(91), BigDecimal.valueOf(80), "CELSIUS");
        RiskAssessment current = createAssessment(75, RiskLevel.HIGH, List.of(currTemp));

        setupRepository(previous, current);

        RiskAssessmentComparisonResponse result = service.compareLatest(assetId);

        assertTrue(result.factorChanges().stream()
                .anyMatch(change -> "LOAD_HIGH".equals(change.code())
                        && change.previous() != null
                        && change.current() == null));
    }

    @Test
    void compareShouldDetectFactorValueChanged() {
        RiskFactor prevFactor = RiskFactor.of("TEMP_HIGH", "TEMPERATURE", "High temp",
                BigDecimal.valueOf(82), BigDecimal.valueOf(80), "CELSIUS");
        RiskFactor currFactor = RiskFactor.of("TEMP_HIGH", "TEMPERATURE", "High temp",
                BigDecimal.valueOf(91), BigDecimal.valueOf(80), "CELSIUS");

        RiskAssessment previous = createAssessment(60, RiskLevel.MEDIUM, List.of(prevFactor));
        RiskAssessment current = createAssessment(75, RiskLevel.HIGH, List.of(currFactor));

        setupRepository(previous, current);

        RiskAssessmentComparisonResponse result = service.compareLatest(assetId);

        Optional<RiskFactorChangeResponse> tempChange = result.factorChanges().stream()
                .filter(c -> "TEMP_HIGH".equals(c.code()))
                .findFirst();

        assertTrue(tempChange.isPresent());
        assertEquals(BigDecimal.valueOf(91), tempChange.get().current().value());
        assertEquals(BigDecimal.valueOf(82), tempChange.get().previous().value());
        assertEquals(BigDecimal.valueOf(9), tempChange.get().valueDelta());
    }

    @Test
    void compareShouldDetectRiskLevelIncreased() {
        RiskAssessment previous = createAssessment(50, RiskLevel.MEDIUM, List.of());
        RiskAssessment current = createAssessment(70, RiskLevel.HIGH, List.of());

        setupRepository(previous, current);

        RiskAssessmentComparisonResponse result = service.compareLatest(assetId);

        assertEquals(RiskLevel.MEDIUM, result.previousLevel());
        assertEquals(RiskLevel.HIGH, result.currentLevel());
        assertEquals(RiskChangeDirection.INCREASED, result.direction());
    }

    @Test
    void compareShouldDetectRiskLevelDecreased() {
        RiskAssessment previous = createAssessment(80, RiskLevel.HIGH, List.of());
        RiskAssessment current = createAssessment(40, RiskLevel.LOW, List.of());

        setupRepository(previous, current);

        RiskAssessmentComparisonResponse result = service.compareLatest(assetId);

        assertEquals(RiskLevel.HIGH, result.previousLevel());
        assertEquals(RiskLevel.LOW, result.currentLevel());
        assertEquals(RiskChangeDirection.DECREASED, result.direction());
    }

    private void setupRepository(RiskAssessment previous, RiskAssessment current) {
        when(repository.findFirstByAssetIdOrderByAssessedAtDesc(assetId)).thenReturn(Optional.of(current));
        // DESC order: current first, previous second
        when(repository.findByAssetId(any(), any())).thenReturn(
                new PageResult<>(List.of(current, previous), 0, 20, 2, 1)
        );
    }

    private RiskAssessment createAssessment(int score, RiskLevel level, List<RiskFactor> factors) {
        return new RiskAssessment(
                UUID.randomUUID(),
                assetId,
                Instant.now(),
                BigDecimal.valueOf(score),
                level,
                factors,
                List.of(),
                "v1",
                "explanation"
        );
    }
}
