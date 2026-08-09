package com.powerassetintelligence.application.port.out;

import com.powerassetintelligence.domain.model.TelemetryRecord;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TelemetryRepositoryPort {
    TelemetryRecord save(TelemetryRecord telemetryRecord);
    Optional<TelemetryRecord> findByExternalTelemetryId(String externalTelemetryId);
    PageResult<TelemetryRecord> findByAssetId(UUID assetId, PageRequest pageRequest);
    Optional<TelemetryRecord> findFirstByAssetIdOrderByTimestampDesc(UUID assetId);

    List<TelemetryRecord> findByAssetIdAndTimestampRange(UUID assetId, Instant from, Instant to);
}
