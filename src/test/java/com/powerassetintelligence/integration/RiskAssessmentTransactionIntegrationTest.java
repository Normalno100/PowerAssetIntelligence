package com.powerassetintelligence.integration;

import com.powerassetintelligence.core.ai.CoreRiskScoringPort;
import com.powerassetintelligence.core.ai.RiskExplanationService;
import com.powerassetintelligence.infrastructure.persistence.entity.AssetEntity;
import com.powerassetintelligence.infrastructure.persistence.entity.RiskAssessment;
import com.powerassetintelligence.infrastructure.persistence.repository.AssetRepository;
import com.powerassetintelligence.infrastructure.persistence.repository.RiskAssessmentRepository;
import com.powerassetintelligence.testsupport.BaseIntegrationTest;
import com.powerassetintelligence.domain.model.AssetCriticality;
import com.powerassetintelligence.domain.model.AssetStatus;
import com.powerassetintelligence.domain.model.AssetType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests for transactional behavior of {@code RiskAnalysisService.assess()}.
 *
 * <p>Proves that:
 * <ul>
 *   <li>a successful assessment is persisted to the database</li>
 *   <li>if scoring fails mid-pipeline, the transaction rolls back and no assessment is saved</li>
 *   <li>if explanation fails mid-pipeline, the transaction rolls back</li>
 * </ul>
 */
@AutoConfigureMockMvc
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class RiskAssessmentTransactionIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private RiskAssessmentRepository riskAssessmentRepository;

    @Autowired
    private AssetRepository assetRepository;

    @MockBean
    private CoreRiskScoringPort coreRiskScoringPort;

    @MockBean
    private RiskExplanationService riskExplanationService;

    private UUID assetId;

    @BeforeEach
    void setUp() throws Exception {
        assetId = createTestAsset();
    }

    // ---------------------------------------------------------------------
    // 1. Successful assessment persists
    // ---------------------------------------------------------------------

    @Test
    @DisplayName("Successful assess must persist assessment to database")
    void successfulAssessMustPersistAssessment() throws Exception {
        // Arrange — stub both phases to succeed
        when(coreRiskScoringPort.score(any()))
                .thenReturn(new com.powerassetintelligence.application.dto.RiskScoringResult(
                        BigDecimal.valueOf(55.0),
                        com.powerassetintelligence.domain.model.RiskLevel.MEDIUM,
                        List.of()
                ));
        when(riskExplanationService.explain(any(), any()))
                .thenReturn(new com.powerassetintelligence.core.ai.RiskExplanationResult(
                        List.of("Monitor closely"),
                        "Normal assessment",
                        "rules-2026.05"
                ));

        // Act — create assessment via API
        mockMvc.perform(post("/api/v1/assets/{assetId}/risk-assessments", assetId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        // Assert — assessment is persisted in DB
        assertThat(riskAssessmentRepository.count()).isEqualTo(1);
        RiskAssessment entity = riskAssessmentRepository
                .findFirstByAssetIdOrderByAssessedAtDesc(assetId)
                .orElseThrow(() -> new AssertionError("Assessment not found in DB"));
        assertThat(entity.getRiskScore()).isEqualByComparingTo(BigDecimal.valueOf(55.0));
        assertThat(entity.getRiskLevel()).isEqualTo(com.powerassetintelligence.domain.model.RiskLevel.MEDIUM);
    }

    // ---------------------------------------------------------------------
    // 2. Rollback on scoring failure
    // ---------------------------------------------------------------------

    @Test
    @DisplayName("Scoring failure must roll back — no assessment saved")
    void scoringFailureMustRollback() throws Exception {
        // Arrange — first call succeeds, second call throws
        when(coreRiskScoringPort.score(any()))
                .thenReturn(new com.powerassetintelligence.application.dto.RiskScoringResult(
                        BigDecimal.valueOf(55.0),
                        com.powerassetintelligence.domain.model.RiskLevel.MEDIUM,
                        List.of()
                ))
                .thenThrow(new RuntimeException("ML scoring service unavailable"));
        when(riskExplanationService.explain(any(), any()))
                .thenReturn(new com.powerassetintelligence.core.ai.RiskExplanationResult(
                        List.of("Monitor closely"),
                        "Normal assessment",
                        "rules-2026.05"
                ));

        // Act — first assessment succeeds
        mockMvc.perform(post("/api/v1/assets/{assetId}/risk-assessments", assetId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        // Verify first assessment persisted
        assertThat(riskAssessmentRepository.count()).isEqualTo(1);

        // Second assessment fails
        mockMvc.perform(post("/api/v1/assets/{assetId}/risk-assessments", assetId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().is5xxServerError());

        // Assert — count unchanged (rollback worked)
        assertThat(riskAssessmentRepository.count()).isEqualTo(1);
    }

    // ---------------------------------------------------------------------
    // 3. Rollback on explanation failure
    // ---------------------------------------------------------------------

    @Test
    @DisplayName("Explanation failure must roll back — no assessment saved")
    void explanationFailureMustRollback() throws Exception {
        // Arrange — scoring succeeds, explanation throws on 2nd call
        when(coreRiskScoringPort.score(any()))
                .thenReturn(new com.powerassetintelligence.application.dto.RiskScoringResult(
                        BigDecimal.valueOf(72.0),
                        com.powerassetintelligence.domain.model.RiskLevel.HIGH,
                        List.of()
                ));
        when(riskExplanationService.explain(any(), any()))
                .thenReturn(new com.powerassetintelligence.core.ai.RiskExplanationResult(
                        List.of("Prioritize maintenance"),
                        "High risk explanation",
                        "rules-2026.05"
                ))
                .thenThrow(new RuntimeException("Explanation engine error"));

        // Act — first assessment succeeds
        mockMvc.perform(post("/api/v1/assets/{assetId}/risk-assessments", assetId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        // Verify first assessment persisted
        assertThat(riskAssessmentRepository.count()).isEqualTo(1);

        // Second assessment fails
        mockMvc.perform(post("/api/v1/assets/{assetId}/risk-assessments", assetId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().is5xxServerError());

        // Assert — count unchanged (rollback worked)
        assertThat(riskAssessmentRepository.count()).isEqualTo(1);
    }

    // ---------------------------------------------------------------------
    // Helper
    // ---------------------------------------------------------------------

    private UUID createTestAsset() {
        AssetEntity asset = new AssetEntity(
                UUID.randomUUID(),
                AssetType.TRANSFORMER,
                "Test Transformer for Transaction",
                LocalDate.of(2015, 6, 15),
                AssetStatus.ACTIVE,
                "TestLocation-Transaction",
                "TestManufacturer",
                AssetCriticality.HIGH,
                25,
                Map.of()
        );

        AssetEntity saved = assetRepository.saveAndFlush(asset);
        return saved.getId();
    }
}
