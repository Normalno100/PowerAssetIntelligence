package com.powerassetintelligence.infrastructure.web;

import static org.junit.jupiter.api.Assertions.*;

import com.powerassetintelligence.application.dto.AssetCreateCommand;
import com.powerassetintelligence.application.dto.AssetUpdateCommand;
import com.powerassetintelligence.application.dto.TelemetryCreateCommand;
import com.powerassetintelligence.application.port.out.PageRequest;
import com.powerassetintelligence.domain.model.AssetCriticality;
import com.powerassetintelligence.domain.model.AssetStatus;
import com.powerassetintelligence.domain.model.AssetType;
import com.powerassetintelligence.domain.model.MaintenanceType;
import com.powerassetintelligence.infrastructure.web.dto.AssetCreateRequest;
import com.powerassetintelligence.infrastructure.web.dto.AssetUpdateRequest;
import com.powerassetintelligence.infrastructure.web.dto.MaintenanceCreateRequest;
import com.powerassetintelligence.infrastructure.web.dto.TelemetryCreateRequest;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

class WebMapperTest {

    private static final UUID ASSET_ID = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
    private static final Instant TIMESTAMP = Instant.parse("2024-06-01T12:00:00Z");

    // =====================================================================
    // AssetWebMapper tests
    // =====================================================================

    // 1. toCreateCommand — maps all fields
    @Test
    void AssetWebMapper_toCreateCommand_shouldMapAllFields() {
        AssetCreateRequest request = new AssetCreateRequest(
                AssetType.TRANSFORMER,
                "TX-101",
                LocalDate.of(2022, 5, 3),
                "North substation",
                "ABB",
                AssetCriticality.HIGH,
                30,
                Map.of("kv", "110", "phase", "3")
        );

        AssetCreateCommand command = AssetWebMapper.toCreateCommand(request);

        assertEquals(AssetType.TRANSFORMER, command.type());
        assertEquals("TX-101", command.name());
        assertEquals(LocalDate.of(2022, 5, 3), command.installationDate());
        assertEquals("North substation", command.location());
        assertEquals("ABB", command.manufacturer());
        assertEquals(AssetCriticality.HIGH, command.criticality());
        assertEquals(30, command.expectedServiceLifeYears());
        assertEquals("110", command.technicalParameters().get("kv"));
        assertEquals("3", command.technicalParameters().get("phase"));
    }

    // 2. toUpdateCommand — maps all fields
    @Test
    void AssetWebMapper_toUpdateCommand_shouldMapAllFields() {
        AssetUpdateRequest request = new AssetUpdateRequest(
                AssetType.TRANSFORMER,
                "TX-101 Updated",
                LocalDate.of(2022, 5, 3),
                AssetStatus.WARNING,
                "South substation",
                "Siemens",
                AssetCriticality.MEDIUM,
                35,
                Map.of("kv", "220")
        );

        AssetUpdateCommand command = AssetWebMapper.toUpdateCommand(request);

        assertEquals(AssetType.TRANSFORMER, command.type());
        assertEquals("TX-101 Updated", command.name());
        assertEquals(LocalDate.of(2022, 5, 3), command.installationDate());
        assertEquals(AssetStatus.WARNING, command.status());
        assertEquals("South substation", command.location());
        assertEquals("Siemens", command.manufacturer());
        assertEquals(AssetCriticality.MEDIUM, command.criticality());
        assertEquals(35, command.expectedServiceLifeYears());
        assertEquals("220", command.technicalParameters().get("kv"));
    }

    // =====================================================================
    // MaintenanceWebMapper tests
    // =====================================================================

    // 3. toCreateCommand — trims description and performedBy
    @Test
    void MaintenanceWebMapper_toCreateCommand_shouldTrimDescriptionAndPerformedBy() {
        MaintenanceCreateRequest request = new MaintenanceCreateRequest(
                LocalDate.of(2024, 1, 15),
                MaintenanceType.REPAIR,
                "  Replaced bushing  ",
                BigDecimal.valueOf(15000.00),
                "FAULT-001",
                "  John Doe  ",
                List.of("bushing-1")
        );

        var command = MaintenanceWebMapper.toCreateCommand(request);

        assertEquals("Replaced bushing", command.description());
        assertEquals("John Doe", command.performedBy());
    }

    // 4. toCreateCommand — preserves null fields
    @Test
    void MaintenanceWebMapper_toCreateCommand_shouldPreserveNullFields() {
        MaintenanceCreateRequest request = new MaintenanceCreateRequest(
                LocalDate.of(2024, 1, 15),
                MaintenanceType.INSPECTION,
                "Routine check",
                BigDecimal.ZERO,
                null,  // failureCode is optional
                "Inspector",
                List.of()
        );

        var command = MaintenanceWebMapper.toCreateCommand(request);

        assertEquals(LocalDate.of(2024, 1, 15), command.repairDate());
        assertEquals(MaintenanceType.INSPECTION, command.maintenanceType());
        assertNull(command.failureCode());
        assertEquals(BigDecimal.ZERO, command.repairCost());
    }

