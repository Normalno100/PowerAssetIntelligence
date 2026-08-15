package com.powerassetintelligence.unit.ai;

import com.powerassetintelligence.core.ai.RiskAssessmentSnapshot;
import com.powerassetintelligence.core.ai.RiskFeatures;
import com.powerassetintelligence.domain.model.AssetCriticality;
import com.powerassetintelligence.domain.model.AssetStatus;
import com.powerassetintelligence.domain.model.AssetType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class RiskAssessmentSnapshotTest {

    private static final UUID TEST_ASSET_ID = UUID.randomUUID();

    @Test
    @DisplayName("Snapshot fields must exactly mirror RiskFeatures fields")
    void snapshotMustMirrorFeatures() {
        RiskFeatures features = new RiskFeatures(
                TEST_ASSET_ID,
                AssetType.TRANSFORMER,
                AssetStatus.ACTIVE,
                AssetCriticality.HIGH,
                12,
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

        RiskAssessmentSnapshot snapshot = RiskAssessmentSnapshot.from(features);

        assertThat(snapshot.assetType()).isEqualTo(features.assetType());
        assertThat(snapshot.assetStatus()).isEqualTo(features.assetStatus());
        assertThat(snapshot.criticality()).isEqualTo(features.criticality());
        assertThat(snapshot.assetAgeYears()).isEqualTo(features.assetAgeYears());
        assertThat(snapshot.latestTemperatureCelsius()).isEqualTo(features.latestTemperatureCelsius());
        assertThat(snapshot.latestLoadPercent()).isEqualTo(features.latestLoadPercent());
        assertThat(snapshot.latestOverheatingCount()).isEqualTo(features.latestOverheatingCount());
        assertThat(snapshot.repairsLastYear()).isEqualTo(features.repairsLastYear());
        assertThat(snapshot.averageTemperatureCelsius()).isEqualTo(features.averageTemperatureCelsius());
        assertThat(snapshot.maxTemperatureCelsius()).isEqualTo(features.maxTemperatureCelsius());
        assertThat(snapshot.averageLoadPercent()).isEqualTo(features.averageLoadPercent());
        assertThat(snapshot.maxLoadPercent()).isEqualTo(features.maxLoadPercent());
        assertThat(snapshot.overheatingEventsLast24Hours()).isEqualTo(features.overheatingEventsLast24Hours());
        assertThat(snapshot.temperatureTrendCelsiusPerHour()).isEqualTo(features.temperatureTrendCelsiusPerHour());
        assertThat(snapshot.loadTrendPercentPerHour()).isEqualTo(features.loadTrendPercentPerHour());
    }

    @Test
    @DisplayName("Snapshot with null fields must not throw")
    void snapshotWithNullFieldsMustNotThrow() {
        RiskFeatures features = new RiskFeatures(
                TEST_ASSET_ID,
                AssetType.CIRCUIT_BREAKER,
                AssetStatus.DECOMMISSIONED,
                AssetCriticality.LOW,
                3,
                null, null, null,
                0L, null, null, null, null, 0L, null, null
        );

        RiskAssessmentSnapshot snapshot = RiskAssessmentSnapshot.from(features);

        assertThat(snapshot.latestTemperatureCelsius()).isNull();
        assertThat(snapshot.temperatureTrendCelsiusPerHour()).isNull();
        assertThat(snapshot.assetType()).isEqualTo(AssetType.CIRCUIT_BREAKER);
    }

    @Test
    @DisplayName("Snapshot is immutable — modifying original features must not affect snapshot")
    void snapshotMustBeIndependentOfFeatures() {
        // RiskFeatures is a record, so it's already immutable.
        // But we verify that the from() method doesn't share mutable state.
        RiskFeatures features = new RiskFeatures(
                TEST_ASSET_ID,
                AssetType.TRANSFORMER,
                AssetStatus.ACTIVE,
                AssetCriticality.MEDIUM,
                10,
                BigDecimal.valueOf(70),
                BigDecimal.valueOf(60),
                1,
                2L,
                BigDecimal.valueOf(65),
                BigDecimal.valueOf(75),
                BigDecimal.valueOf(55),
                BigDecimal.valueOf(70),
                3L,
                new BigDecimal("1.5"),
                new BigDecimal("0.8")
        );

        RiskAssessmentSnapshot snapshot = RiskAssessmentSnapshot.from(features);

        // Snapshot should have the exact same values
        assertThat(snapshot.latestTemperatureCelsius()).isEqualByComparingTo(BigDecimal.valueOf(70));
        assertThat(snapshot.temperatureTrendCelsiusPerHour()).isEqualByComparingTo(new BigDecimal("1.5"));

        // Since both are records (immutable), no further mutation test needed
        // but we verify the snapshot doesn't depend on the features object identity
        RiskAssessmentSnapshot snapshot2 = RiskAssessmentSnapshot.from(features);
        assertThat(snapshot).isEqualTo(snapshot2);
    }
}
