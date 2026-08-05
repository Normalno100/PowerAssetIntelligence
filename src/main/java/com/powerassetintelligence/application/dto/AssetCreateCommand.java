package com.powerassetintelligence.application.dto;

import com.powerassetintelligence.domain.model.AssetType;
import com.powerassetintelligence.domain.model.AssetCriticality;
import java.time.LocalDate;
import java.util.Map;

public record AssetCreateCommand(
        AssetType type,
        String name,
        LocalDate installationDate,
        String location,
        String manufacturer,
        AssetCriticality criticality,
        Integer expectedServiceLifeYears,
        Map<String, String> technicalParameters
) {
}
