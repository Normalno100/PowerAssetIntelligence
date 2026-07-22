package com.powerassetintelligence.application.service;

import com.powerassetintelligence.infrastructure.web.dto.AssetCreateRequest;
import com.powerassetintelligence.application.dto.AssetResponse;
import com.powerassetintelligence.infrastructure.web.dto.AssetUpdateRequest;
import com.powerassetintelligence.domain.model.AssetCriticality;
import com.powerassetintelligence.domain.model.AssetStatus;
import com.powerassetintelligence.domain.model.AssetType;
import com.powerassetintelligence.domain.model.Asset;
import com.powerassetintelligence.application.port.out.AssetRepositoryPort;
import com.powerassetintelligence.application.port.out.PageRequest;
import com.powerassetintelligence.application.port.out.PageResult;
import java.util.Map;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class AssetService {

    private final AssetRepositoryPort assetRepository;

    public AssetService(AssetRepositoryPort assetRepository) {
        this.assetRepository = assetRepository;
    }

    @Transactional
    public AssetResponse create(AssetCreateRequest request) {
        Asset asset = new Asset(
                UUID.randomUUID(),
                parseAssetType(request.type()),
                request.name() != null ? request.name().trim() : null,
                request.installationDate(),
                AssetStatus.ACTIVE,
                request.location() != null ? request.location().trim() : null,
                request.manufacturer() != null ? request.manufacturer().trim() : null,
                parseAssetCriticality(request.criticality()),
                request.expectedServiceLifeYears(),
                nullToEmpty(request.technicalParameters())
        );
        return toResponse(assetRepository.save(asset));
    }

    public PageResult<AssetResponse> search(
            AssetType type,
            AssetStatus status,
            AssetCriticality criticality,
            String location,
            PageRequest pageRequest
    ) {
        var result = assetRepository.search(type, status, criticality, blankToNull(location), pageRequest);
        var content = result.content().stream().map(this::toResponse).toList();
        return new PageResult<>(content, result.page(), result.size(), result.totalElements(), result.totalPages());
    }

    public AssetResponse getById(UUID assetId) {
        return toResponse(getAsset(assetId));
    }

    @Transactional
    public AssetResponse update(UUID assetId, AssetUpdateRequest request) {
        Asset asset = getAsset(assetId);
        if (request.type() != null) {
            asset.setType(parseAssetType(request.type()));
        }
        if (request.name() != null) {
            asset.setName(request.name().trim());
        }
        if (request.installationDate() != null) {
            asset.setInstallationDate(request.installationDate());
        }
        if (request.status() != null) {
            asset.setStatus(parseAssetStatus(request.status()));
        }
        if (request.location() != null) {
            asset.setLocation(request.location().trim());
        }
        if (request.manufacturer() != null) {
            asset.setManufacturer(request.manufacturer().trim());
        }
        if (request.criticality() != null) {
            asset.setCriticality(parseAssetCriticality(request.criticality()));
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

    private AssetType parseAssetType(String value) {
        return AssetType.valueOf(value.toUpperCase());
    }

    private AssetCriticality parseAssetCriticality(String value) {
        return AssetCriticality.valueOf(value.toUpperCase());
    }

    private AssetStatus parseAssetStatus(String value) {
        return AssetStatus.valueOf(value.toUpperCase());
    }
}
