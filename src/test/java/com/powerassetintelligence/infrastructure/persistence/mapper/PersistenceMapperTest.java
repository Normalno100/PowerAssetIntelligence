package com.powerassetintelligence.infrastructure.persistence.mapper;

import static org.junit.jupiter.api.Assertions.*;

import com.powerassetintelligence.application.port.out.PageRequest;
import com.powerassetintelligence.domain.model.Asset;
import com.powerassetintelligence.domain.model.AssetCriticality;
import com.powerassetintelligence.domain.model.AssetStatus;
import com.powerassetintelligence.domain.model.AssetType;
import com.powerassetintelligence.domain.model.MaintenanceType;
import com.powerassetintelligence.domain.model.RiskAssessmentSnapshot;
import com.powerassetintelligence.domain.model.RiskFactor;
import com.powerassetintelligence.domain.model.RiskFactorSeverity;
import com.powerassetintelligence.domain.model.RiskLevel;
import com.powerassetintelligence.infrastructure.persistence.entity.AssetEntity;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Sort;

class PersistenceMapperTest {

    private static final UUID ENTITY_ID = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
    private static final UUID ASSET_ID = UUID.fromString("660e8400-e29b-41d4-a716-446655440000");
    private static final Instant CREATED_AT = Instant.parse("2024-06-01T12:00:00Z");

    // ---------------------------------------------------------------------
    // 1. toDomain — AssetEntity → Asset maps all fields
    // ---------------------------------------------------------------------
    @Test
    void toDomain_Asset_shouldMapAllFields() {
        AssetEntity entity = new AssetEntity(
                ENTITY_ID,
                AssetType.TRANSFORMER,
                "Test Transformer",
                LocalDate.of(2020, 1, 1),
                AssetStatus.ACTIVE,
                "Substation A",
                "Siemens",
                AssetCriticality.HIGH,
                25,
                Map.of("kv", "110", "phase", "3")
        );

        Asset domain = PersistenceMapper.toDomain(entity);

        assertEquals(ENTITY_ID, domain.getId());
        assertEquals(AssetType.TRANSFORMER, domain.getType());
        assertEquals("Test Transformer", domain.getName());
        assertEquals(LocalDate.of(2020, 1, 1), domain.getInstallationDate());
        assertEquals(AssetStatus.ACTIVE, domain.getStatus());
        assertEquals("Substation A", domain.getLocation());
        assertEquals("Siemens", domain.getManufacturer());
        assertEquals(AssetCriticality.HIGH, domain.getCriticality());
        assertEquals(25, domain.getExpectedServiceLifeYears());
        assertEquals("110", domain.getTechnicalParameters().get("kv"));
        assertEquals("3", domain.getTechnicalParameters().get("phase"));
    }

    // ---------------------------------------------------------------------
    // 2. toDomain — TelemetryRecord entity → domain maps all fields
    // ---------------------------------------------------------------------
    @Test
    void toDomain_TelemetryRecordEntity_shouldMapAllFields() {
        AssetEntity assetEntity = new AssetEntity(
                ASSET_ID,
                AssetType.TRANSFORMER,
                "TX",
                LocalDate.of(2020, 1, 1),
                AssetStatus.ACTIVE,
                "Loc",
                "Man",
                AssetCriticality.HIGH,
                25,
                Map.of()
        );

        com.powerassetintelligence.infrastructure.persistence.entity.TelemetryRecord entity =
                new com.powerassetintelligence.infrastructure.persistence.entity.TelemetryRecord(
                        ENTITY_ID,
                        assetEntity,
                        Instant.parse("2024-06-01T10:00:00Z"),
                        BigDecimal.valueOf(75.5),
                        BigDecimal.valueOf(65.0),
                        BigDecimal.valueOf(110.0),
                        BigDecimal.valueOf(150.0),
                        BigDecimal.valueOf(2.5),
                        1,
                        "sensor-001",
                        "ext-tel-001"
                );

        com.powerassetintelligence.domain.model.TelemetryRecord domain =
                PersistenceMapper.toDomain(entity);

        assertEquals(ENTITY_ID, domain.id());
        assertEquals(ASSET_ID, domain.assetId());
        assertEquals(Instant.parse("2024-06-01T10:00:00Z"), domain.timestamp());
        assertEquals(BigDecimal.valueOf(75.5), domain.temperatureCelsius());
        assertEquals(BigDecimal.valueOf(65.0), domain.loadPercent());
        assertEquals(BigDecimal.valueOf(110.0), domain.voltageKv());
        assertEquals(BigDecimal.valueOf(150.0), domain.currentAmpere());
        assertEquals(BigDecimal.valueOf(2.5), domain.vibrationMmSec());
        assertEquals(1, domain.overheatingCount());
        assertEquals("sensor-001", domain.sourceSensorId());
        assertEquals("ext-tel-001", domain.externalTelemetryId());
    }

