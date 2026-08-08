package com.powerassetintelligence.application.service;

import com.powerassetintelligence.application.dto.TelemetryAcceptedResponse;
import com.powerassetintelligence.application.dto.TelemetryCreateCommand;
import com.powerassetintelligence.application.dto.TelemetryResponse;
import com.powerassetintelligence.application.port.out.TelemetryEventPublisher;
import com.powerassetintelligence.application.port.out.TelemetryRepositoryPort;
import com.powerassetintelligence.application.port.out.PageRequest;
import com.powerassetintelligence.application.port.out.PageResult;
import com.powerassetintelligence.domain.model.AssetStatus;
import com.powerassetintelligence.domain.model.Asset;
import com.powerassetintelligence.domain.model.TelemetryRecord;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class TelemetryService {

    private final AssetService assetService;
    private final TelemetryRepositoryPort telemetryRecordRepository;
    private final TelemetryEventPublisher telemetryEventPublisher;

    public TelemetryService(
            AssetService assetService,
            TelemetryRepositoryPort telemetryRecordRepository,
            TelemetryEventPublisher telemetryEventPublisher
    ) {
        this.assetService = assetService;
        this.telemetryRecordRepository = telemetryRecordRepository;
        this.telemetryEventPublisher = telemetryEventPublisher;
    }

    /**
     * Ingests telemetry from an HTTP request.
     *
     * This method publishes a telemetry event to Kafka via the
     * {@link TelemetryEventPublisher} port. It does NOT persist
     * telemetry to the database — that is the responsibility of
     * the Kafka consumer pipeline (persist method).
     *
     * Semantics: "event accepted for asynchronous processing"
     * Returns 202 Accepted upon successful publication.
     *
     * @param command the telemetry command from the HTTP request
     * @return acceptance response with event metadata
     */
    @Transactional
    public TelemetryAcceptedResponse ingest(TelemetryCreateCommand command) {
        return telemetryEventPublisher.publish(command);
    }

    /**
     * Persists telemetry data received from Kafka consumer.
     *
     * This method is called by {@link com.powerassetintelligence.infrastructure.messaging.kafka.TelemetryKafkaConsumer}
     * after receiving and validating a telemetry message from Kafka. It performs
     * idempotent persistence (by externalTelemetryId) and asset status validation.
     *
     * Semantics: "telemetry persisted to database"
     *
     * @param request the original HTTP request (preserved for Kafka consumer compatibility)
     * @return persisted telemetry response
     */
    @Transactional
    public TelemetryResponse persist(TelemetryCreateCommand request) {
        if (request.externalTelemetryId() != null) {
            return telemetryRecordRepository.findByExternalTelemetryId(request.externalTelemetryId())
                    .map(this::toResponse)
                    .orElseGet(() -> persistNewRecord(request));
        }
        return persistNewRecord(request);
    }

    private TelemetryResponse persistNewRecord(TelemetryCreateCommand request) {
        Asset asset = assetService.getAsset(request.assetId());
        if (asset.getStatus() == AssetStatus.DECOMMISSIONED) {
            throw new BusinessValidationException("Decommissioned assets cannot accept operational telemetry: " + asset.getId());
        }

        TelemetryRecord telemetryRecord = new TelemetryRecord(
                UUID.randomUUID(),
                asset.getId(),
                request.timestamp(),
                request.temperatureCelsius(),
                request.loadPercent(),
                request.voltageKv(),
                request.currentAmpere(),
                request.vibrationMmSec(),
                request.overheatingCount(),
                request.sourceSensorId(),
                request.externalTelemetryId()
        );

        return toResponse(telemetryRecordRepository.save(telemetryRecord));
    }

    public PageResult<TelemetryResponse> findByAsset(UUID assetId, PageRequest pageRequest) {
        assetService.getAsset(assetId);
        var result = telemetryRecordRepository.findByAssetId(assetId, pageRequest);
        var content = result.content().stream().map(this::toResponse).toList();
        return new PageResult<>(content, result.page(), result.size(), result.totalElements(), result.totalPages());
    }

    public TelemetryResponse getLatest(UUID assetId) {
        assetService.getAsset(assetId);
        return telemetryRecordRepository.findFirstByAssetIdOrderByTimestampDesc(assetId)
                .map(this::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Telemetry not found for asset: " + assetId));
    }

    private TelemetryResponse toResponse(TelemetryRecord record) {
        return new TelemetryResponse(
                record.id(),
                record.assetId(),
                record.timestamp(),
                record.temperatureCelsius(),
                record.loadPercent(),
                record.voltageKv(),
                record.currentAmpere(),
                record.vibrationMmSec(),
                record.overheatingCount(),
                record.sourceSensorId(),
                record.externalTelemetryId(),
                record.createdAt()
        );
    }
}
