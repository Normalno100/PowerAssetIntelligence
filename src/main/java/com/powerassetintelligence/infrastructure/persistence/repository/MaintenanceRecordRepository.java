package com.powerassetintelligence.infrastructure.persistence.repository;

import com.powerassetintelligence.infrastructure.persistence.entity.MaintenanceRecord;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MaintenanceRecordRepository extends JpaRepository<MaintenanceRecord, UUID> {

    Page<MaintenanceRecord> findByAssetId(UUID assetId, Pageable pageable);

    long countByAssetIdAndRepairDateGreaterThanEqual(UUID assetId, LocalDate fromDate);
}
