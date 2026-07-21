package com.powerassetintelligence.infrastructure.persistence.adapter;

import com.powerassetintelligence.application.port.out.AssetRepositoryPort;
import com.powerassetintelligence.domain.model.Asset;
import com.powerassetintelligence.domain.model.AssetCriticality;
import com.powerassetintelligence.domain.model.AssetStatus;
import com.powerassetintelligence.domain.model.AssetType;
import com.powerassetintelligence.infrastructure.persistence.entity.AssetEntity;
import com.powerassetintelligence.infrastructure.persistence.mapper.PersistenceMapper;
import com.powerassetintelligence.infrastructure.persistence.repository.AssetRepository;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

@Component
public class AssetPersistenceAdapter implements AssetRepositoryPort {
    private final AssetRepository repository;

    public AssetPersistenceAdapter(AssetRepository repository) { this.repository = repository; }

    @Override
    public Asset save(Asset asset) {
        com.powerassetintelligence.infrastructure.persistence.entity.AssetEntity entity = repository.findById(asset.getId())
                .orElseGet(() -> new com.powerassetintelligence.infrastructure.persistence.entity.AssetEntity(asset.getId(),
                        asset.getType(), asset.getName(), asset.getInstallationDate(), asset.getStatus(),
                        asset.getLocation(), asset.getManufacturer(), asset.getCriticality(),
                        asset.getExpectedServiceLifeYears(), asset.getTechnicalParameters()));
        entity.setType(asset.getType()); entity.setName(asset.getName()); entity.setInstallationDate(asset.getInstallationDate());
        entity.setStatus(asset.getStatus()); entity.setLocation(asset.getLocation()); entity.setManufacturer(asset.getManufacturer());
        entity.setCriticality(asset.getCriticality()); entity.setExpectedServiceLifeYears(asset.getExpectedServiceLifeYears());
        entity.setTechnicalParameters(asset.getTechnicalParameters());
        return PersistenceMapper.toDomain(repository.save(entity));
    }

    @Override public Optional<Asset> findById(UUID assetId) { return repository.findById(assetId).map(PersistenceMapper::toDomain); }
    @Override public Page<Asset> search(AssetType type, AssetStatus status, AssetCriticality criticality, String location, Pageable pageable) {
        return repository.search(type, status, criticality, location, pageable)
                .map(entity -> PersistenceMapper.toDomain(entity));
    }
}
