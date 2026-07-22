package com.powerassetintelligence.infrastructure.web.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.Map;

public record AssetCreateRequest(
        @NotNull @Pattern(regexp = "TRANSFORMER|SUBSTATION|CIRCUIT_BREAKER|OVERHEAD_LINE|CABLE_LINE|SWITCHGEAR|SENSOR")
        String type,

        @NotNull @Size(min = 1, max = 255)
        String name,

        @NotNull
        LocalDate installationDate,

        @NotNull @Size(max = 255)
        String location,

        @NotNull @Size(max = 255)
        String manufacturer,

        @NotNull @Pattern(regexp = "LOW|MEDIUM|HIGH|CRITICAL")
        String criticality,

        Integer expectedServiceLifeYears,
        Map<String, String> technicalParameters
) {
}
