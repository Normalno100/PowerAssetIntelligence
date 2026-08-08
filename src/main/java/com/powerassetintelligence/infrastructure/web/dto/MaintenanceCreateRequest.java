package com.powerassetintelligence.infrastructure.web.dto;

import com.powerassetintelligence.domain.model.MaintenanceType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record MaintenanceCreateRequest(
        @NotNull(message = "Repair date is required")
        LocalDate repairDate,

        @NotNull(message = "Maintenance type is required")
        MaintenanceType maintenanceType,

        @NotNull(message = "Description is required")
        String description,

        @NotNull(message = "Repair cost is required")
        BigDecimal repairCost,

        String failureCode,

        @NotNull(message = "Performed by is required")
        String performedBy,

        @Valid
        List<String> replacedComponents
) {
}
