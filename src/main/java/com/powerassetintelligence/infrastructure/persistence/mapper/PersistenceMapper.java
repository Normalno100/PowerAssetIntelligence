package com.powerassetintelligence.infrastructure.persistence.mapper;

import com.powerassetintelligence.application.port.out.PageResult;
import com.powerassetintelligence.domain.model.Asset;
import com.powerassetintelligence.domain.model.MaintenanceRecord;
import com.powerassetintelligence.domain.model.RiskAssessment;
import com.powerassetintelligence.domain.model.TelemetryRecord;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;

public final class PersistenceMapper {
    private PersistenceMapper() {}

    public static Asset toDomain(com.powerassetintelligence.infrastructure.persistence.entity.AssetEntity entity) {
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
        return new RiskAssessment(
                entity.getId(),
                entity.getAsset().getId(),
                entity.getAssessedAt(),
                entity.getRiskScore(),
                entity.getRiskLevel(),
                entity.getRiskFactors(),
                entity.getRecommendations(),
                entity.getModelVersion(),
                entity.getExplanation(),
                entity.getCreatedAt(),
                entity.getSnapshot()
        );
    }

    public static org.springframework.data.domain.Pageable toSpringPageable(com.powerassetintelligence.application.port.out.PageRequest pageRequest) {
        if (pageRequest == null || pageRequest.size() <= 0) {
            return org.springframework.data.domain.PageRequest.of(0, 20);
        }
        if (pageRequest.sort().isEmpty()) {
            return org.springframework.data.domain.PageRequest.of(pageRequest.page(), pageRequest.size());
        }
        var sortBuilder = org.springframework.data.domain.Sort.by(pageRequest.sort().stream()
                .map(sortOrder -> {
                    var direction = sortOrder.direction() == com.powerassetintelligence.application.port.out.PageRequest.SortOrder.Direction.ASC
                            ? Sort.Direction.ASC : Sort.Direction.DESC;
                    return new org.springframework.data.domain.Sort.Order(direction, sortOrder.field());
                }).toList());
        return org.springframework.data.domain.PageRequest.of(pageRequest.page(), pageRequest.size(), sortBuilder);
    }

    public static <S, T> PageResult<T> toPageResult(Page<S> page, java.util.function.Function<S, T> mapper) {
        var content = page.getContent().stream().map(mapper).toList();
        return new PageResult<>(content, page.getNumber(), page.getSize(), page.getTotalElements(), page.getTotalPages());
    }
}
