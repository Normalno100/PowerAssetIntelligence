package com.powerassetintelligence.infrastructure.web.dto;

import com.powerassetintelligence.domain.model.AssetType;
import com.powerassetintelligence.domain.model.AssetCriticality;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.Map;

public record AssetCreateRequest(
        @NotNull AssetType type,

        @NotNull @Size(min = 1, max = 255)
        String name,

        @NotNull
        LocalDate installationDate,

        @NotNull @Size(max = 255)
        String location,

        @NotNull @Size(max = 255)
        String manufacturer,

        @NotNull AssetCriticality criticality,

        Integer expectedServiceLifeYears,
        Map<String, String> technicalParameters
) {
}
