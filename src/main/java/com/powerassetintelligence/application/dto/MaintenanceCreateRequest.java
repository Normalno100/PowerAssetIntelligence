package com.powerassetintelligence.application.dto;

import com.powerassetintelligence.domain.model.MaintenanceType;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record MaintenanceCreateRequest(
        LocalDate repairDate,
        MaintenanceType maintenanceType,
        String description,
        BigDecimal repairCost,
        String failureCode,
        String performedBy,
        List<String> replacedComponents
) {
}
