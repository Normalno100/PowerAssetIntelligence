package com.powerassetintelligence.integration.repository;

import static org.junit.jupiter.api.Assertions.*;
import static org.assertj.core.api.Assertions.assertThat;

import com.powerassetintelligence.domain.model.AssetStatus;
import com.powerassetintelligence.domain.model.AssetType;
import com.powerassetintelligence.domain.model.AssetCriticality;
import com.powerassetintelligence.infrastructure.persistence.entity.AssetEntity;
import com.powerassetintelligence.infrastructure.persistence.entity.TelemetryRecord;
import com.powerassetintelligence.infrastructure.persistence.repository.AssetRepository;
import com.powerassetintelligence.infrastructure.persistence.repository.TelemetryRecordRepository;
import com.powerassetintelligence.testsupport.BaseIntegrationTest;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

/**
 * Integration tests for TelemetryRecordRepository with H2 PostgreSQL-compatible mode.
 */
class TelemetryRepositoryDbIT extends BaseIntegrationTest {

    @Autowired
    private AssetRepository assetRepository;

    @Autowired
    private TelemetryRecordRepository telemetryRepository;

    private UUID assetId;
    private AssetEntity asset;

    @BeforeEach
    void setUp() {
        assetId = UUID.randomUUID();
        asset = new AssetEntity(
                assetId,
                AssetType.TRANSFORMER,
                "TX-DB-IT",
                LocalDate.of(2020, 1, 1),
                AssetStatus.ACTIVE,
                "Substation-Test",
                "TestManufacturer",
                AssetCriticality.HIGH,
                30,
                Collections.emptyMap()
        );
        assetRepository.saveAndFlush(asset);
    }

    // ---------------------------------------------------------------------
    // 1. saveAndFindByExternalTelemetryId — happy path
    // ---------------------------------------------------------------------
    @Test
    void saveAndFindByExternalTelemetryId_shouldReturnRecord() {
        String externalId = "EXT-789";
        TelemetryRecord record = new TelemetryRecord(
                UUID.randomUUID(),
                asset,
                Instant.parse("2024-06-01T10:00:00Z"),
                BigDecimal.valueOf(75.5),
                BigDecimal.valueOf(65.0),
                BigDecimal.valueOf(110.0),
                BigDecimal.valueOf(150.0),
                BigDecimal.valueOf(2.5),
                1,
                "sensor-001",
                externalId
        );

        TelemetryRecord saved = telemetryRepository.saveAndFlush(record);

        Optional<TelemetryRecord> found = telemetryRepository.findByExternalTelemetryId(externalId);
        assertTrue(found.isPresent());
        assertEquals(saved.getId(), found.get().getId());
        assertEquals(externalId, found.get().getExternalTelemetryId());
    }

    // ---------------------------------------------------------------------
    // 2. findByAssetId — returns paginated results
    // ---------------------------------------------------------------------
    @Test
    void findByAssetId_shouldReturnPaginatedResults() {
        Instant baseTime = Instant.parse("2024-06-01T10:00:00Z");
        for (int i = 0; i < 5; i++) {
            TelemetryRecord record = new TelemetryRecord(
                    UUID.randomUUID(),
                    asset,
                    baseTime.plusSeconds(i),
                    BigDecimal.valueOf(70.0 + i),
                    BigDecimal.valueOf(60.0),
                    BigDecimal.valueOf(110.0),
                    BigDecimal.valueOf(140.0),
                    BigDecimal.valueOf(1.5),
                    0,
                    "sensor-" + i,
                    null
            );
            telemetryRepository.saveAndFlush(record);
        }

        Page<TelemetryRecord> page = telemetryRepository.findByAssetId(
                assetId, PageRequest.of(0, 3, Sort.by(Sort.Direction.DESC, "timestamp")));

        assertEquals(3, page.getContent().size());
        assertEquals(5, page.getTotalElements());
        assertEquals(2, page.getTotalPages());
    }

    // ---------------------------------------------------------------------
    // 3. findFirstByAssetIdOrderByTimestampDesc — returns latest
    // ---------------------------------------------------------------------
    @Test
    void findFirstByAssetIdOrderByTimestampDesc_shouldReturnLatest() {
        Instant older = Instant.parse("2024-06-01T10:00:00Z");
        Instant newer = Instant.parse("2024-06-01T12:00:00Z");

        TelemetryRecord oldRecord = new TelemetryRecord(
                UUID.randomUUID(),
                asset,
                older,
                BigDecimal.valueOf(70.0),
                BigDecimal.valueOf(60.0),
                BigDecimal.valueOf(110.0),
                BigDecimal.valueOf(140.0),
                BigDecimal.valueOf(1.5),
                0,
                "sensor-old",
                null
        );

        TelemetryRecord newRecord = new TelemetryRecord(
                UUID.randomUUID(),
                asset,
                newer,
                BigDecimal.valueOf(80.0),
                BigDecimal.valueOf(70.0),
                BigDecimal.valueOf(110.0),
                BigDecimal.valueOf(160.0),
                BigDecimal.valueOf(3.0),
                2,
                "sensor-new",
                null
        );

        telemetryRepository.saveAndFlush(oldRecord);
        telemetryRepository.saveAndFlush(newRecord);

        Optional<TelemetryRecord> latest = telemetryRepository.findFirstByAssetIdOrderByTimestampDesc(assetId);

        assertTrue(latest.isPresent());
        assertEquals(newRecord.getId(), latest.get().getId());
        assertThat(latest.get().getTemperatureCelsius()).isEqualByComparingTo(BigDecimal.valueOf(80.0));
        assertEquals("sensor-new", latest.get().getSourceSensorId());
    }

