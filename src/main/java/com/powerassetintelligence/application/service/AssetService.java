package com.powerassetintelligence.application.service;

import com.powerassetintelligence.application.dto.AssetCreateCommand;
import com.powerassetintelligence.application.dto.AssetResponse;
import com.powerassetintelligence.application.dto.AssetUpdateCommand;
import com.powerassetintelligence.domain.model.Asset;
import com.powerassetintelligence.domain.model.AssetCriticality;
import com.powerassetintelligence.domain.model.AssetStatus;
import com.powerassetintelligence.domain.model.AssetType;
import com.powerassetintelligence.application.port.in.RegisterAssetUseCase;
import com.powerassetintelligence.application.port.out.AssetRepositoryPort;
import com.powerassetintelligence.application.port.out.PageRequest;
import com.powerassetintelligence.application.port.out.PageResult;
import java.util.Map;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class AssetService implements RegisterAssetUseCase {

    private final AssetRepositoryPort assetRepository;

    public AssetService(AssetRepositoryPort assetRepository) {
        this.assetRepository = assetRepository;
    }

    @Override
    @Transactional
    public AssetResponse execute(AssetCreateCommand command) {
        return create(command);
    }

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
        asset.updateFields(
                command.hasName() ? command.trim(command.name()) : null,
                command.hasInstallationDate() ? command.installationDate() : null,
                command.hasStatus() ? command.status() : null,
                command.hasLocation() ? command.trim(command.location()) : null,
                command.hasManufacturer() ? command.trim(command.manufacturer()) : null,
                command.hasCriticality() ? command.criticality() : null,
                command.hasExpectedServiceLifeYears() ? command.expectedServiceLifeYears() : null,
                command.hasTechnicalParameters() ? command.technicalParameters() : null
        );
        return toResponse(assetRepository.save(asset));
    }

    @Transactional
    public AssetResponse decommission(UUID assetId) {
        Asset asset = getAsset(assetId);
        asset.decommission();
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
