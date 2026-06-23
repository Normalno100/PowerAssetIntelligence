package com.powerassetintelligence.application.service;

import com.powerassetintelligence.application.dto.MaintenanceCreateRequest;
import com.powerassetintelligence.application.dto.MaintenanceResponse;
import com.powerassetintelligence.domain.model.AssetStatus;
import com.powerassetintelligence.domain.model.Asset;
import com.powerassetintelligence.domain.model.MaintenanceRecord;
import com.powerassetintelligence.application.port.out.MaintenanceRepositoryPort;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class MaintenanceService {

    private final AssetService assetService;
    private final MaintenanceRepositoryPort maintenanceRecordRepository;

    public MaintenanceService(AssetService assetService, MaintenanceRepositoryPort maintenanceRecordRepository) {
        this.assetService = assetService;
        this.maintenanceRecordRepository = maintenanceRecordRepository;
    }

    @Transactional
    public MaintenanceResponse create(UUID assetId, MaintenanceCreateRequest request) {
        Asset asset = assetService.getAsset(assetId);
        if (request.repairDate().isBefore(asset.getInstallationDate())) {
            throw new BusinessValidationException("Repair date cannot be before asset installation date");
        }

        MaintenanceRecord record = new MaintenanceRecord(
                UUID.randomUUID(),
                asset.getId(),
                request.repairDate(),
                request.maintenanceType(),
                request.description().trim(),
                request.repairCost(),
                request.failureCode(),
                request.performedBy().trim(),
                request.replacedComponents()
        );

        if (asset.getStatus() != AssetStatus.DECOMMISSIONED) {
            asset.setStatus(AssetStatus.UNDER_MAINTENANCE);
        }

        return toResponse(maintenanceRecordRepository.save(record, asset));
    }

    public Page<MaintenanceResponse> findByAsset(UUID assetId, Pageable pageable) {
        assetService.getAsset(assetId);
        return maintenanceRecordRepository.findByAssetId(assetId, pageable).map(this::toResponse);
    }

    private MaintenanceResponse toResponse(MaintenanceRecord record) {
        return new MaintenanceResponse(
                record.id(),
                record.assetId(),
                record.repairDate(),
                record.maintenanceType(),
                record.description(),
                record.repairCost(),
                record.failureCode(),
                record.performedBy(),
                List.copyOf(record.replacedComponents()),
                record.createdAt()
        );
    }
}