    // ---------------------------------------------------------------------
    // 3. toDomain — MaintenanceRecord entity → domain maps all fields
    // ---------------------------------------------------------------------
    @Test
    void toDomain_MaintenanceRecordEntity_shouldMapAllFields() {
        AssetEntity assetEntity = new AssetEntity(
                ASSET_ID,
                AssetType.TRANSFORMER,
                "TX",
                LocalDate.of(2020, 1, 1),
                AssetStatus.ACTIVE,
                "Loc",
                "Man",
                AssetCriticality.HIGH,
                25,
                Map.of()
        );

        com.powerassetintelligence.infrastructure.persistence.entity.MaintenanceRecord entity =
                new com.powerassetintelligence.infrastructure.persistence.entity.MaintenanceRecord(
                        ENTITY_ID,
                        assetEntity,
                        LocalDate.of(2023, 6, 15),
                        MaintenanceType.REPAIR,
                        "Replaced bushing",
                        BigDecimal.valueOf(15000.00),
                        "FAULT-001",
                        "John Doe",
                        List.of("bushing-1", "gasket-set")
                );

        com.powerassetintelligence.domain.model.MaintenanceRecord domain =
                PersistenceMapper.toDomain(entity);

        assertEquals(ENTITY_ID, domain.id());
        assertEquals(ASSET_ID, domain.assetId());
        assertEquals(LocalDate.of(2023, 6, 15), domain.repairDate());
        assertEquals(MaintenanceType.REPAIR, domain.maintenanceType());
        assertEquals("Replaced bushing", domain.description());
        assertEquals(BigDecimal.valueOf(15000.00), domain.repairCost());
        assertEquals("FAULT-001", domain.failureCode());
        assertEquals("John Doe", domain.performedBy());
        assertEquals(2, domain.replacedComponents().size());
    }

    // ---------------------------------------------------------------------
    // 4. toDomain — RiskAssessment entity → domain maps all fields
    // ---------------------------------------------------------------------
    @Test
    void toDomain_RiskAssessmentEntity_shouldMapAllFields() {
        AssetEntity assetEntity = new AssetEntity(
                ASSET_ID,
                AssetType.TRANSFORMER,
                "TX",
                LocalDate.of(2020, 1, 1),
                AssetStatus.ACTIVE,
                "Loc",
                "Man",
                AssetCriticality.HIGH,
                25,
                Map.of()
        );

        com.powerassetintelligence.infrastructure.persistence.entity.RiskAssessment entity =
                new com.powerassetintelligence.infrastructure.persistence.entity.RiskAssessment(
                        ENTITY_ID,
                        assetEntity,
                        Instant.parse("2024-06-01T12:00:00Z"),
                        BigDecimal.valueOf(0.85),
                        RiskLevel.HIGH,
                        List.of(RiskFactor.of("high_temp", RiskFactorSeverity.HIGH,
                                "High temperature detected", BigDecimal.valueOf(0.5))),
                        List.of("Inspect cooling system"),
                        "v1.2.0",
                        "Rule-based assessment",
                        CREATED_AT,
                        null,
                        "RULE_BASED"
                );

        com.powerassetintelligence.domain.model.RiskAssessment domain =
                PersistenceMapper.toDomain(entity);

        assertEquals(ENTITY_ID, domain.id());
        assertEquals(ASSET_ID, domain.assetId());
        assertEquals(Instant.parse("2024-06-01T12:00:00Z"), domain.assessedAt());
        assertEquals(BigDecimal.valueOf(0.85), domain.riskScore());
        assertEquals(RiskLevel.HIGH, domain.riskLevel());
        assertEquals(1, domain.riskFactors().size());
        assertEquals("high_temp", domain.riskFactors().get(0).code());
        assertEquals(RiskFactorSeverity.HIGH, domain.riskFactors().get(0).severity());
        assertEquals(List.of("Inspect cooling system"), domain.recommendations());
        assertEquals("v1.2.0", domain.modelVersion());
        assertEquals("Rule-based assessment", domain.explanation());
        assertEquals("RULE_BASED", domain.scoringSource());
    }

