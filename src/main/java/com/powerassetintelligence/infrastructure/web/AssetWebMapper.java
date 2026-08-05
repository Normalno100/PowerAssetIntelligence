package com.powerassetintelligence.infrastructure.web;

import com.powerassetintelligence.application.dto.AssetCreateCommand;
import com.powerassetintelligence.application.dto.AssetUpdateCommand;
import com.powerassetintelligence.infrastructure.web.dto.AssetCreateRequest;
import com.powerassetintelligence.infrastructure.web.dto.AssetUpdateRequest;

public final class AssetWebMapper {

    private AssetWebMapper() {
    }

    public static AssetCreateCommand toCreateCommand(AssetCreateRequest request) {
        return new AssetCreateCommand(
                request.type(),
                request.name(),
                request.installationDate(),
                request.location(),
                request.manufacturer(),
                request.criticality(),
                request.expectedServiceLifeYears(),
                request.technicalParameters()
        );
    }

    public static AssetUpdateCommand toUpdateCommand(AssetUpdateRequest request) {
        return new AssetUpdateCommand(
                request.type(),
                request.name(),
                request.installationDate(),
                request.status(),
                request.location(),
                request.manufacturer(),
                request.criticality(),
                request.expectedServiceLifeYears(),
                request.technicalParameters()
        );
    }
}
