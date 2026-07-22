package com.powerassetintelligence.infrastructure.web.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Positive;
import java.time.LocalDate;
import java.util.Map;

public record AssetUpdateRequest(
        @Pattern(regexp = "TRANSFORMER|SUBSTATION|CIRCUIT_BREAKER|OVERHEAD_LINE|CABLE_LINE|SWITCHGEAR|SENSOR")
        String type,

        @Size(max = 255)
        String name,

        @Past
        LocalDate installationDate,

        String status,

        @Size(max = 255)
        String location,

        @Size(max = 255)
        String manufacturer,

        @Pattern(regexp = "LOW|MEDIUM|HIGH|CRITICAL")
        String criticality,

        @Positive
        Integer expectedServiceLifeYears,

        Map<String, String> technicalParameters
) {
}
