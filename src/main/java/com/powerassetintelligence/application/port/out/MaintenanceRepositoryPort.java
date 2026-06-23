package com.powerassetintelligence.application.port.out;

import com.powerassetintelligence.domain.model.Asset;
import com.powerassetintelligence.domain.model.MaintenanceRecord;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface MaintenanceRepositoryPort {
    MaintenanceRecord save(MaintenanceRecord maintenanceRecord, Asset asset);
    Page<MaintenanceRecord> findByAssetId(UUID assetId, Pageable pageable);
    long countByAssetIdAndRepairDateGreaterThanEqual(UUID assetId, LocalDate repairDate);
}
