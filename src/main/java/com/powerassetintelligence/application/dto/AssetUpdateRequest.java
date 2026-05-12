package com.powerassetintelligence.application.dto;

import com.powerassetintelligence.domain.model.AssetCriticality;
import com.powerassetintelligence.domain.model.AssetStatus;
import com.powerassetintelligence.domain.model.AssetType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.Map;

public record AssetUpdateRequest(
        AssetType type,
        @Size(max = 255) String name,
        @PastOrPresent LocalDate installationDate,
        AssetStatus status,
        @Size(max = 512) String location,
        @Size(max = 255) String manufacturer,
        AssetCriticality criticality,
        @Min(1) Integer expectedServiceLifeYears,
        Map<@Size(max = 128) String, @Size(max = 512) String> technicalParameters
) {
}
