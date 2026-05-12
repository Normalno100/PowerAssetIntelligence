package com.powerassetintelligence.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "telemetry_records",
        indexes = {
                @Index(name = "idx_telemetry_asset_timestamp", columnList = "asset_id,recorded_at DESC"),
                @Index(name = "idx_telemetry_external_id", columnList = "external_telemetry_id", unique = true)
        }
)
public class TelemetryRecord {

    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "asset_id", nullable = false)
    private Asset asset;

    @Column(name = "recorded_at", nullable = false)
    private Instant timestamp;

    @Column(name = "temperature_celsius", precision = 8, scale = 2)
    private BigDecimal temperatureCelsius;

    @Column(name = "load_percent", precision = 8, scale = 2)
    private BigDecimal loadPercent;

    @Column(name = "voltage_kv", precision = 10, scale = 3)
    private BigDecimal voltageKv;

    @Column(name = "current_ampere", precision = 12, scale = 3)
    private BigDecimal currentAmpere;

    @Column(name = "vibration_mm_sec", precision = 8, scale = 3)
    private BigDecimal vibrationMmSec;

    @Column(name = "overheating_count")
    private Integer overheatingCount;

    @Column(name = "source_sensor_id", nullable = false, length = 128)
    private String sourceSensorId;

    @Column(name = "external_telemetry_id", length = 128, unique = true)
    private String externalTelemetryId;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    protected TelemetryRecord() {
    }

    public TelemetryRecord(
            UUID id,
            Asset asset,
            Instant timestamp,
            BigDecimal temperatureCelsius,
            BigDecimal loadPercent,
            BigDecimal voltageKv,
            BigDecimal currentAmpere,
            BigDecimal vibrationMmSec,
            Integer overheatingCount,
            String sourceSensorId,
            String externalTelemetryId
    ) {
        this.id = id;
        this.asset = asset;
        this.timestamp = timestamp;
        this.temperatureCelsius = temperatureCelsius;
        this.loadPercent = loadPercent;
        this.voltageKv = voltageKv;
        this.currentAmpere = currentAmpere;
        this.vibrationMmSec = vibrationMmSec;
        this.overheatingCount = overheatingCount;
        this.sourceSensorId = sourceSensorId;
        this.externalTelemetryId = externalTelemetryId;
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

    public Instant getTimestamp() {
        return timestamp;
    }

    public BigDecimal getTemperatureCelsius() {
        return temperatureCelsius;
    }

    public BigDecimal getLoadPercent() {
        return loadPercent;
    }

    public BigDecimal getVoltageKv() {
        return voltageKv;
    }

    public BigDecimal getCurrentAmpere() {
        return currentAmpere;
    }

    public BigDecimal getVibrationMmSec() {
        return vibrationMmSec;
    }

    public Integer getOverheatingCount() {
        return overheatingCount;
    }

    public String getSourceSensorId() {
        return sourceSensorId;
    }

    public String getExternalTelemetryId() {
        return externalTelemetryId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
