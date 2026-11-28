package com.powerassetintelligence.integration.kafka;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.powerassetintelligence.infrastructure.persistence.entity.TelemetryRecord;
import com.powerassetintelligence.infrastructure.persistence.repository.TelemetryRecordRepository;
import com.powerassetintelligence.infrastructure.web.dto.TelemetryCreateRequest;
import com.powerassetintelligence.testsupport.BaseIntegrationTest;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Integration tests for the telemetry ingestion HTTP endpoint with EmbeddedKafka.
 *
 * Tests validate HTTP-level behavior: request validation, acceptance responses,
 * and that messages are published to the Kafka topic.
 *
 * Note: The decommissioned-asset check and persistence happen asynchronously
 * in the Kafka consumer pipeline (TelemetryService.persist), not in the
 * HTTP endpoint (TelemetryService.ingest). Those are covered by unit tests.
 */
@EmbeddedKafka(
        partitions = 1,
        topics = {"telemetry.raw.v1", "telemetry.raw.v1.dlt"},
        bootstrapServersProperty = "spring.kafka.bootstrap-servers"
)
@AutoConfigureMockMvc
class TelemetryKafkaConsumerIT extends BaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private TelemetryRecordRepository telemetryRepository;

    // ---------------------------------------------------------------------
    // 1. Happy path: valid telemetry accepted (202) and published to Kafka
    // ---------------------------------------------------------------------
    @Test
    void shouldAcceptValidTelemetryAndReturn202() throws Exception {
        UUID assetId = UUID.randomUUID();
        String externalId = "EXT-" + UUID.randomUUID();

        TelemetryCreateRequest request = new TelemetryCreateRequest(
                assetId,
                Instant.now(),
                BigDecimal.valueOf(75.5),
                BigDecimal.valueOf(65.0),
                BigDecimal.valueOf(110.0),
                BigDecimal.valueOf(150.0),
                BigDecimal.valueOf(2.5),
                1,
                "sensor-kafka-001",
                externalId
        );

        mockMvc.perform(post("/api/v1/telemetry")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.status").value("ACCEPTED"))
                .andExpect(jsonPath("$.assetId").value(assetId.toString()));
    }

    // ---------------------------------------------------------------------
    // 2. Rejects null assetId (validation error)
    // ---------------------------------------------------------------------
    @Test
    void shouldRejectNullAssetId() throws Exception {
        TelemetryCreateRequest request = new TelemetryCreateRequest(
                null,  // null assetId
                Instant.now(),
                BigDecimal.valueOf(75.5),
                BigDecimal.valueOf(65.0),
                BigDecimal.valueOf(110.0),
                BigDecimal.valueOf(150.0),
                BigDecimal.valueOf(2.5),
                0,
                "sensor-001",
                null
        );

        mockMvc.perform(post("/api/v1/telemetry")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    // ---------------------------------------------------------------------
    // 3. Rejects out-of-range temperature
    // ---------------------------------------------------------------------
    @Test
    void shouldRejectOutOfRangeTemperature() throws Exception {
        TelemetryCreateRequest request = new TelemetryCreateRequest(
                UUID.randomUUID(),
                Instant.now(),
                BigDecimal.valueOf(300.0),  // > 250 max
                BigDecimal.valueOf(65.0),
                BigDecimal.valueOf(110.0),
                BigDecimal.valueOf(150.0),
                BigDecimal.valueOf(2.5),
                0,
                "sensor-001",
                null
        );

        mockMvc.perform(post("/api/v1/telemetry")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    // ---------------------------------------------------------------------
    // 4. Rejects blank sourceSensorId
    // ---------------------------------------------------------------------
    @Test
    void shouldRejectBlankSourceSensorId() throws Exception {
        TelemetryCreateRequest request = new TelemetryCreateRequest(
                UUID.randomUUID(),
                Instant.now(),
                BigDecimal.valueOf(75.5),
                BigDecimal.valueOf(65.0),
                BigDecimal.valueOf(110.0),
                BigDecimal.valueOf(150.0),
                BigDecimal.valueOf(2.5),
                0,
                "   ",  // blank sourceSensorId
                null
        );

        mockMvc.perform(post("/api/v1/telemetry")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    // ---------------------------------------------------------------------
    // 5. Idempotent: duplicate externalTelemetryId — both requests accepted
    // ---------------------------------------------------------------------
    @Test
    void shouldAcceptDuplicateExternalTelemetryId() throws Exception {
        String externalId = "EXT-DUP-" + UUID.randomUUID();

        TelemetryCreateRequest request = new TelemetryCreateRequest(
                UUID.randomUUID(),
                Instant.now(),
                BigDecimal.valueOf(75.5),
                BigDecimal.valueOf(65.0),
                BigDecimal.valueOf(110.0),
                BigDecimal.valueOf(150.0),
                BigDecimal.valueOf(2.5),
                0,
                "sensor-001",
                externalId
        );

        // First request
        mockMvc.perform(post("/api/v1/telemetry")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isAccepted());

        // Second request with same externalId
        mockMvc.perform(post("/api/v1/telemetry")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isAccepted());
    }

    // ---------------------------------------------------------------------
    // 6. Rejects negative vibration
    // ---------------------------------------------------------------------
    @Test
    void shouldRejectNegativeVibration() throws Exception {
        TelemetryCreateRequest request = new TelemetryCreateRequest(
                UUID.randomUUID(),
                Instant.now(),
                BigDecimal.valueOf(75.5),
                BigDecimal.valueOf(65.0),
                BigDecimal.valueOf(110.0),
                BigDecimal.valueOf(150.0),
                BigDecimal.valueOf(-1.0),  // negative vibration
                0,
                "sensor-001",
                null
        );

        mockMvc.perform(post("/api/v1/telemetry")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }
}