    // ---------------------------------------------------------------------
    // 5. toSpringPageable — handles null
    // ---------------------------------------------------------------------
    @Test
    void toSpringPageable_nullInput_shouldReturnDefault() {
        var result = PersistenceMapper.toSpringPageable(null);
        assertNotNull(result);
        assertEquals(0, result.getPageNumber());
        assertEquals(20, result.getPageSize());
    }

    // ---------------------------------------------------------------------
    // 6. toSpringPageable — empty PageRequest returns defaults
    // ---------------------------------------------------------------------
    @Test
    void toSpringPageable_emptyPageRequest_shouldReturnDefault() {
        PageRequest emptyRequest = new PageRequest(0, 0, List.of());
        var pageable = PersistenceMapper.toSpringPageable(emptyRequest);

        assertEquals(0, pageable.getPageNumber());
        assertEquals(20, pageable.getPageSize());
        assertFalse(pageable.getSort().isSorted());
    }

    // ---------------------------------------------------------------------
    // 7. toSpringPageable — with sort converts direction correctly
    // ---------------------------------------------------------------------
    @Test
    void toSpringPageable_withSort_shouldConvertDirection() {
        PageRequest request = new PageRequest(
                2,
                50,
                List.of(
                        new PageRequest.SortOrder("timestamp", PageRequest.SortOrder.Direction.DESC),
                        new PageRequest.SortOrder("assetId", PageRequest.SortOrder.Direction.ASC)
                )
        );

        var pageable = PersistenceMapper.toSpringPageable(request);

        assertEquals(2, pageable.getPageNumber());
        assertEquals(50, pageable.getPageSize());
        assertTrue(pageable.getSort().isSorted());

        List<Sort.Order> orders = pageable.getSort().toList();
        assertEquals(2, orders.size());
        assertEquals("timestamp", orders.get(0).getProperty());
        assertEquals(Sort.Direction.DESC, orders.get(0).getDirection());
        assertEquals("assetId", orders.get(1).getProperty());
        assertEquals(Sort.Direction.ASC, orders.get(1).getDirection());
    }

    // ---------------------------------------------------------------------
    // 8. toPageResult — maps Page content with custom mapper
    // ---------------------------------------------------------------------
    @Test
    void toPageResult_shouldMapDomainEntities() {
        AssetEntity entity1 = new AssetEntity(
                UUID.randomUUID(),
                AssetType.TRANSFORMER,
                "TX-1",
                LocalDate.of(2020, 1, 1),
                AssetStatus.ACTIVE,
                "Loc-1",
                "Man-1",
                AssetCriticality.HIGH,
                25,
                Map.of()
        );
        AssetEntity entity2 = new AssetEntity(
                UUID.randomUUID(),
                AssetType.CIRCUIT_BREAKER,
                "CB-1",
                LocalDate.of(2021, 6, 1),
                AssetStatus.WARNING,
                "Loc-2",
                "Man-2",
                AssetCriticality.MEDIUM,
                20,
                Map.of()
        );

        Page<AssetEntity> page = new PageImpl<>(List.of(entity1, entity2),
                org.springframework.data.domain.PageRequest.of(0, 10), 2);

        var result = PersistenceMapper.toPageResult(page, PersistenceMapper::toDomain);

        assertEquals(2, result.content().size());
        assertEquals(0, result.page());
        assertEquals(10, result.size());
        assertEquals(2, result.totalElements());
        assertEquals(1, result.totalPages());
    }
}
