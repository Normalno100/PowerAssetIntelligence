package com.powerassetintelligence.unit.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.powerassetintelligence.application.dto.TelemetryAcceptedResponse;
import com.powerassetintelligence.application.dto.TelemetryCreateCommand;
import com.powerassetintelligence.application.dto.TelemetryResponse;
import com.powerassetintelligence.application.port.out.AssetRepositoryPort;
import com.powerassetintelligence.application.port.out.PageRequest;
import com.powerassetintelligence.application.port.out.PageResult;
import com.powerassetintelligence.application.port.out.TelemetryEventPublisher;
import com.powerassetintelligence.application.port.out.TelemetryRepositoryPort;
import com.powerassetintelligence.application.service.BusinessValidationException;
import com.powerassetintelligence.application.service.ResourceNotFoundException;
import com.powerassetintelligence.application.service.TelemetryService;
import com.powerassetintelligence.domain.model.Asset;
import com.powerassetintelligence.domain.model.AssetStatus;
import com.powerassetintelligence.domain.model.AssetType;
import com.powerassetintelligence.domain.model.TelemetryRecord;
import java.math.BigDecimal;
import java.time.Instant;
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
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

@ExtendWith(MockitoExtension.class)
class TelemetryServiceTest {

    @Mock
    private AssetRepositoryPort assetRepository;

    @Mock
    private TelemetryRepositoryPort telemetryRepository;

    @Mock
    private TelemetryEventPublisher telemetryEventPublisher;

    @Mock
    private TransactionTemplate transactionTemplate;

    @Captor
    private ArgumentCaptor<TelemetryRecord> telemetryCaptor;

    private TelemetryService service;

    private static final UUID ASSET_ID = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
    private static final Instant NOW = Instant.now();

    private Asset activeAsset;
    private Asset decommissionedAsset;
    private TelemetryCreateCommand commandWithExternalId;
    private TelemetryCreateCommand commandWithoutExternalId;

    @BeforeEach
    void setUp() {
        activeAsset = new Asset(
                ASSET_ID,
                AssetType.TRANSFORMER,
                "Test Transformer",
                java.time.LocalDate.of(2020, 1, 1),
                AssetStatus.ACTIVE,
                "Substation A",
                "Siemens",
                com.powerassetintelligence.domain.model.AssetCriticality.HIGH,
                25,
                Map.of()
        );

        decommissionedAsset = new Asset(
                ASSET_ID,
                AssetType.TRANSFORMER,
                "Test Transformer",
                java.time.LocalDate.of(2020, 1, 1),
                AssetStatus.DECOMMISSIONED,
                "Substation A",
                "Siemens",
                com.powerassetintelligence.domain.model.AssetCriticality.HIGH,
                25,
                Map.of()
        );

        commandWithExternalId = new TelemetryCreateCommand(
                ASSET_ID,
                NOW,
                BigDecimal.valueOf(65.0),
                BigDecimal.valueOf(70.0),
                BigDecimal.valueOf(110.0),
                BigDecimal.valueOf(150.0),
                BigDecimal.valueOf(2.5),
                1,
                "sensor-001",
                "ext-tel-001"
        );

        // Command without externalTelemetryId - bypasses persistWithRetry
        commandWithoutExternalId = new TelemetryCreateCommand(
                ASSET_ID,
                NOW,
                BigDecimal.valueOf(65.0),
                BigDecimal.valueOf(70.0),
                BigDecimal.valueOf(110.0),
                BigDecimal.valueOf(150.0),
                BigDecimal.valueOf(2.5),
                1,
                "sensor-001",
                null  // No external ID
        );

        service = new TelemetryService(
                assetRepository,
                telemetryRepository,
                telemetryEventPublisher,
                transactionTemplate
        );
    }

    // ---------------------------------------------------------------------
    // 1. ingest — publishes event, does NOT persist
    // ---------------------------------------------------------------------
    @Test
    void ingest_shouldPublishEventAndReturnAcceptedResponse() {
        TelemetryAcceptedResponse expected = new TelemetryAcceptedResponse(
                UUID.randomUUID(), ASSET_ID, "ACCEPTED", NOW
        );
        when(telemetryEventPublisher.publish(commandWithExternalId)).thenReturn(expected);

        TelemetryAcceptedResponse response = service.ingest(commandWithExternalId);

        assertNotNull(response);
        assertEquals("ACCEPTED", response.status());
        verify(telemetryEventPublisher).publish(commandWithExternalId);
        verify(telemetryRepository, never()).save(any());
    }

    // ---------------------------------------------------------------------
    // 2. persist with externalTelemetryId — idempotent, returns existing
    // ---------------------------------------------------------------------
    @Test
    void persist_withExistingExternalId_shouldReturnExistingRecord() {
        TelemetryRecord existing = createTelemetryRecord(
                UUID.fromString("11111111-1111-1111-1111-111111111111"), ASSET_ID, NOW);
        when(transactionTemplate.execute(any(TransactionCallback.class)))
                .thenAnswer(invocation -> {
                    TransactionCallback<Object> callback = invocation.getArgument(0);
                    return callback.doInTransaction(null);
                });
        when(telemetryRepository.findByExternalTelemetryId("ext-tel-001"))
                .thenReturn(Optional.of(existing));

        TelemetryResponse response = service.persist(commandWithExternalId);

        assertEquals(existing.id(), response.id());
        verify(telemetryRepository, never()).save(any());
    }

