package com.powerassetintelligence.application.service;

import com.powerassetintelligence.application.dto.TelemetryCreateRequest;
import com.powerassetintelligence.application.dto.TelemetryResponse;
import com.powerassetintelligence.domain.model.AssetStatus;
import com.powerassetintelligence.infrastructure.persistence.entity.Asset;
import com.powerassetintelligence.infrastructure.persistence.entity.TelemetryRecord;
import com.powerassetintelligence.infrastructure.persistence.repository.TelemetryRecordRepository;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class TelemetryService {

    private final AssetService assetService;
    private final TelemetryRecordRepository telemetryRecordRepository;

    public TelemetryService(AssetService assetService, TelemetryRecordRepository telemetryRecordRepository) {
        this.assetService = assetService;
        this.telemetryRecordRepository = telemetryRecordRepository;
    }

    @Transactional
    public TelemetryResponse persist(TelemetryCreateRequest request) {
        if (request.externalTelemetryId() != null) {
            return telemetryRecordRepository.findByExternalTelemetryId(request.externalTelemetryId())
                    .map(this::toResponse)
                    .orElseGet(() -> persistNewRecord(request));
        }
        return persistNewRecord(request);
    }

    private TelemetryResponse persistNewRecord(TelemetryCreateRequest request) {
        Asset asset = assetService.getAsset(request.assetId());
        if (asset.getStatus() == AssetStatus.DECOMMISSIONED) {
            throw new BusinessValidationException("Decommissioned assets cannot accept operational telemetry: " + asset.getId());
        }

        TelemetryRecord telemetryRecord = new TelemetryRecord(
                UUID.randomUUID(),
                asset,
                request.timestamp(),
                request.temperatureCelsius(),
                request.loadPercent(),
                request.voltageKv(),
                request.currentAmpere(),
                request.vibrationMmSec(),
                request.overheatingCount(),
                request.sourceSensorId().trim(),
                request.externalTelemetryId()
        );

        return toResponse(telemetryRecordRepository.save(telemetryRecord));
    }

    public Page<TelemetryResponse> findByAsset(UUID assetId, Pageable pageable) {
        assetService.getAsset(assetId);
        return telemetryRecordRepository.findByAssetId(assetId, pageable).map(this::toResponse);
    }

    public TelemetryResponse getLatest(UUID assetId) {
        assetService.getAsset(assetId);
        return telemetryRecordRepository.findFirstByAssetIdOrderByTimestampDesc(assetId)
                .map(this::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Telemetry not found for asset: " + assetId));
    }

    private TelemetryResponse toResponse(TelemetryRecord record) {
        return new TelemetryResponse(
                record.getId(),
                record.getAsset().getId(),
                record.getTimestamp(),
                record.getTemperatureCelsius(),
                record.getLoadPercent(),
                record.getVoltageKv(),
                record.getCurrentAmpere(),
                record.getVibrationMmSec(),
                record.getOverheatingCount(),
                record.getSourceSensorId(),
                record.getExternalTelemetryId(),
                record.getCreatedAt()
        );
    }
}
