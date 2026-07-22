package com.powerassetintelligence.application.port.out;

import com.powerassetintelligence.domain.model.MaintenanceRecord;
import java.time.LocalDate;
import java.util.UUID;

public interface MaintenanceRepositoryPort {
    MaintenanceRecord save(MaintenanceRecord maintenanceRecord);
    PageResult<MaintenanceRecord> findByAssetId(UUID assetId, PageRequest pageRequest);
    long countByAssetIdAndRepairDateGreaterThanEqual(UUID assetId, LocalDate repairDate);
}
