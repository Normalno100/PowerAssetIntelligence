package com.powerassetintelligence.infrastructure.web;

import com.powerassetintelligence.application.dto.MaintenanceCreateCommand;
import com.powerassetintelligence.infrastructure.web.dto.MaintenanceCreateRequest;

public final class MaintenanceWebMapper {

    private MaintenanceWebMapper() {
    }

    public static MaintenanceCreateCommand toCreateCommand(MaintenanceCreateRequest request) {
        return new MaintenanceCreateCommand(
                request.repairDate(),
                request.maintenanceType(),
                request.description() != null ? request.description().trim() : null,
                request.repairCost(),
                request.failureCode(),
                request.performedBy() != null ? request.performedBy().trim() : null,
                request.replacedComponents()
        );
    }
}
