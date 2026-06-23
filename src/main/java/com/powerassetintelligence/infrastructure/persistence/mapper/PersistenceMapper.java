package com.powerassetintelligence.infrastructure.persistence.mapper;

import com.powerassetintelligence.domain.model.Asset;
import com.powerassetintelligence.domain.model.MaintenanceRecord;
import com.powerassetintelligence.domain.model.RiskAssessment;
import com.powerassetintelligence.domain.model.TelemetryRecord;

public final class PersistenceMapper {
    private PersistenceMapper() {}

    public static Asset toDomain(com.powerassetintelligence.infrastructure.persistence.entity.Asset entity) {
        return new Asset(entity.getId(), entity.getType(), entity.getName(), entity.getInstallationDate(),
                entity.getStatus(), entity.getLocation(), entity.getManufacturer(), entity.getCriticality(),
                entity.getExpectedServiceLifeYears(), entity.getTechnicalParameters(), entity.getVersion(),
                entity.getCreatedAt(), entity.getUpdatedAt());
    }

    public static TelemetryRecord toDomain(com.powerassetintelligence.infrastructure.persistence.entity.TelemetryRecord entity) {
        return new TelemetryRecord(entity.getId(), entity.getAsset().getId(), entity.getTimestamp(),
                entity.getTemperatureCelsius(), entity.getLoadPercent(), entity.getVoltageKv(),
                entity.getCurrentAmpere(), entity.getVibrationMmSec(), entity.getOverheatingCount(),
                entity.getSourceSensorId(), entity.getExternalTelemetryId(), entity.getCreatedAt());
    }

    public static MaintenanceRecord toDomain(com.powerassetintelligence.infrastructure.persistence.entity.MaintenanceRecord entity) {
        return new MaintenanceRecord(entity.getId(), entity.getAsset().getId(), entity.getRepairDate(),
                entity.getMaintenanceType(), entity.getDescription(), entity.getRepairCost(), entity.getFailureCode(),
                entity.getPerformedBy(), entity.getReplacedComponents(), entity.getCreatedAt());
    }

    public static RiskAssessment toDomain(com.powerassetintelligence.infrastructure.persistence.entity.RiskAssessment entity) {
        return new RiskAssessment(entity.getId(), entity.getAsset().getId(), entity.getAssessedAt(),
                entity.getRiskScore(), entity.getRiskLevel(), entity.getRiskFactors(), entity.getRecommendations(),
                entity.getModelVersion(), entity.getExplanation(), entity.getCreatedAt());
    }
}
