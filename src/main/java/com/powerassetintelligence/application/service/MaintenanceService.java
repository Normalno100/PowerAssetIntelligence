package com.powerassetintelligence.application.service;

import com.powerassetintelligence.application.dto.MaintenanceCreateCommand;
import com.powerassetintelligence.application.dto.MaintenanceResponse;
import com.powerassetintelligence.domain.model.Asset;
import com.powerassetintelligence.domain.model.MaintenanceRecord;
import com.powerassetintelligence.application.port.out.AssetRepositoryPort;
import com.powerassetintelligence.application.port.out.MaintenanceRepositoryPort;
import com.powerassetintelligence.application.port.out.PageRequest;
import com.powerassetintelligence.application.port.out.PageResult;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class MaintenanceService {

    private final AssetRepositoryPort assetRepositoryPort;
    private final MaintenanceRepositoryPort maintenanceRecordRepository;

    public MaintenanceService(AssetRepositoryPort assetRepositoryPort, MaintenanceRepositoryPort maintenanceRecordRepository) {
        this.assetRepositoryPort = assetRepositoryPort;
        this.maintenanceRecordRepository = maintenanceRecordRepository;
    }

    @Transactional
    public MaintenanceResponse create(UUID assetId, MaintenanceCreateCommand command) {
        Asset asset = assetRepositoryPort.findById(assetId)
                .orElseThrow(() -> new ResourceNotFoundException("Asset not found: " + assetId));

        if (command.repairDate().isBefore(asset.getInstallationDate())) {
            throw new BusinessValidationException("Repair date cannot be before asset installation date");
        }

        asset.startMaintenance();
        assetRepositoryPort.save(asset);

        MaintenanceRecord record = new MaintenanceRecord(
                UUID.randomUUID(),
                asset.getId(),
                command.repairDate(),
                command.maintenanceType(),
                command.description(),
                command.repairCost(),
                command.failureCode(),
                command.performedBy(),
                command.replacedComponents()
        );

        maintenanceRecordRepository.save(record);

        return toResponse(record);
    }

    public PageResult<MaintenanceResponse> findByAsset(UUID assetId, PageRequest pageRequest) {
        assetRepositoryPort.findById(assetId)
                .orElseThrow(() -> new ResourceNotFoundException("Asset not found: " + assetId));

        var result = maintenanceRecordRepository.findByAssetId(assetId, pageRequest);
        var content = result.content().stream().map(this::toResponse).toList();
        return new PageResult<>(content, result.page(), result.size(), result.totalElements(), result.totalPages());
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
