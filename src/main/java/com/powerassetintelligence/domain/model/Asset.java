package com.powerassetintelligence.domain.model;

import java.time.Instant;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
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
    public void setType(AssetType type) { this.type = type; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public LocalDate getInstallationDate() { return installationDate; }
    public void setInstallationDate(LocalDate installationDate) { this.installationDate = installationDate; }
    public AssetStatus getStatus() { return status; }
    public void setStatus(AssetStatus status) { this.status = status; }
    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }
    public String getManufacturer() { return manufacturer; }
    public void setManufacturer(String manufacturer) { this.manufacturer = manufacturer; }
    public AssetCriticality getCriticality() { return criticality; }
    public void setCriticality(AssetCriticality criticality) { this.criticality = criticality; }
    public Integer getExpectedServiceLifeYears() { return expectedServiceLifeYears; }
    public void setExpectedServiceLifeYears(Integer expectedServiceLifeYears) { this.expectedServiceLifeYears = expectedServiceLifeYears; }
    public Map<String, String> getTechnicalParameters() { return technicalParameters; }
    public void setTechnicalParameters(Map<String, String> technicalParameters) { this.technicalParameters = new HashMap<>(technicalParameters == null ? Map.of() : technicalParameters); }
    public long getVersion() { return version; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
