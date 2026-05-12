package com.powerassetintelligence.application.dto;

import com.powerassetintelligence.domain.model.MaintenanceType;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record MaintenanceResponse(
        UUID id,
        UUID assetId,
        LocalDate repairDate,
        MaintenanceType maintenanceType,
        String description,
        BigDecimal repairCost,
        String failureCode,
        String performedBy,
        List<String> replacedComponents,
        Instant createdAt
) {
}
