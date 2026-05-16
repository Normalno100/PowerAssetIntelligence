package com.powerassetintelligence.application.service;

import com.powerassetintelligence.application.dto.AssetCreateRequest;
import com.powerassetintelligence.application.dto.AssetResponse;
import com.powerassetintelligence.application.dto.AssetUpdateRequest;
import com.powerassetintelligence.domain.model.AssetCriticality;
import com.powerassetintelligence.domain.model.AssetStatus;
import com.powerassetintelligence.domain.model.AssetType;
import com.powerassetintelligence.infrastructure.persistence.entity.Asset;
import com.powerassetintelligence.infrastructure.persistence.repository.AssetRepository;
import java.util.Map;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class AssetService {

    private final AssetRepository assetRepository;

    public AssetService(AssetRepository assetRepository) {
        this.assetRepository = assetRepository;
    }

    @Transactional
    public AssetResponse create(AssetCreateRequest request) {
        Asset asset = new Asset(
                UUID.randomUUID(),
                request.type(),
                request.name().trim(),
                request.installationDate(),
                AssetStatus.ACTIVE,
                request.location().trim(),
                request.manufacturer().trim(),
                request.criticality(),
                request.expectedServiceLifeYears(),
                nullToEmpty(request.technicalParameters())
        );
        return toResponse(assetRepository.save(asset));
    }

    public Page<AssetResponse> search(
            AssetType type,
            AssetStatus status,
            AssetCriticality criticality,
            String location,
            Pageable pageable
    ) {
        return assetRepository.search(type, status, criticality, blankToNull(location), pageable)
                .map(this::toResponse);
    }

    public AssetResponse getById(UUID assetId) {
        return toResponse(getAsset(assetId));
    }

    @Transactional
    public AssetResponse update(UUID assetId, AssetUpdateRequest request) {
        Asset asset = getAsset(assetId);
        if (request.type() != null) {
            asset.setType(request.type());
        }
        if (request.name() != null) {
            asset.setName(request.name().trim());
        }
        if (request.installationDate() != null) {
            asset.setInstallationDate(request.installationDate());
        }
        if (request.status() != null) {
            asset.setStatus(request.status());
        }
        if (request.location() != null) {
            asset.setLocation(request.location().trim());
        }
        if (request.manufacturer() != null) {
            asset.setManufacturer(request.manufacturer().trim());
        }
        if (request.criticality() != null) {
            asset.setCriticality(request.criticality());
        }
        if (request.expectedServiceLifeYears() != null) {
            asset.setExpectedServiceLifeYears(request.expectedServiceLifeYears());
        }
        if (request.technicalParameters() != null) {
            asset.setTechnicalParameters(request.technicalParameters());
        }
        return toResponse(assetRepository.save(asset));
    }

    @Transactional
    public AssetResponse decommission(UUID assetId) {
        Asset asset = getAsset(assetId);
        asset.setStatus(AssetStatus.DECOMMISSIONED);
        return toResponse(assetRepository.save(asset));
    }

    Asset getAsset(UUID assetId) {
        return assetRepository.findById(assetId)
                .orElseThrow(() -> new ResourceNotFoundException("Asset not found: " + assetId));
    }

    private AssetResponse toResponse(Asset asset) {
        return new AssetResponse(
                asset.getId(),
                asset.getType(),
                asset.getName(),
                asset.getInstallationDate(),
                asset.getStatus(),
                asset.getLocation(),
                asset.getManufacturer(),
                asset.getCriticality(),
                asset.getExpectedServiceLifeYears(),
                Map.copyOf(asset.getTechnicalParameters()),
                asset.getVersion(),
                asset.getCreatedAt(),
                asset.getUpdatedAt()
        );
    }

    private Map<String, String> nullToEmpty(Map<String, String> value) {
        return value == null ? Map.of() : value;
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }
}
