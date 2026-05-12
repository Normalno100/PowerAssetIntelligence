package com.powerassetintelligence.infrastructure.persistence.entity;

import com.powerassetintelligence.domain.model.MaintenanceType;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(
        name = "maintenance_records",
        indexes = {
                @Index(name = "idx_maintenance_asset_repair_date", columnList = "asset_id,repair_date DESC"),
                @Index(name = "idx_maintenance_type", columnList = "maintenance_type")
        }
)
public class MaintenanceRecord {

    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "asset_id", nullable = false)
    private Asset asset;

    @Column(name = "repair_date", nullable = false)
    private LocalDate repairDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "maintenance_type", nullable = false, length = 64)
    private MaintenanceType maintenanceType;

    @Column(nullable = false, length = 2_000)
    private String description;

    @Column(name = "repair_cost", nullable = false, precision = 14, scale = 2)
    private BigDecimal repairCost;

    @Column(name = "failure_code", length = 64)
    private String failureCode;

    @Column(name = "performed_by", nullable = false, length = 255)
    private String performedBy;

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "maintenance_replaced_components", joinColumns = @JoinColumn(name = "maintenance_record_id"))
    @Column(name = "component", length = 255)
    private List<String> replacedComponents = new ArrayList<>();

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    protected MaintenanceRecord() {
    }

    public MaintenanceRecord(
            UUID id,
            Asset asset,
            LocalDate repairDate,
            MaintenanceType maintenanceType,
            String description,
            BigDecimal repairCost,
            String failureCode,
            String performedBy,
            List<String> replacedComponents
    ) {
        this.id = id;
        this.asset = asset;
        this.repairDate = repairDate;
        this.maintenanceType = maintenanceType;
        this.description = description;
        this.repairCost = repairCost;
        this.failureCode = failureCode;
        this.performedBy = performedBy;
        this.replacedComponents = new ArrayList<>(replacedComponents == null ? List.of() : replacedComponents);
    }

    @PrePersist
    void prePersist() {
        createdAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public Asset getAsset() {
        return asset;
    }

    public LocalDate getRepairDate() {
        return repairDate;
    }

    public MaintenanceType getMaintenanceType() {
        return maintenanceType;
    }

    public String getDescription() {
        return description;
    }

    public BigDecimal getRepairCost() {
        return repairCost;
    }

    public String getFailureCode() {
        return failureCode;
    }

    public String getPerformedBy() {
        return performedBy;
    }

    public List<String> getReplacedComponents() {
        return replacedComponents;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
