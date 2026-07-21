package com.powerassetintelligence.infrastructure.persistence.adapter;

import com.powerassetintelligence.application.port.out.TelemetryRepositoryPort;
import com.powerassetintelligence.domain.model.Asset;
import com.powerassetintelligence.domain.model.TelemetryRecord;
import com.powerassetintelligence.infrastructure.persistence.mapper.PersistenceMapper;
import com.powerassetintelligence.infrastructure.persistence.repository.AssetRepository;
import com.powerassetintelligence.infrastructure.persistence.repository.TelemetryRecordRepository;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

@Component
public class TelemetryPersistenceAdapter implements TelemetryRepositoryPort {
    private final TelemetryRecordRepository repository;
    private final AssetRepository assetRepository;
    public TelemetryPersistenceAdapter(TelemetryRecordRepository repository, AssetRepository assetRepository) { this.repository = repository; this.assetRepository = assetRepository; }
    @Override public TelemetryRecord save(TelemetryRecord record, Asset asset) {
        var assetEntity = assetRepository.findById(asset.getId()).orElseThrow();
        assetEntity.setType(asset.getType()); assetEntity.setName(asset.getName()); assetEntity.setInstallationDate(asset.getInstallationDate());
        assetEntity.setStatus(asset.getStatus()); assetEntity.setLocation(asset.getLocation()); assetEntity.setManufacturer(asset.getManufacturer());
        assetEntity.setCriticality(asset.getCriticality()); assetEntity.setExpectedServiceLifeYears(asset.getExpectedServiceLifeYears());
        assetEntity.setTechnicalParameters(asset.getTechnicalParameters());
        var entity = new com.powerassetintelligence.infrastructure.persistence.entity.TelemetryRecord(record.id(), assetEntity,
                record.timestamp(), record.temperatureCelsius(), record.loadPercent(), record.voltageKv(), record.currentAmpere(),
                record.vibrationMmSec(), record.overheatingCount(), record.sourceSensorId(), record.externalTelemetryId());
        return PersistenceMapper.toDomain(repository.save(entity));
    }
    @Override public Optional<TelemetryRecord> findByExternalTelemetryId(String externalTelemetryId) { return repository.findByExternalTelemetryId(externalTelemetryId).map(PersistenceMapper::toDomain); }
    @Override public Page<TelemetryRecord> findByAssetId(UUID assetId, Pageable pageable) { return repository.findByAssetId(assetId, pageable).map(PersistenceMapper::toDomain); }
    @Override public Optional<TelemetryRecord> findFirstByAssetIdOrderByTimestampDesc(UUID assetId) { return repository.findFirstByAssetIdOrderByTimestampDesc(assetId).map(PersistenceMapper::toDomain); }
}
