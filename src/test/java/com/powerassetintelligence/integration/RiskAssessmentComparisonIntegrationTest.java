package com.powerassetintelligence.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.powerassetintelligence.application.dto.RiskAssessmentComparisonResponse;
import com.powerassetintelligence.application.dto.RiskAssessmentResponse;
import com.powerassetintelligence.application.service.RiskAssessmentComparisonService;
import com.powerassetintelligence.core.ai.RiskChangeDirection;
import com.powerassetintelligence.infrastructure.web.dto.AssetCreateRequest;
import com.powerassetintelligence.testsupport.BaseIntegrationTest;
import com.powerassetintelligence.domain.model.AssetCriticality;
import com.powerassetintelligence.domain.model.AssetStatus;
import com.powerassetintelligence.domain.model.AssetType;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@AutoConfigureMockMvc
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class RiskAssessmentComparisonIntegrationTest extends BaseIntegrationTest {

    private static final ObjectMapper MAPPER = new ObjectMapper()
            .registerModule(new JavaTimeModule());

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private RiskAssessmentComparisonService comparisonService;

    @Test
    void shouldCompareTwoAssessmentsCorrectly() throws Exception {
        UUID assetId = createTestAsset();

        mockMvc.perform(post("/api/v1/assets/{assetId}/risk-assessments", assetId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        RiskAssessmentResponse first = getLatestAssessment(assetId);
        assertThat(first.riskLevel()).isNotNull();
        BigDecimal firstScore = first.riskScore();

        mockMvc.perform(post("/api/v1/assets/{assetId}/risk-assessments", assetId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        RiskAssessmentResponse second = getLatestAssessment(assetId);
        assertThat(second.riskScore()).isNotNull();

        MvcResult result = mockMvc.perform(get("/api/v1/assets/{assetId}/risk-assessments/latest/comparison", assetId))
                .andExpect(status().isOk())
                .andReturn();

        String responseBody = result.getResponse().getContentAsString();
        System.out.println("Comparison response: " + responseBody);

        RiskAssessmentComparisonResponse comparison = MAPPER.readValue(responseBody,
                RiskAssessmentComparisonResponse.class);

        assertThat(comparison.currentScore()).isNotNull();
        assertThat(comparison.currentLevel()).isEqualTo(second.riskLevel());
        assertThat(comparison.direction()).isNotNull();
    }

    @Test
    void shouldReturnNoPreviousWhenFirstAssessment() throws Exception {
        UUID assetId = createTestAsset();

        mockMvc.perform(post("/api/v1/assets/{assetId}/risk-assessments", assetId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        MvcResult result = mockMvc.perform(get("/api/v1/assets/{assetId}/risk-assessments/latest/comparison", assetId))
                .andExpect(status().isOk())
                .andReturn();

        String responseBody = result.getResponse().getContentAsString();
        System.out.println("Single assessment comparison: " + responseBody);

        RiskAssessmentComparisonResponse comparison = MAPPER.readValue(responseBody,
                RiskAssessmentComparisonResponse.class);

        assertThat(comparison.direction()).isEqualTo(RiskChangeDirection.NO_PREVIOUS_ASSESSMENT);
        assertThat(comparison.previousScore()).isNull();
        assertThat(comparison.scoreDelta()).isNull();
    }

    private UUID createTestAsset() throws Exception {
        AssetCreateRequest request = new AssetCreateRequest(
                AssetType.TRANSFORMER,
                "Test Transformer",
                LocalDate.of(2020, 1, 1),
                "TestLocation",
                "TestManufacturer",
                AssetCriticality.MEDIUM,
                30,
                Map.of()
        );

        MvcResult result = mockMvc.perform(post("/api/v1/assets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(MAPPER.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();

        com.powerassetintelligence.application.dto.AssetResponse response =
                MAPPER.readValue(result.getResponse().getContentAsString(),
                        com.powerassetintelligence.application.dto.AssetResponse.class);

        return response.id();
    }

    private RiskAssessmentResponse getLatestAssessment(UUID assetId) throws Exception {
        MvcResult result = mockMvc.perform(get("/api/v1/assets/{assetId}/risk-assessments/latest", assetId))
                .andExpect(status().isOk())
                .andReturn();

        return MAPPER.readValue(result.getResponse().getContentAsString(), RiskAssessmentResponse.class);
    }
}
