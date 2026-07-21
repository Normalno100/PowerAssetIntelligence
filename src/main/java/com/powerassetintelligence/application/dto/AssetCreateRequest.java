package com.powerassetintelligence.application.dto;

import com.powerassetintelligence.domain.model.AssetCriticality;
import com.powerassetintelligence.domain.model.AssetType;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.Map;

public record AssetCreateRequest(
        @NotNull AssetType type,
        @NotNull String name,
        @NotNull LocalDate installationDate,
        @NotNull String location,
        @NotNull String manufacturer,
        @NotNull AssetCriticality criticality,
        Integer expectedServiceLifeYears,
        Map<String, String> technicalParameters
) {
}
