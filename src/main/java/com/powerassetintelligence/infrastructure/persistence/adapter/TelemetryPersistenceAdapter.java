package com.powerassetintelligence.infrastructure.persistence.adapter;

import com.powerassetintelligence.application.port.out.PageRequest;
import com.powerassetintelligence.application.port.out.PageResult;
import com.powerassetintelligence.application.port.out.TelemetryRepositoryPort;
import com.powerassetintelligence.domain.model.TelemetryRecord;
import com.powerassetintelligence.infrastructure.persistence.entity.AssetEntity;
import com.powerassetintelligence.infrastructure.persistence.mapper.PersistenceMapper;
import com.powerassetintelligence.infrastructure.persistence.repository.AssetRepository;
import com.powerassetintelligence.infrastructure.persistence.repository.TelemetryRecordRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

@Component
public class TelemetryPersistenceAdapter implements TelemetryRepositoryPort {
    private final TelemetryRecordRepository repository;
    private final AssetRepository assetRepository;

    @PersistenceContext
    private EntityManager entityManager;

    public TelemetryPersistenceAdapter(TelemetryRecordRepository repository, AssetRepository assetRepository) {
        this.repository = repository;
        this.assetRepository = assetRepository;
    }

    @Override
    public TelemetryRecord save(TelemetryRecord record) {
        AssetEntity asset = assetRepository.findById(record.assetId())
                .orElseThrow(() -> new IllegalStateException(
                        "Asset not found: " + record.assetId()));

        var entity = new com.powerassetintelligence.infrastructure.persistence.entity.TelemetryRecord(
                record.id(), asset,
                record.timestamp(), record.temperatureCelsius(), record.loadPercent(), record.voltageKv(), record.currentAmpere(),
                record.vibrationMmSec(), record.overheatingCount(), record.sourceSensorId(), record.externalTelemetryId());

        try {
            return PersistenceMapper.toDomain(repository.save(entity));
        } catch (DataIntegrityViolationException e) {
            // TOCTOU race: another thread committed a duplicate first.
            // Flush to sync the failed insert, then retry the lookup.
            entityManager.flush();

            if (record.externalTelemetryId() != null) {
                return repository.findByExternalTelemetryId(record.externalTelemetryId())
                        .map(PersistenceMapper::toDomain)
                        .orElseThrow(() -> e);
            }
            // For null externalTelemetryId — no idempotency key to look up by.
            // This shouldn't happen for the unique constraint, but rethrow as a safety net.
            throw e;
        }
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
