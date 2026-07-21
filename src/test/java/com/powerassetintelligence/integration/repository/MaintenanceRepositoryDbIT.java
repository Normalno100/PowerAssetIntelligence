package com.powerassetintelligence.integration.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.powerassetintelligence.domain.model.AssetCriticality;
import com.powerassetintelligence.domain.model.AssetStatus;
import com.powerassetintelligence.domain.model.AssetType;
import com.powerassetintelligence.infrastructure.persistence.entity.AssetEntity;
import com.powerassetintelligence.infrastructure.persistence.entity.MaintenanceRecord;
import com.powerassetintelligence.infrastructure.persistence.repository.AssetRepository;
import com.powerassetintelligence.infrastructure.persistence.repository.MaintenanceRecordRepository;
import com.powerassetintelligence.testsupport.BaseIntegrationTest;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;

class MaintenanceRepositoryDbIT extends BaseIntegrationTest {

    @Autowired
    private AssetRepository assetRepository;

    @Autowired
    private MaintenanceRecordRepository maintenanceRepository;

    @Test
    void findByAssetIdShouldReturnPage() {
        UUID assetId = UUID.randomUUID();
        AssetEntity asset = assetRepository.saveAndFlush(new AssetEntity(
                assetId,
                AssetType.TRANSFORMER,
                "TX-1",
                LocalDate.of(2022, 1, 1),
                AssetStatus.ACTIVE,
                "Site-A",
                "ABB",
                AssetCriticality.HIGH,
                30,
                Map.of()
        ));

        maintenanceRepository.saveAndFlush(new MaintenanceRecord(
                UUID.randomUUID(),
                asset,
                LocalDate.of(2026, 1, 10),
                com.powerassetintelligence.domain.model.MaintenanceType.REPAIR,
                "Routine inspection",
                BigDecimal.valueOf(1000),
                "PC001",
                "Maintenance Co",
                List.of("oil filter")
        ));

        maintenanceRepository.saveAndFlush(new MaintenanceRecord(
                UUID.randomUUID(),
                asset,
                LocalDate.of(2026, 1, 15),
                com.powerassetintelligence.domain.model.MaintenanceType.REPAIR,
                "Repair description",
                BigDecimal.valueOf(2000),
                "FC001",
                "Repair Co",
                List.of("coil")
        ));

        Page<MaintenanceRecord> result = maintenanceRepository.findByAssetId(assetId, org.springframework.data.domain.Pageable.ofSize(10));

        assertEquals(2, result.getTotalElements());
        assertEquals("Maintenance Co", result.getContent().get(0).getPerformedBy());
    }

    @Test
    void countByAssetIdAndRepairDateGreaterThanEqualShouldReturnCount() {
        UUID assetId = UUID.randomUUID();
        AssetEntity asset = assetRepository.saveAndFlush(new AssetEntity(
                assetId,
                AssetType.TRANSFORMER,
                "TX-1",
                LocalDate.of(2022, 1, 1),
                AssetStatus.ACTIVE,
                "Site-A",
                "ABB",
                AssetCriticality.HIGH,
                30,
                Map.of()
        ));

        LocalDate today = LocalDate.now();
        maintenanceRepository.saveAndFlush(new MaintenanceRecord(
                UUID.randomUUID(),
                asset,
                today.minusDays(10),
                com.powerassetintelligence.domain.model.MaintenanceType.REPAIR,
                "Routine inspection",
                BigDecimal.valueOf(1000),
                "PC001",
                "Maintenance Co",
                List.of("oil filter")
        ));

        maintenanceRepository.saveAndFlush(new MaintenanceRecord(
                UUID.randomUUID(),
                asset,
                today.minusDays(30),
                com.powerassetintelligence.domain.model.MaintenanceType.REPAIR,
                "Quarterly inspection",
                BigDecimal.valueOf(1500),
                "PC002",
                "Maintenance Co",
                List.of("gasket")
        ));

        maintenanceRepository.saveAndFlush(new MaintenanceRecord(
                UUID.randomUUID(),
                asset,
                today.minusYears(2),
                com.powerassetintelligence.domain.model.MaintenanceType.REPAIR,
                "Old maintenance",
                BigDecimal.valueOf(500),
                "PC003",
                "Maintenance Co",
                List.of("part")
        ));

        long count = maintenanceRepository.countByAssetIdAndRepairDateGreaterThanEqual(assetId, today.minusYears(1));

        assertEquals(2, count);
    }

    @Test
    void findByAssetIdShouldOrderByRepairDate() {
        UUID assetId = UUID.randomUUID();
        AssetEntity asset = assetRepository.saveAndFlush(new AssetEntity(
                assetId,
                AssetType.TRANSFORMER,
                "TX-1",
                LocalDate.of(2022, 1, 1),
                AssetStatus.ACTIVE,
                "Site-A",
                "ABB",
                AssetCriticality.HIGH,
                30,
                Map.of()
        ));

        MaintenanceRecord first = maintenanceRepository.saveAndFlush(new MaintenanceRecord(
                UUID.randomUUID(),
                asset,
                LocalDate.of(2026, 1, 5),
                com.powerassetintelligence.domain.model.MaintenanceType.REPAIR,
                "First maintenance",
                BigDecimal.valueOf(1000),
                "PC001",
                "Maintenance Co",
                List.of("oil filter")
        ));

        MaintenanceRecord last = maintenanceRepository.saveAndFlush(new MaintenanceRecord(
                UUID.randomUUID(),
                asset,
                LocalDate.of(2026, 1, 20),
                com.powerassetintelligence.domain.model.MaintenanceType.REPAIR,
                "Last maintenance",
                BigDecimal.valueOf(2000),
                "FC001",
                "Repair Co",
                List.of("coil")
        ));

        Page<MaintenanceRecord> result = maintenanceRepository.findByAssetId(assetId, org.springframework.data.domain.Pageable.ofSize(10));

        assertEquals(2, result.getTotalElements());
        assertEquals(first.getId(), result.getContent().get(0).getId());
        assertEquals(last.getId(), result.getContent().get(1).getId());
    }
}
