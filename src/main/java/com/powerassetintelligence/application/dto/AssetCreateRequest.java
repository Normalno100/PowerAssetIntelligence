package com.powerassetintelligence.application.dto;

import com.powerassetintelligence.domain.model.AssetCriticality;
import com.powerassetintelligence.domain.model.AssetType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.Map;

public record AssetCreateRequest(
        @NotNull AssetType type,
        @NotBlank @Size(max = 255) String name,
        @NotNull @PastOrPresent LocalDate installationDate,
        @NotBlank @Size(max = 512) String location,
        @NotBlank @Size(max = 255) String manufacturer,
        @NotNull AssetCriticality criticality,
        @NotNull @Min(1) Integer expectedServiceLifeYears,
        Map<@NotBlank @Size(max = 128) String, @Size(max = 512) String> technicalParameters
) {
}
