package com.powerassetintelligence.infrastructure.persistence.repository;

import com.powerassetintelligence.infrastructure.persistence.entity.TelemetryRecord;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TelemetryRecordRepository extends JpaRepository<TelemetryRecord, UUID> {

    Page<TelemetryRecord> findByAssetId(UUID assetId, Pageable pageable);

    Optional<TelemetryRecord> findFirstByAssetIdOrderByTimestampDesc(UUID assetId);

    boolean existsByExternalTelemetryId(String externalTelemetryId);

    Optional<TelemetryRecord> findByExternalTelemetryId(String externalTelemetryId);
}