    // =====================================================================
    // TelemetryWebMapper tests
    // =====================================================================

    // 5. toCommand — maps all fields
    @Test
    void TelemetryWebMapper_toCommand_shouldMapAllFields() {
        TelemetryCreateRequest request = new TelemetryCreateRequest(
                ASSET_ID,
                TIMESTAMP,
                BigDecimal.valueOf(75.5),
                BigDecimal.valueOf(65.0),
                BigDecimal.valueOf(110.0),
                BigDecimal.valueOf(150.0),
                BigDecimal.valueOf(2.5),
                1,
                "sensor-001",
                "ext-tel-001"
        );

        TelemetryCreateCommand command = TelemetryWebMapper.toCommand(request);

        assertEquals(ASSET_ID, command.assetId());
        assertEquals(TIMESTAMP, command.timestamp());
        assertEquals(BigDecimal.valueOf(75.5), command.temperatureCelsius());
        assertEquals(BigDecimal.valueOf(65.0), command.loadPercent());
        assertEquals(BigDecimal.valueOf(110.0), command.voltageKv());
        assertEquals(BigDecimal.valueOf(150.0), command.currentAmpere());
        assertEquals(BigDecimal.valueOf(2.5), command.vibrationMmSec());
        assertEquals(1, command.overheatingCount());
        assertEquals("sensor-001", command.sourceSensorId());
        assertEquals("ext-tel-001", command.externalTelemetryId());
    }

    // 6. TelemetryCreateCommand normalizes blank sourceSensorId to null
    @Test
    void TelemetryWebMapper_toCommand_blankSourceSensorId_shouldBeNormalizedToNull() {
        TelemetryCreateRequest request = new TelemetryCreateRequest(
                ASSET_ID,
                TIMESTAMP,
                BigDecimal.valueOf(75.5),
                BigDecimal.valueOf(65.0),
                BigDecimal.valueOf(110.0),
                BigDecimal.valueOf(150.0),
                BigDecimal.valueOf(2.5),
                0,
                "   ",
                null
        );

        TelemetryCreateCommand command = TelemetryWebMapper.toCommand(request);

        assertNull(command.sourceSensorId());
    }

    // =====================================================================
    // WebPageMapper tests
    // =====================================================================

    // 7. toPageRequest — converts Spring Pageable to PageRequest
    @Test
    void WebPageMapper_toPageRequest_shouldConvertSpringPageable() {
        Sort sort = Sort.by(Sort.Direction.DESC, "timestamp")
                .and(Sort.by(Sort.Direction.ASC, "assetId"));
        Pageable pageable = org.springframework.data.domain.PageRequest.of(2, 50, sort);

        PageRequest result = WebPageMapper.toPageRequest(pageable);

        assertEquals(2, result.page());
        assertEquals(50, result.size());
        assertEquals(2, result.sort().size());

        assertEquals("timestamp", result.sort().get(0).field());
        assertEquals(PageRequest.SortOrder.Direction.DESC, result.sort().get(0).direction());
        assertEquals("assetId", result.sort().get(1).field());
        assertEquals(PageRequest.SortOrder.Direction.ASC, result.sort().get(1).direction());
    }

    // 8. toPageRequest — handles null
    @Test
    void WebPageMapper_toPageRequest_shouldHandleNull() {
        PageRequest result = WebPageMapper.toPageRequest(null);

        assertEquals(0, result.page());
        assertEquals(20, result.size());
        assertTrue(result.sort().isEmpty());
    }

    // 9. toPageResult — maps Page content
    @Test
    void WebPageMapper_toPageResult_shouldMapContent() {
        List<String> content = List.of("item1", "item2", "item3");
        Page<String> page = new PageImpl<>(content,
                org.springframework.data.domain.PageRequest.of(1, 10), 25);

        var result = WebPageMapper.toPageResult(page);

        assertEquals(3, result.content().size());
        assertEquals("item1", result.content().get(0));
        assertEquals(1, result.page());
        assertEquals(10, result.size());
        assertEquals(25, result.totalElements());
        assertEquals(3, result.totalPages());
    }

    // 10. mapPage — applies mapper to content
    @Test
    void WebPageMapper_mapPage_shouldApplyMapper() {
        List<Integer> content = List.of(1, 2, 3);
        Page<Integer> page = new PageImpl<>(content,
                org.springframework.data.domain.PageRequest.of(0, 10), 100);

        var result = WebPageMapper.mapPage(page, x -> x * 10);

        assertEquals(3, result.content().size());
        assertEquals(10, result.content().get(0));
        assertEquals(20, result.content().get(1));
        assertEquals(30, result.content().get(2));
        assertEquals(100, result.totalElements());
    }
}