    // ---------------------------------------------------------------------
    // 4. saveWithDuplicateExternalTelemetryId — fails with constraint violation
    // ---------------------------------------------------------------------
    @Test
    void saveWithDuplicateExternalTelemetryId_shouldFail() {
        TelemetryRecord record1 = new TelemetryRecord(
                UUID.randomUUID(),
                asset,
                Instant.now(),
                BigDecimal.valueOf(70.0),
                BigDecimal.valueOf(60.0),
                BigDecimal.valueOf(110.0),
                BigDecimal.valueOf(140.0),
                BigDecimal.valueOf(1.5),
                0,
                "sensor-1",
                "UNIQUE-EXT-ID"
        );

        TelemetryRecord record2 = new TelemetryRecord(
                UUID.randomUUID(),
                asset,
                Instant.now(),
                BigDecimal.valueOf(75.0),
                BigDecimal.valueOf(65.0),
                BigDecimal.valueOf(110.0),
                BigDecimal.valueOf(150.0),
                BigDecimal.valueOf(2.0),
                1,
                "sensor-2",
                "UNIQUE-EXT-ID"  // Same external ID
        );

        telemetryRepository.saveAndFlush(record1);

        assertThrows(DataIntegrityViolationException.class, () ->
                telemetryRepository.saveAndFlush(record2));
    }

    // ---------------------------------------------------------------------
    // 5. findByAssetIdAndTimestampBetween — returns records in range
    // ---------------------------------------------------------------------
    @Test
    void findByAssetIdAndTimestampBetween_shouldReturnRecordsInRange() {
        Instant from = Instant.parse("2024-06-01T09:00:00Z");
        Instant to = Instant.parse("2024-06-01T11:00:00Z");

        // Record inside range
        TelemetryRecord inside = new TelemetryRecord(
                UUID.randomUUID(),
                asset,
                Instant.parse("2024-06-01T10:00:00Z"),
                BigDecimal.valueOf(75.0),
                BigDecimal.valueOf(65.0),
                BigDecimal.valueOf(110.0),
                BigDecimal.valueOf(150.0),
                BigDecimal.valueOf(2.0),
                1,
                "sensor-inside",
                null
        );

        // Record outside range (too early)
        TelemetryRecord tooEarly = new TelemetryRecord(
                UUID.randomUUID(),
                asset,
                Instant.parse("2024-06-01T08:00:00Z"),
                BigDecimal.valueOf(60.0),
                BigDecimal.valueOf(50.0),
                BigDecimal.valueOf(110.0),
                BigDecimal.valueOf(100.0),
                BigDecimal.valueOf(1.0),
                0,
                "sensor-early",
                null
        );

        // Record outside range (too late)
        TelemetryRecord tooLate = new TelemetryRecord(
                UUID.randomUUID(),
                asset,
                Instant.parse("2024-06-01T12:00:00Z"),
                BigDecimal.valueOf(85.0),
                BigDecimal.valueOf(75.0),
                BigDecimal.valueOf(110.0),
                BigDecimal.valueOf(200.0),
                BigDecimal.valueOf(4.0),
                3,
                "sensor-late",
                null
        );

        telemetryRepository.saveAndFlush(inside);
        telemetryRepository.saveAndFlush(tooEarly);
        telemetryRepository.saveAndFlush(tooLate);

        List<TelemetryRecord> result = telemetryRepository.findByAssetIdAndTimestampBetween(
                assetId, from, to);

        assertEquals(1, result.size());
        assertEquals("sensor-inside", result.get(0).getSourceSensorId());
    }

    // ---------------------------------------------------------------------
    // 6. findByExternalTelemetryId — returns empty for non-existent ID
    // ---------------------------------------------------------------------
    @Test
    void findByExternalTelemetryId_shouldReturnEmptyForNonExistentId() {
        Optional<TelemetryRecord> result = telemetryRepository.findByExternalTelemetryId("NON-EXISTENT");

        assertTrue(result.isEmpty());
    }

    // ---------------------------------------------------------------------
    // 7. existsByExternalTelemetryId — returns true for existing ID
    // ---------------------------------------------------------------------
    @Test
    void existsByExternalTelemetryId_shouldReturnTrueForExistingId() {
        String externalId = "EXIST-123";
        TelemetryRecord record = new TelemetryRecord(
                UUID.randomUUID(),
                asset,
                Instant.now(),
                BigDecimal.valueOf(70.0),
                BigDecimal.valueOf(60.0),
                BigDecimal.valueOf(110.0),
                BigDecimal.valueOf(140.0),
                BigDecimal.valueOf(1.5),
                0,
                "sensor-1",
                externalId
        );
        telemetryRepository.saveAndFlush(record);

        assertTrue(telemetryRepository.existsByExternalTelemetryId(externalId));
        assertFalse(telemetryRepository.existsByExternalTelemetryId("NON-EXIST"));
    }
}
