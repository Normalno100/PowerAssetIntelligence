package com.powerassetintelligence.integration.api;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.powerassetintelligence.application.dto.RiskTrendPoint;
import com.powerassetintelligence.application.dto.RiskTrendResponse;
import com.powerassetintelligence.application.service.RiskHistoryService;
import com.powerassetintelligence.core.ai.TrendDirection;
import com.powerassetintelligence.domain.model.RiskLevel;
import com.powerassetintelligence.testsupport.BaseIntegrationTest;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@AutoConfigureMockMvc
class RiskAnalysisControllerMockMvcIT extends BaseIntegrationTest {

    private static final UUID VALID_ASSET_ID = UUID.randomUUID();

    @MockBean
    private RiskHistoryService riskHistoryService;

    @Autowired
    private MockMvc mockMvc;

    private RiskTrendResponse sampleResponse;

    @BeforeEach
    void setUp() {
        sampleResponse = new RiskTrendResponse(
                VALID_ASSET_ID,
                List.of(
                        new RiskTrendPoint(
                                UUID.randomUUID(), VALID_ASSET_ID, Instant.parse("2024-01-01T00:00:00Z"),
                                new BigDecimal("42"), RiskLevel.MEDIUM, null, TrendDirection.STABLE),
                        new RiskTrendPoint(
                                UUID.randomUUID(), VALID_ASSET_ID, Instant.parse("2024-01-02T00:00:00Z"),
                                new BigDecimal("51"), RiskLevel.MEDIUM, new BigDecimal("9"), TrendDirection.RISING),
                        new RiskTrendPoint(
                                UUID.randomUUID(), VALID_ASSET_ID, Instant.parse("2024-01-03T00:00:00Z"),
                                new BigDecimal("63"), RiskLevel.HIGH, new BigDecimal("12"), TrendDirection.RISING)
                ),
                new BigDecimal("63"),
                new BigDecimal("51"),
                new BigDecimal("12"),
                new BigDecimal("10.50"),
                TrendDirection.RISING
        );
    }

    // ---------------------------------------------------------------------
    // 1. Valid assetId with default limit
    // ---------------------------------------------------------------------
    @Test
    void getRiskTrend_validAssetDefaultLimit_shouldReturn200() throws Exception {
        when(riskHistoryService.getTrend(eq(VALID_ASSET_ID), eq(20))).thenReturn(sampleResponse);

        mockMvc.perform(get("/api/v1/assets/{assetId}/risk-assessments/trend", VALID_ASSET_ID)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.assetId").value(VALID_ASSET_ID.toString()))
                .andExpect(jsonPath("$.points").isArray())
                .andExpect(jsonPath("$.points.length()").value(3))
                .andExpect(jsonPath("$.currentScore").value("63"))
                .andExpect(jsonPath("$.previousScore").value("51"))
                .andExpect(jsonPath("$.totalChange").value("12"))
                .andExpect(jsonPath("$.averageChange").value(10.5))
                .andExpect(jsonPath("$.direction").value("RISING"));
    }

    // ---------------------------------------------------------------------
    // 2. Valid assetId with explicit limit
    // ---------------------------------------------------------------------
    @Test
    void getRiskTrend_validAssetWithCustomLimit_shouldReturn200() throws Exception {
        when(riskHistoryService.getTrend(eq(VALID_ASSET_ID), eq(5))).thenReturn(sampleResponse);

        mockMvc.perform(get("/api/v1/assets/{assetId}/risk-assessments/trend", VALID_ASSET_ID)
                        .param("limit", "5")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.assetId").value(VALID_ASSET_ID.toString()));
    }

    // ---------------------------------------------------------------------
    // 3. Invalid limit — too low (0)
    // ---------------------------------------------------------------------
    @Test
    void getRiskTrend_limitZero_shouldReturn400() throws Exception {
        mockMvc.perform(get("/api/v1/assets/{assetId}/risk-assessments/trend", VALID_ASSET_ID)
                        .param("limit", "0")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Invalid query parameter"))
                .andExpect(jsonPath("$.detail").exists());
    }

    // ---------------------------------------------------------------------
    // 4. Invalid limit — too high (101)
    // ---------------------------------------------------------------------
    @Test
    void getRiskTrend_limitTooHigh_shouldReturn400() throws Exception {
        mockMvc.perform(get("/api/v1/assets/{assetId}/risk-assessments/trend", VALID_ASSET_ID)
                        .param("limit", "101")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Invalid query parameter"))
                .andExpect(jsonPath("$.detail").exists());
    }

    // ---------------------------------------------------------------------
    // 5. Invalid limit — non-numeric value
    // ---------------------------------------------------------------------
    @Test
    void getRiskTrend_limitNonNumeric_shouldReturn400() throws Exception {
        mockMvc.perform(get("/api/v1/assets/{assetId}/risk-assessments/trend", VALID_ASSET_ID)
                        .param("limit", "abc")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    // ---------------------------------------------------------------------
    // 6. Boundary — limit = 1 (minimum allowed)
    // ---------------------------------------------------------------------
    @Test
    void getRiskTrend_limitOne_shouldReturn200() throws Exception {
        RiskTrendResponse singlePoint = new RiskTrendResponse(
                VALID_ASSET_ID,
                List.of(
                        new RiskTrendPoint(
                                UUID.randomUUID(), VALID_ASSET_ID, Instant.parse("2024-01-01T00:00:00Z"),
                                new BigDecimal("42"), RiskLevel.MEDIUM, null, TrendDirection.STABLE)
                ),
                new BigDecimal("42"),
                null,
                BigDecimal.ZERO,
                null,
                TrendDirection.STABLE
        );
        when(riskHistoryService.getTrend(eq(VALID_ASSET_ID), eq(1))).thenReturn(singlePoint);

        mockMvc.perform(get("/api/v1/assets/{assetId}/risk-assessments/trend", VALID_ASSET_ID)
                        .param("limit", "1")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.points.length()").value(1));
    }

    // ---------------------------------------------------------------------
    // 7. Boundary — limit = 100 (maximum allowed)
    // ---------------------------------------------------------------------
    @Test
    void getRiskTrend_limitOneHundred_shouldReturn200() throws Exception {
        when(riskHistoryService.getTrend(eq(VALID_ASSET_ID), eq(100))).thenReturn(sampleResponse);

        mockMvc.perform(get("/api/v1/assets/{assetId}/risk-assessments/trend", VALID_ASSET_ID)
                        .param("limit", "100")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }
}
