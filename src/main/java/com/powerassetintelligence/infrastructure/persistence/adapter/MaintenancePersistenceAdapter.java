package com.powerassetintelligence.infrastructure.persistence.adapter;

import com.powerassetintelligence.application.port.out.MaintenanceRepositoryPort;
import com.powerassetintelligence.application.port.out.PageRequest;
import com.powerassetintelligence.application.port.out.PageResult;
import com.powerassetintelligence.domain.model.MaintenanceRecord;
import com.powerassetintelligence.infrastructure.persistence.mapper.PersistenceMapper;
import com.powerassetintelligence.infrastructure.persistence.repository.MaintenanceRecordRepository;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class MaintenancePersistenceAdapter implements MaintenanceRepositoryPort {
    private final MaintenanceRecordRepository repository;
    public MaintenancePersistenceAdapter(MaintenanceRecordRepository repository) { this.repository = repository; }
    @Override public MaintenanceRecord save(MaintenanceRecord record) {
        var entity = new com.powerassetintelligence.infrastructure.persistence.entity.MaintenanceRecord(record.id(), null,
                record.repairDate(), record.maintenanceType(), record.description(), record.repairCost(), record.failureCode(),
                record.performedBy(), record.replacedComponents());
        return PersistenceMapper.toDomain(repository.save(entity));
    }
    @Override public PageResult<MaintenanceRecord> findByAssetId(UUID assetId, PageRequest pageRequest) {
        var springPageable = PersistenceMapper.toSpringPageable(pageRequest);
        var result = repository.findByAssetId(assetId, springPageable);
        return new PageResult<>(result.getContent().stream().map(PersistenceMapper::toDomain).toList(),
                result.getNumber(), result.getSize(), result.getTotalElements(), result.getTotalPages());
    }
    @Override public long countByAssetIdAndRepairDateGreaterThanEqual(UUID assetId, LocalDate repairDate) { return repository.countByAssetIdAndRepairDateGreaterThanEqual(assetId, repairDate); }
}
