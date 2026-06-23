package com.powerassetintelligence.application.port.out;

import com.powerassetintelligence.domain.model.Asset;
import com.powerassetintelligence.domain.model.AssetCriticality;
import com.powerassetintelligence.domain.model.AssetStatus;
import com.powerassetintelligence.domain.model.AssetType;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface AssetRepositoryPort {
    Asset save(Asset asset);
    Optional<Asset> findById(UUID assetId);
    Page<Asset> search(AssetType type, AssetStatus status, AssetCriticality criticality, String location, Pageable pageable);
}
