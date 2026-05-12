package com.powerassetintelligence.infrastructure.persistence.entity;

import com.powerassetintelligence.domain.model.AssetCriticality;
import com.powerassetintelligence.domain.model.AssetStatus;
import com.powerassetintelligence.domain.model.AssetType;
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
import jakarta.persistence.MapKeyColumn;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(
        name = "assets",
        indexes = {
                @Index(name = "idx_assets_type_status", columnList = "type,status"),
                @Index(name = "idx_assets_criticality", columnList = "criticality"),
                @Index(name = "idx_assets_location", columnList = "location")
        }
)
public class Asset {

    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 64)
    private AssetType type;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(nullable = false)
    private LocalDate installationDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 64)
    private AssetStatus status;

    @Column(nullable = false, length = 512)
    private String location;

    @Column(nullable = false, length = 255)
    private String manufacturer;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 64)
    private AssetCriticality criticality;

    @Column(nullable = false)
    private Integer expectedServiceLifeYears;

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "asset_technical_parameters", joinColumns = @JoinColumn(name = "asset_id"))
    @MapKeyColumn(name = "parameter_name", length = 128)
    @Column(name = "parameter_value", length = 512)
    private Map<String, String> technicalParameters = new HashMap<>();

    @Version
    @Column(nullable = false)
    private long version;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    protected Asset() {
    }

    public Asset(
            UUID id,
            AssetType type,
            String name,
            LocalDate installationDate,
            AssetStatus status,
            String location,
            String manufacturer,
            AssetCriticality criticality,
            Integer expectedServiceLifeYears,
            Map<String, String> technicalParameters
    ) {
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
    }

    @PrePersist
    void prePersist() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public AssetType getType() {
        return type;
    }

    public void setType(AssetType type) {
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public LocalDate getInstallationDate() {
        return installationDate;
    }

    public void setInstallationDate(LocalDate installationDate) {
        this.installationDate = installationDate;
    }

    public AssetStatus getStatus() {
        return status;
    }

    public void setStatus(AssetStatus status) {
        this.status = status;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getManufacturer() {
        return manufacturer;
    }

    public void setManufacturer(String manufacturer) {
        this.manufacturer = manufacturer;
    }

    public AssetCriticality getCriticality() {
        return criticality;
    }

    public void setCriticality(AssetCriticality criticality) {
        this.criticality = criticality;
    }

    public Integer getExpectedServiceLifeYears() {
        return expectedServiceLifeYears;
    }

    public void setExpectedServiceLifeYears(Integer expectedServiceLifeYears) {
        this.expectedServiceLifeYears = expectedServiceLifeYears;
    }

    public Map<String, String> getTechnicalParameters() {
        return technicalParameters;
    }

    public void setTechnicalParameters(Map<String, String> technicalParameters) {
        this.technicalParameters = new HashMap<>(technicalParameters == null ? Map.of() : technicalParameters);
    }

    public long getVersion() {
        return version;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
