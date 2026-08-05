package com.powerassetintelligence.application.dto;

import com.powerassetintelligence.domain.model.AssetStatus;
import com.powerassetintelligence.domain.model.AssetType;
import com.powerassetintelligence.domain.model.AssetCriticality;
import java.time.LocalDate;
import java.util.Map;

public record AssetUpdateCommand(
        AssetType type,
        String name,
        LocalDate installationDate,
        AssetStatus status,
        String location,
        String manufacturer,
        AssetCriticality criticality,
        Integer expectedServiceLifeYears,
        Map<String, String> technicalParameters
) {
    public boolean hasType() {
        return type != null;
    }

    public boolean hasName() {
        return name != null && !name.isBlank();
    }

    public boolean hasInstallationDate() {
        return installationDate != null;
    }

    public boolean hasStatus() {
        return status != null;
    }

    public boolean hasLocation() {
        return location != null && !location.isBlank();
    }

    public boolean hasManufacturer() {
        return manufacturer != null && !manufacturer.isBlank();
    }

    public boolean hasCriticality() {
        return criticality != null;
    }

    public boolean hasExpectedServiceLifeYears() {
        return expectedServiceLifeYears != null;
    }

    public boolean hasTechnicalParameters() {
        return technicalParameters != null;
    }

    public String trim(String value) {
        return value != null ? value.trim() : null;
    }
}
