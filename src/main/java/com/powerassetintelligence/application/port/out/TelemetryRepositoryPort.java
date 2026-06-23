package com.powerassetintelligence.application.port.out;

import com.powerassetintelligence.domain.model.Asset;
import com.powerassetintelligence.domain.model.TelemetryRecord;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface TelemetryRepositoryPort {
    TelemetryRecord save(TelemetryRecord telemetryRecord, Asset asset);
    Optional<TelemetryRecord> findByExternalTelemetryId(String externalTelemetryId);
    Page<TelemetryRecord> findByAssetId(UUID assetId, Pageable pageable);
    Optional<TelemetryRecord> findFirstByAssetIdOrderByTimestampDesc(UUID assetId);
}
