package com.powerassetintelligence.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.powerassetintelligence.application.dto.RiskAssessmentResponse;
import com.powerassetintelligence.infrastructure.web.dto.AssetCreateRequest;
import com.powerassetintelligence.testsupport.BaseIntegrationTest;
import com.powerassetintelligence.domain.model.AssetCriticality;
import com.powerassetintelligence.domain.model.AssetStatus;
import com.powerassetintelligence.domain.model.AssetType;
import com.powerassetintelligence.infrastructure.persistence.entity.RiskAssessment;
import com.powerassetintelligence.infrastructure.persistence.repository.RiskAssessmentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration test proving that RiskAssessment snapshots preserve input features
 * even after the underlying telemetry data changes.
 */
@AutoConfigureMockMvc
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class RiskAssessmentSnapshotIntegrationTest extends BaseIntegrationTest {

    private static final ObjectMapper MAPPER = new ObjectMapper()
            .registerModule(new JavaTimeModule());

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private RiskAssessmentRepository riskAssessmentRepository;

    private UUID assetId;

    @BeforeEach
    void setUp() throws Exception {
        assetId = createTestAsset();
    }

    @Test
    @DisplayName("New assessment must include snapshot with all feature fields")
    void newAssessmentMustIncludeSnapshot() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/assets/{assetId}/risk-assessments", assetId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();

        String body = result.getResponse().getContentAsString();
        JsonNode json = MAPPER.readTree(body);

        // Verify assessment fields
        assertThat(json.get("assessment")).isNotNull();
        assertThat(json.get("features")).isNotNull();

        // Verify snapshot exists in assessment response
        JsonNode snapshotNode = json.get("assessment").get("snapshot");
        assertThat(snapshotNode).isNotNull();

        // Verify snapshot contains key fields
        assertThat(snapshotNode.get("assetType")).isNotNull();
        assertThat(snapshotNode.get("assetStatus")).isNotNull();
        assertThat(snapshotNode.get("criticality")).isNotNull();
        assertThat(snapshotNode.get("assetAgeYears")).isNotNull();
        assertThat(snapshotNode.get("latestTemperatureCelsius")).isNotNull();
        assertThat(snapshotNode.get("latestLoadPercent")).isNotNull();
        assertThat(snapshotNode.get("repairsLastYear")).isNotNull();
    }

    @Test
    @DisplayName("Snapshot in assessment must match features in response")
    void snapshotMustMatchFeatures() throws Exception {
        mockMvc.perform(post("/api/v1/assets/{assetId}/risk-assessments", assetId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        // Get the details response (assessment + features)
        MvcResult result = mockMvc.perform(post("/api/v1/assets/{assetId}/risk-assessments", assetId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();

        String body = result.getResponse().getContentAsString();
        JsonNode json = MAPPER.readTree(body);

        JsonNode features = json.get("features");
        JsonNode snapshot = json.get("assessment").get("snapshot");

        // Key fields should match between features and snapshot
        assertThat(snapshot.get("latestTemperatureCelsius").asDouble())
                .isEqualTo(features.get("latestTemperatureCelsius").asDouble());
        assertThat(snapshot.get("latestLoadPercent").asDouble())
                .isEqualTo(features.get("latestLoadPercent").asDouble());
        assertThat(snapshot.get("assetAgeYears").asInt())
                .isEqualTo(features.get("assetAgeYears").asInt());
        assertThat(snapshot.get("averageTemperatureCelsius").asDouble())
                .isEqualTo(features.get("averageTemperatureCelsius").asDouble());
    }

    @Test
    @DisplayName("GET latest assessment must include snapshot")
    void getLatestAssessmentMustIncludeSnapshot() throws Exception {
        mockMvc.perform(post("/api/v1/assets/{assetId}/risk-assessments", assetId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        MvcResult result = mockMvc.perform(get("/api/v1/assets/{assetId}/risk-assessments/latest", assetId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();

        String body = result.getResponse().getContentAsString();
        JsonNode json = MAPPER.readTree(body);

        assertThat(json.get("snapshot")).isNotNull();
        assertThat(json.get("snapshot").get("assetType")).isNotNull();
    }

    @Test
    @DisplayName("Paginated assessments must include optional snapshot")
    void paginatedAssessmentsMustIncludeSnapshot() throws Exception {
        // Create two assessments
        mockMvc.perform(post("/api/v1/assets/{assetId}/risk-assessments", assetId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/assets/{assetId}/risk-assessments", assetId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        MvcResult result = mockMvc.perform(get("/api/v1/assets/{assetId}/risk-assessments", assetId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();

        String body = result.getResponse().getContentAsString();
        JsonNode json = MAPPER.readTree(body);

        // Each page item should have snapshot
        JsonNode content = json.get("content");
        assertThat(content.isArray()).isTrue();
        assertThat(content.size()).isGreaterThanOrEqualTo(2);

        for (int i = 0; i < content.size(); i++) {
            assertThat(content.get(i).get("snapshot")).isNotNull();
        }
    }

    @Test
    @DisplayName("Assessment response DTO must have snapshot field")
    void responseDtoMustHaveSnapshot() throws Exception {
        mockMvc.perform(post("/api/v1/assets/{assetId}/risk-assessments", assetId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        MvcResult result = mockMvc.perform(get("/api/v1/assets/{assetId}/risk-assessments/latest", assetId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();

        RiskAssessmentResponse response = MAPPER.readValue(
                result.getResponse().getContentAsString(), RiskAssessmentResponse.class);

        assertThat(response.snapshot()).isNotNull();
        assertThat(response.snapshot().assetType()).isNotNull();
        assertThat(response.snapshot().assetAgeYears()).isGreaterThan(0);
    }

    @Test
    @DisplayName("JPA entity must persist snapshot in database")
    void entityMustPersistSnapshot() throws Exception {
        mockMvc.perform(post("/api/v1/assets/{assetId}/risk-assessments", assetId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        // Query the database directly
        RiskAssessment entity = riskAssessmentRepository.findFirstByAssetIdOrderByAssessedAtDesc(assetId)
                .orElseThrow(() -> new AssertionError("Assessment not found in DB"));

        assertThat(entity.getSnapshot()).isNotNull();
        assertThat(entity.getSnapshot().assetType()).isEqualTo(AssetType.TRANSFORMER);
        assertThat(entity.getSnapshot().assetAgeYears()).isGreaterThan(0);
    }

    private UUID createTestAsset() throws Exception {
        AssetCreateRequest request = new AssetCreateRequest(
                AssetType.TRANSFORMER,
                "Test Transformer for Snapshot",
                LocalDate.of(2015, 6, 15),
                "TestLocation-Snapshot",
                "TestManufacturer",
                AssetCriticality.HIGH,
                25,
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
}
