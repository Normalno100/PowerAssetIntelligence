package com.powerassetintelligence.application.service;

import com.powerassetintelligence.application.port.out.MaintenanceRepositoryPort;
import com.powerassetintelligence.application.port.out.TelemetryRepositoryPort;
import com.powerassetintelligence.core.ai.RiskFeatures;
import com.powerassetintelligence.domain.model.Asset;
import com.powerassetintelligence.domain.model.TelemetryRecord;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.Period;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Extracts risk features from an asset by combining latest telemetry,
 * 24-hour telemetry statistics, maintenance history, and asset metadata.
 */
@Service
@Transactional(readOnly = true)
public class RiskFeaturesExtractor {

    private static final int HOURS_WINDOW = 24;
    private static final int MINIMUM_DATA_POINTS_FOR_TREND = 2;

    private final TelemetryRepositoryPort telemetryRepository;
    private final MaintenanceRepositoryPort maintenanceRepository;
    private final Clock clock;

    public RiskFeaturesExtractor(
            TelemetryRepositoryPort telemetryRepository,
            MaintenanceRepositoryPort maintenanceRepository,
            Clock clock
    ) {
        this.telemetryRepository = telemetryRepository;
        this.maintenanceRepository = maintenanceRepository;
        this.clock = clock;
    }

    /**
     * Extracts all risk features for the given asset.
     */
    public RiskFeatures extract(Asset asset) {
        TelemetryRecord latestTelemetry = telemetryRepository
                .findFirstByAssetIdOrderByTimestampDesc(asset.getId())
                .orElse(null);

        Instant now = Instant.now(clock);
        Instant from = now.minus(HOURS_WINDOW, ChronoUnit.HOURS);
        List<TelemetryRecord> telemetry24h = telemetryRepository
                .findByAssetIdAndTimestampRange(asset.getId(), from, now);

        LocalDate today = LocalDate.now(clock);
        long repairsLastYear = maintenanceRepository
                .countByAssetIdAndRepairDateGreaterThanEqual(asset.getId(), today.minusYears(1));

        int ageYears = Math.max(0, Period.between(asset.getInstallationDate(), today).getYears());

        // 24-hour statistics
        BigDecimal averageTemp = averageNonNull(telemetry24h, TelemetryRecord::temperatureCelsius);
        BigDecimal maxTemp = maxNonNull(telemetry24h, TelemetryRecord::temperatureCelsius);
        BigDecimal averageLoad = averageNonNull(telemetry24h, TelemetryRecord::loadPercent);
        BigDecimal maxLoad = maxNonNull(telemetry24h, TelemetryRecord::loadPercent);
        long overheatingEvents = sumNonZeroOverheating(telemetry24h);

        // Temporal trends (computed independently)
        BigDecimal temperatureTrend = computeTrendPerHour(
                telemetry24h, TelemetryRecord::timestamp, TelemetryRecord::temperatureCelsius);
        BigDecimal loadTrend = computeTrendPerHour(
                telemetry24h, TelemetryRecord::timestamp, TelemetryRecord::loadPercent);

        return new RiskFeatures(
                asset.getId(),
                asset.getType(),
                asset.getStatus(),
                asset.getCriticality(),
                ageYears,
                latestTelemetry == null ? null : latestTelemetry.temperatureCelsius(),
                latestTelemetry == null ? null : latestTelemetry.loadPercent(),
                latestTelemetry == null ? null : latestTelemetry.overheatingCount(),
                repairsLastYear,
                averageTemp,
                maxTemp,
                averageLoad,
                maxLoad,
                overheatingEvents,
                temperatureTrend,
                loadTrend
        );
    }

    /**
     * Computes the slope (delta per hour) for a given numeric field over telemetry records.
     * Uses real timestamps to determine the time delta.
     *
     * <p>Null values are filtered out independently — a null in one field does not
     * affect the calculation for another field.
     *
     * @return slope in units per hour, or {@code null} if fewer than 2 valid data points
     */
    private BigDecimal computeTrendPerHour(
            List<TelemetryRecord> records,
            java.util.function.Function<TelemetryRecord, Instant> timestampExtractor,
            java.util.function.Function<TelemetryRecord, BigDecimal> valueExtractor
    ) {
        // Extract non-null (timestamp, value) pairs
        List<Measurement> measurements = new ArrayList<>();
        for (TelemetryRecord record : records) {
            Instant ts = timestampExtractor.apply(record);
            BigDecimal value = valueExtractor.apply(record);
            if (ts != null && value != null) {
                measurements.add(new Measurement(ts, value));
            }
        }

        // Need at least 2 data points to compute a trend
        if (measurements.size() < MINIMUM_DATA_POINTS_FOR_TREND) {
            return null;
        }

        // Sort by timestamp to ensure correct first/last
        measurements.sort(java.util.Comparator.comparing(Measurement::timestamp));

        BigDecimal valueDelta = measurements.get(measurements.size() - 1).value()
                .subtract(measurements.get(0).value());

        Duration duration = Duration.between(
                measurements.get(0).timestamp(),
                measurements.get(measurements.size() - 1).timestamp()
        );

        long durationHours = duration.toHours();
        long durationMinutes = duration.toMinutes();

        // Avoid division by zero — if all measurements have the same timestamp, return 0
        if (durationMinutes == 0) {
            return BigDecimal.ZERO;
        }

        // Convert duration to hours as a fraction for precision
        BigDecimal hours = BigDecimal.valueOf(durationMinutes).divide(
                BigDecimal.valueOf(60), 4, RoundingMode.HALF_UP);

        return valueDelta.divide(hours, 4, RoundingMode.HALF_UP);
    }

    private BigDecimal averageNonNull(
            List<TelemetryRecord> records,
            java.util.function.Function<TelemetryRecord, BigDecimal> extractor
    ) {
        Optional<BigDecimal> sum = records.stream()
                .map(extractor)
                .filter(Objects::nonNull)
                .filter(v -> v.compareTo(BigDecimal.ZERO) != 0)
                .reduce(BigDecimal::add);

        if (sum.isEmpty()) {
            return null;
        }

        // Count of non-null, non-zero values
        long count = records.stream()
                .map(extractor)
                .filter(Objects::nonNull)
                .filter(v -> v.compareTo(BigDecimal.ZERO) != 0)
                .count();

        return sum.get().divide(BigDecimal.valueOf(count), 2, RoundingMode.HALF_UP);
    }

    private BigDecimal maxNonNull(
            List<TelemetryRecord> records,
            java.util.function.Function<TelemetryRecord, BigDecimal> extractor
    ) {
        return records.stream()
                .map(extractor)
                .filter(Objects::nonNull)
                .filter(v -> v.compareTo(BigDecimal.ZERO) != 0)
                .max(BigDecimal::compareTo)
                .orElse(null);
    }

    private long sumNonZeroOverheating(List<TelemetryRecord> records) {
        return records.stream()
                .map(TelemetryRecord::overheatingCount)
                .filter(Objects::nonNull)
                .filter(v -> v > 0)
                .mapToLong(Integer::intValue)
                .sum();
    }

    /**
     * Internal helper to store a timestamp-value pair for trend calculation.
     */
    private record Measurement(Instant timestamp, BigDecimal value) {
    }
}
