package com.powerassetintelligence.domain.model;

import java.time.Instant;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class Asset {
    private final UUID id;
    private AssetType type;
    private String name;
    private LocalDate installationDate;
    private AssetStatus status;
    private String location;
    private String manufacturer;
    private AssetCriticality criticality;
    private Integer expectedServiceLifeYears;
    private Map<String, String> technicalParameters;
    private final long version;
    private final Instant createdAt;
    private final Instant updatedAt;

    public Asset(UUID id, AssetType type, String name, LocalDate installationDate, AssetStatus status, String location,
            String manufacturer, AssetCriticality criticality, Integer expectedServiceLifeYears,
            Map<String, String> technicalParameters) {
        this(id, type, name, installationDate, status, location, manufacturer, criticality, expectedServiceLifeYears,
                technicalParameters, 0, null, null);
    }

    public Asset(UUID id, AssetType type, String name, LocalDate installationDate, AssetStatus status, String location,
            String manufacturer, AssetCriticality criticality, Integer expectedServiceLifeYears,
            Map<String, String> technicalParameters, long version, Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.type = type;
        this.name = name;
        this.installationDate = installationDate;
        this.status = status;
        this.location = location;
        this.manufacturer = manufacturer;
        this.criticality = criticality;
        this.expectedServiceLifeYears = expectedServiceLifeYears;
        this.technicalParameters = new HashMap<>(technicalParameters == null ? Map.of() : technicalParameters);
        this.version = version;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public UUID getId() { return id; }
    public AssetType getType() { return type; }
    public String getName() { return name; }
    public LocalDate getInstallationDate() { return installationDate; }
    public AssetStatus getStatus() { return status; }

    /**
     * Transitions the asset to UNDER_MAINTENANCE status.
     * No-op if the asset is already DECOMMISSIONED.
     */
    public void startMaintenance() {
        if (this.status != AssetStatus.DECOMMISSIONED) {
            this.status = AssetStatus.UNDER_MAINTENANCE;
        }
    }

    /**
     * Transitions the asset to DECOMMISSIONED status.
     * No-op if the asset is already DECOMMISSIONED.
     */
    public void decommission() {
        if (this.status != AssetStatus.DECOMMISSIONED) {
            this.status = AssetStatus.DECOMMISSIONED;
        }
    }

    /**
     * Partial update of mutable fields. Type is immutable after creation.
     * Null values are skipped (partial update). Validates domain invariants:
     * name not blank, serviceLife > 0, and valid status transitions.
     */
    public void updateFields(String name,
                             LocalDate installationDate,
                             AssetStatus status,
                             String location,
                             String manufacturer,
                             AssetCriticality criticality,
                             Integer expectedServiceLifeYears,
                             Map<String, String> technicalParameters) {
        if (name != null && name.isBlank()) {
            throw new IllegalArgumentException("Asset name must not be blank");
        }
        if (name != null) {
            this.name = name.trim();
        }
        if (installationDate != null) {
            this.installationDate = installationDate;
        }
        if (status != null) {
            updateStatus(status);
        }
        if (location != null) {
            this.location = location.trim();
        }
        if (manufacturer != null) {
            this.manufacturer = manufacturer.trim();
        }
        if (criticality != null) {
            this.criticality = criticality;
        }
        if (expectedServiceLifeYears != null) {
            if (expectedServiceLifeYears <= 0) {
                throw new IllegalArgumentException("expectedServiceLifeYears must be greater than 0");
            }
            this.expectedServiceLifeYears = expectedServiceLifeYears;
        }
        if (technicalParameters != null) {
            this.technicalParameters = new HashMap<>(technicalParameters);
        }
    }

    public String getLocation() { return location; }
    public String getManufacturer() { return manufacturer; }
    public AssetCriticality getCriticality() { return criticality; }
    public Integer getExpectedServiceLifeYears() { return expectedServiceLifeYears; }
    public Map<String, String> getTechnicalParameters() { return technicalParameters; }
    public long getVersion() { return version; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }

    // ---- Private helpers for domain invariants ----

    private void updateName(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Asset name must not be blank");
        }
        this.name = name;
    }

    private void updateStatus(AssetStatus newStatus) {
        if (newStatus == null) {
            throw new IllegalArgumentException("Status must not be null");
        }
        validateStatusTransition(this.status, newStatus);
        this.status = newStatus;
    }

    private void validateStatusTransition(AssetStatus from, AssetStatus to) {
        Set<AssetStatus> allowed = switch (from) {
            case ACTIVE -> Set.of(
                    AssetStatus.WARNING,
                    AssetStatus.CRITICAL,
                    AssetStatus.UNDER_MAINTENANCE,
                    AssetStatus.DECOMMISSIONED
            );
            case WARNING -> Set.of(
                    AssetStatus.CRITICAL,
                    AssetStatus.UNDER_MAINTENANCE,
                    AssetStatus.ACTIVE
            );
            case CRITICAL -> Set.of(
                    AssetStatus.UNDER_MAINTENANCE,
                    AssetStatus.ACTIVE
            );
            case UNDER_MAINTENANCE -> Set.of(
                    AssetStatus.ACTIVE,
                    AssetStatus.DECOMMISSIONED
            );
            case DECOMMISSIONED -> Set.of(); // terminal state
        };
        if (!allowed.contains(to)) {
            throw new IllegalStateException(
                    "Invalid status transition: " + from + " -> " + to);
        }
    }

    // ---- Private setters (for potential reflection-based hydration) ----

    private void setType(AssetType type) { this.type = type; }
    private void setName(String name) { this.name = name; }
    private void setInstallationDate(LocalDate installationDate) { this.installationDate = installationDate; }
    private void setStatus(AssetStatus status) { this.status = status; }
    private void setLocation(String location) { this.location = location; }
    private void setManufacturer(String manufacturer) { this.manufacturer = manufacturer; }
    private void setCriticality(AssetCriticality criticality) { this.criticality = criticality; }
    private void setExpectedServiceLifeYears(Integer expectedServiceLifeYears) {
        this.expectedServiceLifeYears = expectedServiceLifeYears;
    }
    private void setTechnicalParameters(Map<String, String> technicalParameters) {
        this.technicalParameters = new HashMap<>(technicalParameters == null ? Map.of() : technicalParameters);
    }
}
