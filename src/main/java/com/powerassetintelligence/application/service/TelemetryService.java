package com.powerassetintelligence.application.service;

import com.powerassetintelligence.application.dto.TelemetryCreateRequest;
import com.powerassetintelligence.application.dto.TelemetryResponse;
import com.powerassetintelligence.domain.model.AssetStatus;
import com.powerassetintelligence.domain.model.Asset;
import com.powerassetintelligence.domain.model.TelemetryRecord;
import com.powerassetintelligence.application.port.out.TelemetryRepositoryPort;
import com.powerassetintelligence.application.port.out.PageRequest;
import com.powerassetintelligence.application.port.out.PageResult;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class TelemetryService {

    private final AssetService assetService;
    private final TelemetryRepositoryPort telemetryRecordRepository;

    public TelemetryService(AssetService assetService, TelemetryRepositoryPort telemetryRecordRepository) {
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
                asset.getId(),
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
