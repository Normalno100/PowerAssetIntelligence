package com.powerassetintelligence.unit;

import com.powerassetintelligence.core.ai.RiskAssessmentSnapshot;
import com.powerassetintelligence.core.ai.RiskFeatures;
import com.powerassetintelligence.domain.model.AssetStatus;
import com.powerassetintelligence.domain.model.AssetType;
import com.powerassetintelligence.domain.model.AssetCriticality;
import com.powerassetintelligence.infrastructure.persistence.entity.RiskAssessment;
import com.powerassetintelligence.infrastructure.persistence.converter.RiskAssessmentSnapshotConverter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class RiskAssessmentSnapshotPersistenceTest {

    private RiskAssessmentSnapshotConverter converter;

    @BeforeEach
    void setUp() {
        converter = new RiskAssessmentSnapshotConverter();
    }

    private RiskAssessmentSnapshot createTestSnapshot() {
        RiskFeatures features = new RiskFeatures(
                UUID.randomUUID(),
                AssetType.TRANSFORMER,
                AssetStatus.ACTIVE,
                AssetCriticality.HIGH,
                15,
                BigDecimal.valueOf(85.5),
                BigDecimal.valueOf(72.3),
                3,
                4L,
                BigDecimal.valueOf(80.0),
                BigDecimal.valueOf(92.1),
                BigDecimal.valueOf(68.5),
                BigDecimal.valueOf(88.0),
                7L,
                new BigDecimal("5.2500"),
                new BigDecimal("2.1000")
        );
        return RiskAssessmentSnapshot.from(features);
    }

    @Test
    @DisplayName("Converter must round-trip snapshot through JSON")
    void converterMustRoundTripSnapshot() {
        RiskAssessmentSnapshot original = createTestSnapshot();

        String json = converter.convertToDatabaseColumn(original);
        RiskAssessmentSnapshot restored = converter.convertToEntityAttribute(json);

        assertThat(restored).isNotNull();
        assertThat(restored.assetType()).isEqualTo(original.assetType());
        assertThat(restored.assetStatus()).isEqualTo(original.assetStatus());
        assertThat(restored.criticality()).isEqualTo(original.criticality());
        assertThat(restored.assetAgeYears()).isEqualTo(original.assetAgeYears());
        assertThat(restored.latestTemperatureCelsius()).isEqualTo(original.latestTemperatureCelsius());
        assertThat(restored.latestLoadPercent()).isEqualTo(original.latestLoadPercent());
        assertThat(restored.latestOverheatingCount()).isEqualTo(original.latestOverheatingCount());
        assertThat(restored.repairsLastYear()).isEqualTo(original.repairsLastYear());
        assertThat(restored.averageTemperatureCelsius()).isEqualTo(original.averageTemperatureCelsius());
        assertThat(restored.maxTemperatureCelsius()).isEqualTo(original.maxTemperatureCelsius());
        assertThat(restored.averageLoadPercent()).isEqualTo(original.averageLoadPercent());
        assertThat(restored.maxLoadPercent()).isEqualTo(original.maxLoadPercent());
        assertThat(restored.overheatingEventsLast24Hours()).isEqualTo(original.overheatingEventsLast24Hours());
        assertThat(restored.temperatureTrendCelsiusPerHour()).isEqualTo(original.temperatureTrendCelsiusPerHour());
        assertThat(restored.loadTrendPercentPerHour()).isEqualTo(original.loadTrendPercentPerHour());
    }

    @Test
    @DisplayName("Converter must handle null snapshot")
    void converterMustHandleNull() {
        assertThat(converter.convertToDatabaseColumn(null)).isNull();
        assertThat(converter.convertToEntityAttribute(null)).isNull();
        assertThat(converter.convertToEntityAttribute("   ")).isNull();
    }

    @Test
    @DisplayName("Converter must handle null values inside snapshot (no telemetry)")
    void converterMustHandleNullValuesInsideSnapshot() {
        RiskFeatures features = new RiskFeatures(
                UUID.randomUUID(),
                AssetType.CIRCUIT_BREAKER,
                AssetStatus.DECOMMISSIONED,
                AssetCriticality.LOW,
                3,
                null, null, null,
                0L, null, null, null, null, 0L, null, null
        );
        RiskAssessmentSnapshot snapshot = RiskAssessmentSnapshot.from(features);

        String json = converter.convertToDatabaseColumn(snapshot);
        RiskAssessmentSnapshot restored = converter.convertToEntityAttribute(json);

        assertThat(restored).isNotNull();
        assertThat(restored.latestTemperatureCelsius()).isNull();
        assertThat(restored.temperatureTrendCelsiusPerHour()).isNull();
        assertThat(restored.assetType()).isEqualTo(AssetType.CIRCUIT_BREAKER);
        assertThat(restored.assetAgeYears()).isEqualTo(3);
    }

    @Test
    @DisplayName("JPA entity must persist and restore snapshot")
    void entityMustPersistAndRestoreSnapshot() {
        RiskAssessmentSnapshot snapshot = createTestSnapshot();

        RiskAssessment entity = new RiskAssessment(
                UUID.randomUUID(),
                null, // asset (not needed for this test)
                Instant.now(),
                BigDecimal.valueOf(82.5),
                com.powerassetintelligence.domain.model.RiskLevel.HIGH,
                List.of(),
                List.of("Inspect thermal condition"),
                "rules-2026.05",
                "Assessment explanation",
                Instant.now(),
                snapshot
        );

        // Simulate JPA conversion
        String json = converter.convertToDatabaseColumn(entity.getSnapshot());
        RiskAssessmentSnapshot restored = converter.convertToEntityAttribute(json);

        assertThat(restored).isNotNull();
        assertThat(restored.latestTemperatureCelsius())
                .as("snapshot must preserve temperature")
                .isEqualTo(snapshot.latestTemperatureCelsius());
    }
}
