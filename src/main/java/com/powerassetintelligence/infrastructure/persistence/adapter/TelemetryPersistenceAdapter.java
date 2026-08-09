package com.powerassetintelligence.infrastructure.persistence.adapter;

import com.powerassetintelligence.application.port.out.PageRequest;
import com.powerassetintelligence.application.port.out.PageResult;
import com.powerassetintelligence.application.port.out.TelemetryRepositoryPort;
import com.powerassetintelligence.domain.model.TelemetryRecord;
import com.powerassetintelligence.infrastructure.persistence.mapper.PersistenceMapper;
import com.powerassetintelligence.infrastructure.persistence.repository.TelemetryRecordRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class TelemetryPersistenceAdapter implements TelemetryRepositoryPort {
    private final TelemetryRecordRepository repository;
    public TelemetryPersistenceAdapter(TelemetryRecordRepository repository) { this.repository = repository; }
    @Override public TelemetryRecord save(TelemetryRecord record) {
        var entity = new com.powerassetintelligence.infrastructure.persistence.entity.TelemetryRecord(record.id(), null,
                record.timestamp(), record.temperatureCelsius(), record.loadPercent(), record.voltageKv(), record.currentAmpere(),
                record.vibrationMmSec(), record.overheatingCount(), record.sourceSensorId(), record.externalTelemetryId());
        return PersistenceMapper.toDomain(repository.save(entity));
    }
    @Override public Optional<TelemetryRecord> findByExternalTelemetryId(String externalTelemetryId) { return repository.findByExternalTelemetryId(externalTelemetryId).map(PersistenceMapper::toDomain); }
    @Override public PageResult<TelemetryRecord> findByAssetId(UUID assetId, PageRequest pageRequest) {
        var springPageable = PersistenceMapper.toSpringPageable(pageRequest);
        var result = repository.findByAssetId(assetId, springPageable);
        return new PageResult<>(result.getContent().stream().map(PersistenceMapper::toDomain).toList(),
                result.getNumber(), result.getSize(), result.getTotalElements(), result.getTotalPages());
    }
    @Override public Optional<TelemetryRecord> findFirstByAssetIdOrderByTimestampDesc(UUID assetId) { return repository.findFirstByAssetIdOrderByTimestampDesc(assetId).map(PersistenceMapper::toDomain); }
    @Override public List<TelemetryRecord> findByAssetIdAndTimestampRange(UUID assetId, Instant from, Instant to) {
        return repository.findByAssetIdAndTimestampBetween(assetId, from, to).stream()
                .map(PersistenceMapper::toDomain)
                .toList();
    }
}
