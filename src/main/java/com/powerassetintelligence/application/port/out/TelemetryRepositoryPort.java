package com.powerassetintelligence.application.port.out;

import com.powerassetintelligence.domain.model.TelemetryRecord;
import java.util.Optional;
import java.util.UUID;

public interface TelemetryRepositoryPort {
    TelemetryRecord save(TelemetryRecord telemetryRecord);
    Optional<TelemetryRecord> findByExternalTelemetryId(String externalTelemetryId);
    PageResult<TelemetryRecord> findByAssetId(UUID assetId, PageRequest pageRequest);
    Optional<TelemetryRecord> findFirstByAssetIdOrderByTimestampDesc(UUID assetId);
}
