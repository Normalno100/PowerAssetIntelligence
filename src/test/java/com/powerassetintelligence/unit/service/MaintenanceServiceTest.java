package com.powerassetintelligence.unit.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.powerassetintelligence.application.dto.MaintenanceCreateCommand;
import com.powerassetintelligence.application.dto.MaintenanceResponse;
import com.powerassetintelligence.application.port.out.AssetRepositoryPort;
import com.powerassetintelligence.application.port.out.MaintenanceRepositoryPort;
import com.powerassetintelligence.application.port.out.PageRequest;
import com.powerassetintelligence.application.port.out.PageResult;
import com.powerassetintelligence.application.exception.BusinessValidationException;
import com.powerassetintelligence.application.exception.ResourceNotFoundException;
import com.powerassetintelligence.application.service.MaintenanceService;
import com.powerassetintelligence.domain.model.Asset;
import com.powerassetintelligence.domain.model.AssetStatus;
import com.powerassetintelligence.domain.model.AssetType;
import com.powerassetintelligence.domain.model.AssetCriticality;
import com.powerassetintelligence.domain.model.MaintenanceRecord;
import com.powerassetintelligence.domain.model.MaintenanceType;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MaintenanceServiceTest {

    @Mock
    private AssetRepositoryPort assetRepository;

    @Mock
    private MaintenanceRepositoryPort maintenanceRepository;

    @Captor
    private ArgumentCaptor<MaintenanceRecord> maintenanceCaptor;

    @Captor
    private ArgumentCaptor<Asset> assetCaptor;

    private MaintenanceService service;

    private static final UUID ASSET_ID = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
    private static final LocalDate INSTALLATION_DATE = LocalDate.of(2020, 1, 1);
    private static final LocalDate VALID_REPAIR_DATE = LocalDate.of(2023, 6, 15);
    private static final LocalDate EARLY_REPAIR_DATE = LocalDate.of(2019, 1, 1);

    private Asset activeAsset;
    private MaintenanceCreateCommand validCommand;

    @BeforeEach
    void setUp() {
        activeAsset = new Asset(
                ASSET_ID,
                AssetType.TRANSFORMER,
                "Test Transformer",
                INSTALLATION_DATE,
                AssetStatus.ACTIVE,
                "Substation A",
                "Siemens",
                AssetCriticality.HIGH,
                25,
                Map.of()
        );

        validCommand = new MaintenanceCreateCommand(
                VALID_REPAIR_DATE,
                MaintenanceType.REPAIR,
                "Replaced bushing",
                BigDecimal.valueOf(15000.00),
                "FAULT-001",
                "John Doe",
                List.of("bushing-1", "gasket-set")
        );

        service = new MaintenanceService(assetRepository, maintenanceRepository);
    }

    // ---------------------------------------------------------------------
    // 1. create — happy path: creates maintenance and transitions asset
    // ---------------------------------------------------------------------
    @Test
    void create_shouldCreateMaintenanceAndTransitionAssetToUnderMaintenance() {
        when(assetRepository.findById(ASSET_ID)).thenReturn(Optional.of(activeAsset));
        when(maintenanceRepository.save(any(MaintenanceRecord.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        MaintenanceResponse response = service.create(ASSET_ID, validCommand);

        assertNotNull(response);
        assertEquals(ASSET_ID, response.assetId());
        assertEquals(MaintenanceType.REPAIR, response.maintenanceType());
        assertEquals("Replaced bushing", response.description());
        assertEquals(BigDecimal.valueOf(15000.00), response.repairCost());
        assertEquals("John Doe", response.performedBy());
        assertEquals(List.of("bushing-1", "gasket-set"), response.replacedComponents());

        // Asset status should be transitioned to UNDER_MAINTENANCE
        verify(assetRepository).save(assetCaptor.capture());
        Asset savedAsset = assetCaptor.getValue();
        assertEquals(AssetStatus.UNDER_MAINTENANCE, savedAsset.getStatus());
    }

    // ---------------------------------------------------------------------
    // 2. create — repair date before installation throws BusinessValidationException
    // ---------------------------------------------------------------------
    @Test
    void create_shouldThrowBusinessValidationWhenRepairDateBeforeInstallation() {
        MaintenanceCreateCommand invalidCommand = new MaintenanceCreateCommand(
                EARLY_REPAIR_DATE,
                MaintenanceType.REPAIR,
                "Early repair",
                BigDecimal.valueOf(5000.00),
                null,
                "Jane Doe",
                List.of()
        );

        when(assetRepository.findById(ASSET_ID)).thenReturn(Optional.of(activeAsset));

        BusinessValidationException exception = assertThrows(
                BusinessValidationException.class,
                () -> service.create(ASSET_ID, invalidCommand)
        );

        assertEquals("Repair date cannot be before asset installation date", exception.getMessage());
        verify(assetRepository, never()).save(any());
        verify(maintenanceRepository, never()).save(any());
    }

    // ---------------------------------------------------------------------
    // 3. create — asset not found throws ResourceNotFoundException
    // ---------------------------------------------------------------------
    @Test
    void create_shouldThrowResourceNotFoundWhenAssetNotFound() {
        when(assetRepository.findById(ASSET_ID)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.create(ASSET_ID, validCommand));

        verify(maintenanceRepository, never()).save(any());
    }

    // ---------------------------------------------------------------------
    // 4. create — saves maintenance record with correct data
    // ---------------------------------------------------------------------
    @Test
    void create_shouldSaveMaintenanceRecordWithCorrectData() {
        when(assetRepository.findById(ASSET_ID)).thenReturn(Optional.of(activeAsset));
        when(maintenanceRepository.save(any(MaintenanceRecord.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        service.create(ASSET_ID, validCommand);

        verify(maintenanceRepository).save(maintenanceCaptor.capture());
        MaintenanceRecord saved = maintenanceCaptor.getValue();

        assertEquals(ASSET_ID, saved.assetId());
        assertEquals(VALID_REPAIR_DATE, saved.repairDate());
        assertEquals(MaintenanceType.REPAIR, saved.maintenanceType());
        assertEquals("Replaced bushing", saved.description());
        assertEquals(BigDecimal.valueOf(15000.00), saved.repairCost());
        assertEquals("FAULT-001", saved.failureCode());
        assertEquals("John Doe", saved.performedBy());
        // replacedComponents should be an unmodifiable copy
        assertInstanceOf(List.class, saved.replacedComponents());
        assertEquals(2, saved.replacedComponents().size());
    }

    // ---------------------------------------------------------------------
    // 5. create — replacedComponents should be copied (immutability)
    // ---------------------------------------------------------------------
    @Test
    void create_shouldCopyReplacedComponentsList() {
        java.util.ArrayList<String> mutableList = new java.util.ArrayList<>();
        mutableList.add("part-1");
        MaintenanceCreateCommand command = new MaintenanceCreateCommand(
                VALID_REPAIR_DATE,
                MaintenanceType.COMPONENT_REPLACEMENT,
                "Replace part",
                BigDecimal.valueOf(1000.00),
                null,
                "Tech",
                mutableList
        );

        when(assetRepository.findById(ASSET_ID)).thenReturn(Optional.of(activeAsset));
        when(maintenanceRepository.save(any(MaintenanceRecord.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        service.create(ASSET_ID, command);

        verify(maintenanceRepository).save(maintenanceCaptor.capture());
        MaintenanceRecord saved = maintenanceCaptor.getValue();
        // Modifying original list should not affect saved record
        mutableList.add("part-2");
        assertEquals(1, saved.replacedComponents().size());
    }

    // ---------------------------------------------------------------------
    // 6. findByAsset — returns page of maintenance records
    // ---------------------------------------------------------------------
    @Test
    void findByAsset_shouldReturnPageOfMaintenanceRecords() {
        MaintenanceRecord record = new MaintenanceRecord(
                UUID.randomUUID(),
                ASSET_ID,
                VALID_REPAIR_DATE,
                MaintenanceType.INSPECTION,
                "Routine check",
                BigDecimal.ZERO,
                null,
                "Inspector",
                List.of(),
                Instant.now()
        );

        when(assetRepository.findById(ASSET_ID)).thenReturn(Optional.of(activeAsset));
        PageRequest pageRequest = new PageRequest(0, 10, List.of());
        when(maintenanceRepository.findByAssetId(ASSET_ID, pageRequest))
                .thenReturn(new PageResult<>(List.of(record), 0, 10, 1, 1));

        PageResult<MaintenanceResponse> result = service.findByAsset(ASSET_ID, pageRequest);

        assertNotNull(result);
        assertEquals(1, result.content().size());
        assertEquals(MaintenanceType.INSPECTION, result.content().get(0).maintenanceType());
        assertEquals("Routine check", result.content().get(0).description());
    }

    // ---------------------------------------------------------------------
    // 7. findByAsset — asset not found throws ResourceNotFoundException
    // ---------------------------------------------------------------------
    @Test
    void findByAsset_shouldThrowWhenAssetNotFound() {
        when(assetRepository.findById(ASSET_ID)).thenReturn(Optional.empty());
        PageRequest pageRequest = new PageRequest(0, 10, List.of());

        assertThrows(ResourceNotFoundException.class, () -> service.findByAsset(ASSET_ID, pageRequest));

        verify(maintenanceRepository, never()).findByAssetId(any(), any());
    }
}
