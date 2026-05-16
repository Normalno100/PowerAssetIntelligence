package com.powerassetintelligence.application.dto;

import com.powerassetintelligence.domain.model.MaintenanceType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record MaintenanceCreateRequest(
        @NotNull @PastOrPresent LocalDate repairDate,
        @NotNull MaintenanceType maintenanceType,
        @NotBlank @Size(max = 2_000) String description,
        @NotNull @DecimalMin("0.00") BigDecimal repairCost,
        @Size(max = 64) String failureCode,
        @NotBlank @Size(max = 255) String performedBy,
        List<@NotBlank @Size(max = 255) String> replacedComponents
) {
}
