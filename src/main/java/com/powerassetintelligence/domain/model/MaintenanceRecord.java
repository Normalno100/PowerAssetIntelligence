package com.powerassetintelligence.domain.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record MaintenanceRecord(UUID id, UUID assetId, LocalDate repairDate, MaintenanceType maintenanceType,
        String description, BigDecimal repairCost, String failureCode, String performedBy,
        List<String> replacedComponents, Instant createdAt) {
    public MaintenanceRecord {
        replacedComponents = List.copyOf(replacedComponents == null ? List.of() : replacedComponents);
    }

    public MaintenanceRecord(UUID id, UUID assetId, LocalDate repairDate, MaintenanceType maintenanceType,
            String description, BigDecimal repairCost, String failureCode, String performedBy,
            List<String> replacedComponents) {
        this(id, assetId, repairDate, maintenanceType, description, repairCost, failureCode, performedBy,
                replacedComponents, null);
    }
}
