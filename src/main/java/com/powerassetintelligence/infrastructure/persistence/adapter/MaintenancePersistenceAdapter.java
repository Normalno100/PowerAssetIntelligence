package com.powerassetintelligence.infrastructure.persistence.adapter;

import com.powerassetintelligence.application.port.out.MaintenanceRepositoryPort;
import com.powerassetintelligence.domain.model.Asset;
import com.powerassetintelligence.domain.model.MaintenanceRecord;
import com.powerassetintelligence.infrastructure.persistence.mapper.PersistenceMapper;
import com.powerassetintelligence.infrastructure.persistence.repository.AssetRepository;
import com.powerassetintelligence.infrastructure.persistence.repository.MaintenanceRecordRepository;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

@Component
public class MaintenancePersistenceAdapter implements MaintenanceRepositoryPort {
    private final MaintenanceRecordRepository repository;
    private final AssetRepository assetRepository;
    public MaintenancePersistenceAdapter(MaintenanceRecordRepository repository, AssetRepository assetRepository) { this.repository = repository; this.assetRepository = assetRepository; }
    @Override public MaintenanceRecord save(MaintenanceRecord record, Asset asset) {
        var assetEntity = assetRepository.findById(asset.getId()).orElseThrow();
        assetEntity.setStatus(asset.getStatus());
        var entity = new com.powerassetintelligence.infrastructure.persistence.entity.MaintenanceRecord(record.id(), assetEntity,
                record.repairDate(), record.maintenanceType(), record.description(), record.repairCost(), record.failureCode(),
                record.performedBy(), record.replacedComponents());
        return PersistenceMapper.toDomain(repository.save(entity));
    }
    @Override public Page<MaintenanceRecord> findByAssetId(UUID assetId, Pageable pageable) { return repository.findByAssetId(assetId, pageable).map(PersistenceMapper::toDomain); }
    @Override public long countByAssetIdAndRepairDateGreaterThanEqual(UUID assetId, LocalDate repairDate) { return repository.countByAssetIdAndRepairDateGreaterThanEqual(assetId, repairDate); }
}
