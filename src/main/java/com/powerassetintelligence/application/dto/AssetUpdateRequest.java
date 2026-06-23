package com.powerassetintelligence.application.dto;

import com.powerassetintelligence.domain.model.AssetCriticality;
import com.powerassetintelligence.domain.model.AssetStatus;
import com.powerassetintelligence.domain.model.AssetType;
import java.time.LocalDate;
import java.util.Map;

public record AssetUpdateRequest(
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
}
