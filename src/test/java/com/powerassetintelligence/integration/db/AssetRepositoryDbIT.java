package com.powerassetintelligence.integration.db;

import static org.junit.jupiter.api.Assertions.assertThrows;

import com.powerassetintelligence.domain.model.AssetCriticality;
import com.powerassetintelligence.domain.model.AssetStatus;
import com.powerassetintelligence.domain.model.AssetType;
import com.powerassetintelligence.infrastructure.persistence.entity.Asset;
import com.powerassetintelligence.infrastructure.persistence.entity.TelemetryRecord;
import com.powerassetintelligence.infrastructure.persistence.repository.AssetRepository;
import com.powerassetintelligence.infrastructure.persistence.repository.TelemetryRecordRepository;
import com.powerassetintelligence.testsupport.BaseIntegrationTest;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;

class AssetRepositoryDbIT extends BaseIntegrationTest {

    @Autowired
    private AssetRepository assetRepository;

    @Autowired
    private TelemetryRecordRepository telemetryRecordRepository;

    @Test
    void shouldFailOnDuplicateExternalTelemetryId() {
        Asset asset = assetRepository.saveAndFlush(new Asset(UUID.randomUUID(), AssetType.TRANSFORMER, "DB-1",
                LocalDate.of(2022, 2, 2), AssetStatus.ACTIVE, "Loc-1", "ABB", AssetCriticality.HIGH, 30, Map.of()));

        TelemetryRecord first = new TelemetryRecord(UUID.randomUUID(), asset, Instant.now(), BigDecimal.TEN,
                BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ONE, 0, "sensor-1", "EXT-777");
        TelemetryRecord second = new TelemetryRecord(UUID.randomUUID(), asset, Instant.now(), BigDecimal.TEN,
                BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ONE, 0, "sensor-2", "EXT-777");

        telemetryRecordRepository.saveAndFlush(first);
        assertThrows(DataIntegrityViolationException.class, () -> telemetryRecordRepository.saveAndFlush(second));
    }
}
