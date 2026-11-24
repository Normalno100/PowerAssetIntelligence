package com.powerassetintelligence.application.service;

import com.powerassetintelligence.application.dto.TelemetryAcceptedResponse;
import com.powerassetintelligence.application.dto.TelemetryCreateCommand;
import com.powerassetintelligence.application.dto.TelemetryResponse;
import com.powerassetintelligence.application.port.in.IngestTelemetryUseCase;
import com.powerassetintelligence.application.port.out.AssetRepositoryPort;
import com.powerassetintelligence.application.port.out.TelemetryEventPublisher;
import com.powerassetintelligence.application.port.out.TelemetryRepositoryPort;
import com.powerassetintelligence.application.port.out.PageRequest;
import com.powerassetintelligence.application.port.out.PageResult;
import com.powerassetintelligence.domain.model.AssetStatus;
import com.powerassetintelligence.domain.model.Asset;
import com.powerassetintelligence.domain.model.TelemetryRecord;
import com.powerassetintelligence.application.exception.BusinessValidationException;
import com.powerassetintelligence.application.exception.ResourceNotFoundException;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

@Service
@Transactional(readOnly = true)
public class TelemetryService implements IngestTelemetryUseCase {

    private final AssetRepositoryPort assetRepository;
    private final TelemetryRepositoryPort telemetryRecordRepository;
    private final TelemetryEventPublisher telemetryEventPublisher;
    private final TransactionTemplate transactionTemplate;

    public TelemetryService(
            AssetRepositoryPort assetRepository,
            TelemetryRepositoryPort telemetryRecordRepository,
            TelemetryEventPublisher telemetryEventPublisher,
            TransactionTemplate transactionTemplate
    ) {
        this.assetRepository = assetRepository;
        this.telemetryRecordRepository = telemetryRecordRepository;
        this.telemetryEventPublisher = telemetryEventPublisher;
        this.transactionTemplate = transactionTemplate;
    }

    @Override
    public TelemetryAcceptedResponse execute(TelemetryCreateCommand command) {
        return ingest(command);
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
     * Delegates to {@link #persistWithRetry} which handles TOCTOU race condition
     * by retrying in fresh transactions.
     *
     * Semantics: "telemetry persisted to database"
     *
     * @param request the original HTTP request (preserved for Kafka consumer compatibility)
     * @return persisted telemetry response
     */
    public TelemetryResponse persist(TelemetryCreateCommand request) {
        if (request.externalTelemetryId() != null) {
            return persistWithRetry(request);
        }
        // No externalTelemetryId — no idempotency needed, just persist directly
        return persistNewRecord(request);
    }

    /**
     * Entry point for persist with retry — handles TOCTOU race at the transactional boundary.
     * Each retry uses TransactionTemplate to guarantee a FRESH transaction,
     * avoiding the "rollbackOnly" problem where a failed INSERT marks the transaction
     * for rollback, making subsequent lookups in the same transaction silently fail.
     */
    public TelemetryResponse persistWithRetry(TelemetryCreateCommand request) {
        for (int attempt = 0; attempt < 20; attempt++) {
            try {
                // Each call to execute() gets a FRESH transaction via TransactionTemplate
                return transactionTemplate.execute(status -> doPersist(request));
            } catch (Throwable e) {
                // Transaction rolled back due to unique constraint violation.
                // Retry in a fresh transaction.
                if (attempt < 19) {
                    try {
                        Thread.sleep(200L * (attempt + 1));
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException("Interrupted during TOCTOU retry", ie);
                    }
                } else {
                    throw new RuntimeException(
                            "Failed to persist telemetry after 20 retries: " + e.getMessage(), e);
                }
            }
        }
        throw new RuntimeException("Should not reach here");
    }

    /**
     * Core persist logic — called from persistWithRetry inside a transaction.
     */
    private TelemetryResponse doPersist(TelemetryCreateCommand request) {
        if (request.externalTelemetryId() != null) {
            return telemetryRecordRepository.findByExternalTelemetryId(request.externalTelemetryId())
                    .map(this::toResponse)
                    .orElseGet(() -> persistNewRecord(request));
        }
        return persistNewRecord(request);
    }

    /**
     * Persist with retry for TOCTOU race condition.
     * When two threads insert the same externalTelemetryId simultaneously,
     * the losing thread's INSERT fails with DataIntegrityViolationException.
     * This method retries the whole check-then-act up to 20 times with
     * exponential backoff to allow the winning thread to commit.
     */
    private TelemetryResponse persistNewRecordWithRetry(TelemetryCreateCommand request) {
        for (int attempt = 0; attempt < 20; attempt++) {
            // Re-check: the winning thread may have committed since last attempt.
            Optional<com.powerassetintelligence.domain.model.TelemetryRecord> existing =
                    telemetryRecordRepository.findByExternalTelemetryId(request.externalTelemetryId());
            if (existing.isPresent()) {
                return toResponse(existing.get());
            }

            try {
                return persistNewRecord(request);
            } catch (Throwable e) {
                // Another thread still winning the race — retry after exponential backoff
                if (attempt < 19) {
                    try {
                        Thread.sleep(200L * (attempt + 1));
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException("Interrupted during TOCTOU retry", ie);
                    }
                } else {
                    // Final attempt failed — re-lookup one last time
                    return telemetryRecordRepository
                            .findByExternalTelemetryId(request.externalTelemetryId())
                            .map(this::toResponse)
                            .orElseThrow(() -> new RuntimeException(
                                    "Failed to persist telemetry after 20 retries: " + e.getMessage(), e));
                }
            }
        }
        // Should not reach here
        return telemetryRecordRepository
                .findByExternalTelemetryId(request.externalTelemetryId())
                .map(this::toResponse)
                .orElseThrow();
    }

    private TelemetryResponse persistNewRecord(TelemetryCreateCommand request) {
        Asset asset = assetRepository.findById(request.assetId())
                .orElseThrow(() -> new ResourceNotFoundException("Asset not found: " + request.assetId()));
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
        assetRepository.findById(assetId)
                .orElseThrow(() -> new ResourceNotFoundException("Asset not found: " + assetId));
        var result = telemetryRecordRepository.findByAssetId(assetId, pageRequest);
        var content = result.content().stream().map(this::toResponse).toList();
        return new PageResult<>(content, result.page(), result.size(), result.totalElements(), result.totalPages());
    }

    public TelemetryResponse getLatest(UUID assetId) {
        assetRepository.findById(assetId)
                .orElseThrow(() -> new ResourceNotFoundException("Asset not found: " + assetId));
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
