package com.powerassetintelligence.infrastructure.persistence.repository;

import com.powerassetintelligence.infrastructure.persistence.entity.TelemetryRecord;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TelemetryRecordRepository extends JpaRepository<TelemetryRecord, UUID> {

    Page<TelemetryRecord> findByAssetId(UUID assetId, Pageable pageable);

    Optional<TelemetryRecord> findFirstByAssetIdOrderByTimestampDesc(UUID assetId);

    boolean existsByExternalTelemetryId(String externalTelemetryId);

    Optional<TelemetryRecord> findByExternalTelemetryId(String externalTelemetryId);

    @Query("SELECT t FROM TelemetryRecord t WHERE t.asset.id = :assetId AND t.timestamp BETWEEN :from AND :to ORDER BY t.timestamp ASC")
    List<TelemetryRecord> findByAssetIdAndTimestampBetween(@Param("assetId") UUID assetId, @Param("from") Instant from, @Param("to") Instant to);
}
