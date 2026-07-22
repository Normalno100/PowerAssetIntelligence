package com.powerassetintelligence.application.port.out;

import com.powerassetintelligence.domain.model.Asset;
import com.powerassetintelligence.domain.model.AssetCriticality;
import com.powerassetintelligence.domain.model.AssetStatus;
import com.powerassetintelligence.domain.model.AssetType;
import java.util.Optional;
import java.util.UUID;

public interface AssetRepositoryPort {
    Asset save(Asset asset);
    Optional<Asset> findById(UUID assetId);
    PageResult<Asset> search(AssetType type, AssetStatus status, AssetCriticality criticality, String location, PageRequest pageRequest);
}
