package com.powerassetintelligence.application.service;

import com.powerassetintelligence.application.dto.AssetCreateCommand;
import com.powerassetintelligence.application.dto.AssetResponse;
import com.powerassetintelligence.application.dto.AssetUpdateCommand;
import com.powerassetintelligence.domain.model.Asset;
import com.powerassetintelligence.domain.model.AssetCriticality;
import com.powerassetintelligence.domain.model.AssetStatus;
import com.powerassetintelligence.domain.model.AssetType;
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
    public AssetResponse create(AssetCreateCommand command) {
        Asset asset = new Asset(
                UUID.randomUUID(),
                command.type(),
                command.name() != null ? command.name().trim() : null,
                command.installationDate(),
                AssetStatus.ACTIVE,
                command.location() != null ? command.location().trim() : null,
                command.manufacturer() != null ? command.manufacturer().trim() : null,
                command.criticality(),
                command.expectedServiceLifeYears(),
                nullToEmpty(command.technicalParameters())
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
    public AssetResponse update(UUID assetId, AssetUpdateCommand command) {
        Asset asset = getAsset(assetId);

        if (command.hasType()) {
            asset.setType(command.type());
        }
        if (command.hasName()) {
            asset.setName(command.trim(command.name()));
        }
        if (command.hasInstallationDate()) {
            asset.setInstallationDate(command.installationDate());
        }
        if (command.hasStatus()) {
            asset.setStatus(command.status());
        }
        if (command.hasLocation()) {
            asset.setLocation(command.trim(command.location()));
        }
        if (command.hasManufacturer()) {
            asset.setManufacturer(command.trim(command.manufacturer()));
        }
        if (command.hasCriticality()) {
            asset.setCriticality(command.criticality());
        }
        if (command.hasExpectedServiceLifeYears()) {
            asset.setExpectedServiceLifeYears(command.expectedServiceLifeYears());
        }
        if (command.hasTechnicalParameters()) {
            asset.setTechnicalParameters(command.technicalParameters());
        }

        return toResponse(assetRepository.save(asset));
    }

    @Transactional
    public AssetResponse decommission(UUID assetId) {
        Asset asset = getAsset(assetId);
        asset.setStatus(AssetStatus.DECOMMISSIONED);
        return toResponse(assetRepository.save(asset));
    }

    public Asset getAsset(UUID assetId) {
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