    // ---------------------------------------------------------------------
    // 3. persist new record — validates asset, persists, returns response
    // ---------------------------------------------------------------------
    @Test
    void persist_newRecord_shouldValidateAssetAndPersist() {
        when(assetRepository.findById(ASSET_ID)).thenReturn(Optional.of(activeAsset));
        when(telemetryRepository.save(any(TelemetryRecord.class))).thenAnswer(inv -> inv.getArgument(0));

        TelemetryResponse response = service.persist(commandWithoutExternalId);

        assertNotNull(response);
        assertEquals(ASSET_ID, response.assetId());
        verify(telemetryRepository).save(telemetryCaptor.capture());
        TelemetryRecord captured = telemetryCaptor.getValue();
        assertEquals(ASSET_ID, captured.assetId());
        assertEquals(BigDecimal.valueOf(65.0), captured.temperatureCelsius());
        assertNull(captured.externalTelemetryId());
    }

    // ---------------------------------------------------------------------
    // 4. persist — decommissioned asset throws BusinessValidationException
    // ---------------------------------------------------------------------
    @Test
    void persist_decommissionedAsset_shouldThrowBusinessValidationException() {
        when(assetRepository.findById(ASSET_ID)).thenReturn(Optional.of(decommissionedAsset));

        assertThrows(BusinessValidationException.class, () -> service.persist(commandWithoutExternalId));

        verify(telemetryRepository, never()).save(any());
    }

    // ---------------------------------------------------------------------
    // 5. persist — asset not found throws ResourceNotFoundException
    // ---------------------------------------------------------------------
    @Test
    void persist_assetNotFound_shouldThrowResourceNotFoundException() {
        when(assetRepository.findById(ASSET_ID)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.persist(commandWithoutExternalId));

        verify(telemetryRepository, never()).save(any());
    }

    // ---------------------------------------------------------------------
    // 6. findByAsset — validates asset existence, returns page
    // ---------------------------------------------------------------------
    @Test
    void findByAsset_shouldValidateAssetAndReturnPage() {
        when(assetRepository.findById(ASSET_ID)).thenReturn(Optional.of(activeAsset));
        TelemetryRecord record = createTelemetryRecord(UUID.randomUUID(), ASSET_ID, NOW);
        PageRequest pageRequest = new PageRequest(0, 10, List.of());
        when(telemetryRepository.findByAssetId(ASSET_ID, pageRequest))
                .thenReturn(new PageResult<>(List.of(record), 0, 10, 1, 1));

        PageResult<TelemetryResponse> result = service.findByAsset(ASSET_ID, pageRequest);

        assertEquals(1, result.content().size());
        assertEquals(ASSET_ID, result.content().get(0).assetId());
    }

    // ---------------------------------------------------------------------
    // 7. findByAsset — asset not found throws ResourceNotFoundException
    // ---------------------------------------------------------------------
    @Test
    void findByAsset_assetNotFound_shouldThrowResourceNotFoundException() {
        when(assetRepository.findById(ASSET_ID)).thenReturn(Optional.empty());
        PageRequest pageRequest = new PageRequest(0, 10, List.of());

        assertThrows(ResourceNotFoundException.class, () -> service.findByAsset(ASSET_ID, pageRequest));

        verify(telemetryRepository, never()).findByAssetId(any(), any());
    }

    // ---------------------------------------------------------------------
    // 8. getLatest — returns latest telemetry for asset
    // ---------------------------------------------------------------------
    @Test
    void getLatest_shouldReturnLatestTelemetry() {
        when(assetRepository.findById(ASSET_ID)).thenReturn(Optional.of(activeAsset));
        TelemetryRecord latest = createTelemetryRecord(UUID.randomUUID(), ASSET_ID, NOW);
        when(telemetryRepository.findFirstByAssetIdOrderByTimestampDesc(ASSET_ID))
                .thenReturn(Optional.of(latest));

        TelemetryResponse response = service.getLatest(ASSET_ID);

        assertNotNull(response);
        assertEquals(ASSET_ID, response.assetId());
        assertEquals(latest.temperatureCelsius(), response.temperatureCelsius());
    }

    // ---------------------------------------------------------------------
    // 9. getLatest — asset not found throws ResourceNotFoundException
    // ---------------------------------------------------------------------
    @Test
    void getLatest_assetNotFound_shouldThrowResourceNotFoundException() {
        when(assetRepository.findById(ASSET_ID)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.getLatest(ASSET_ID));

        verify(telemetryRepository, never()).findFirstByAssetIdOrderByTimestampDesc(any());
    }

    // ---------------------------------------------------------------------
    // 10. getLatest — no telemetry for existing asset
    // ---------------------------------------------------------------------
    @Test
    void getLatest_noTelemetry_shouldThrowResourceNotFoundException() {
        when(assetRepository.findById(ASSET_ID)).thenReturn(Optional.of(activeAsset));
        when(telemetryRepository.findFirstByAssetIdOrderByTimestampDesc(ASSET_ID))
                .thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.getLatest(ASSET_ID));
    }

    // ---------------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------------

    private TelemetryRecord createTelemetryRecord(UUID id, UUID assetId, Instant timestamp) {
        return new TelemetryRecord(
                id,
                assetId,
                timestamp,
                BigDecimal.valueOf(65.0),
                BigDecimal.valueOf(70.0),
                BigDecimal.valueOf(110.0),
                BigDecimal.valueOf(150.0),
                BigDecimal.valueOf(2.5),
                1,
                "sensor-001",
                "ext-tel-001"
        );
    }
}
