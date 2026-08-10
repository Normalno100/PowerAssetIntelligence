package com.powerassetintelligence.unit;

import com.powerassetintelligence.application.port.out.MaintenanceRepositoryPort;
import com.powerassetintelligence.application.port.out.TelemetryRepositoryPort;
import com.powerassetintelligence.application.service.RiskFeaturesExtractor;
import com.powerassetintelligence.core.ai.RiskFeatures;
import com.powerassetintelligence.domain.model.Asset;
import com.powerassetintelligence.domain.model.AssetCriticality;
import com.powerassetintelligence.domain.model.AssetStatus;
import com.powerassetintelligence.domain.model.AssetType;
import com.powerassetintelligence.domain.model.MaintenanceRecord;
import com.powerassetintelligence.domain.model.TelemetryRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RiskFeaturesExtractorTest {

    private static final UUID TEST_ASSET_ID = UUID.randomUUID();
    private static final LocalDate INSTALLATION_DATE = LocalDate.of(2010, 1, 1);
    private static final Instant FIXED_NOW = Instant.parse("2025-06-15T12:00:00Z");
    private static final Instant FROM_24H_AGO = Instant.parse("2025-06-14T12:00:00Z");

    private TelemetryRepositoryPort telemetryRepository;
    private MaintenanceRepositoryPort maintenanceRepository;
    private RiskFeaturesExtractor extractor;
    private Clock fixedClock;

    @BeforeEach
    void setUp() {
        telemetryRepository = mock(TelemetryRepositoryPort.class);
        maintenanceRepository = mock(MaintenanceRepositoryPort.class);
        fixedClock = Clock.fixed(FIXED_NOW, ZoneId.of("UTC"));
        extractor = new RiskFeaturesExtractor(telemetryRepository, maintenanceRepository, fixedClock);
    }

    private Asset createAsset() {
        return new Asset(
                TEST_ASSET_ID,
                AssetType.TRANSFORMER,
                "Test Transformer",
                INSTALLATION_DATE,
                AssetStatus.ACTIVE,
                "Location A",
                "Manufacturer X",
                AssetCriticality.HIGH,
                30,
                null
        );
    }

    private TelemetryRecord telemetry(BigDecimal temp, BigDecimal load, Integer overheating, Instant timestamp) {
        return new TelemetryRecord(
                UUID.randomUUID(),
                TEST_ASSET_ID,
                timestamp,
                temp,
                load,
                null, null, null,
                overheating,
                "sensor-1",
                "ext-1"
        );
    }

    // ===== Test 1: Telemetry отсутствует =====
    @Test
    @DisplayName("Test 1 - telemetry отсутствует: все статистические значения = null")
    void shouldReturnNullStatsWhenNoTelemetry() {
        Asset asset = createAsset();

        when(telemetryRepository.findFirstByAssetIdOrderByTimestampDesc(TEST_ASSET_ID))
                .thenReturn(Optional.empty());
        when(telemetryRepository.findByAssetIdAndTimestampRange(TEST_ASSET_ID, FROM_24H_AGO, FIXED_NOW))
                .thenReturn(List.of());
        when(maintenanceRepository.countByAssetIdAndRepairDateGreaterThanEqual(TEST_ASSET_ID, LocalDate.of(2024, 6, 15)))
                .thenReturn(3L);

        RiskFeatures features = extractor.extract(asset);

        assertThat(features.assetId()).isEqualTo(TEST_ASSET_ID);
        assertThat(features.assetType()).isEqualTo(AssetType.TRANSFORMER);
        assertThat(features.assetAgeYears()).isEqualTo(15); // 2025 - 2010
        assertThat(features.repairsLastYear()).isEqualTo(3L);

        // Latest telemetry fields
        assertThat(features.latestTemperatureCelsius()).isNull();
        assertThat(features.latestLoadPercent()).isNull();
        assertThat(features.latestOverheatingCount()).isNull();

        // 24h statistics should all be null
        assertThat(features.averageTemperatureCelsius()).isNull();
        assertThat(features.maxTemperatureCelsius()).isNull();
        assertThat(features.averageLoadPercent()).isNull();
        assertThat(features.maxLoadPercent()).isNull();
        assertThat(features.overheatingEventsLast24Hours()).isZero();
    }

    // ===== Test 2: Одна telemetry запись =====
    @Test
    @DisplayName("Test 2 - одна telemetry запись: avg/max совпадают с единственным значением")
    void shouldReturnSameValuesForSingleTelemetry() {
        Asset asset = createAsset();
        Instant ts = FIXED_NOW.minus(6, java.time.temporal.ChronoUnit.HOURS);

        TelemetryRecord record = telemetry(
                new BigDecimal("80"),
                new BigDecimal("75"),
                2,
                ts
        );

        when(telemetryRepository.findFirstByAssetIdOrderByTimestampDesc(TEST_ASSET_ID))
                .thenReturn(Optional.of(record));
        when(telemetryRepository.findByAssetIdAndTimestampRange(TEST_ASSET_ID, FROM_24H_AGO, FIXED_NOW))
                .thenReturn(List.of(record));
        when(maintenanceRepository.countByAssetIdAndRepairDateGreaterThanEqual(TEST_ASSET_ID, LocalDate.of(2024, 6, 15)))
                .thenReturn(1L);

        RiskFeatures features = extractor.extract(asset);

        // Latest
        assertThat(features.latestTemperatureCelsius()).isEqualByComparingTo(new BigDecimal("80"));
        assertThat(features.latestLoadPercent()).isEqualByComparingTo(new BigDecimal("75"));

        // Stats should match single value
        assertThat(features.averageTemperatureCelsius()).isEqualByComparingTo(new BigDecimal("80"));
        assertThat(features.maxTemperatureCelsius()).isEqualByComparingTo(new BigDecimal("80"));
        assertThat(features.averageLoadPercent()).isEqualByComparingTo(new BigDecimal("75"));
        assertThat(features.maxLoadPercent()).isEqualByComparingTo(new BigDecimal("75"));

        // Overheating events: one record with overheatingCount=2
        assertThat(features.overheatingEventsLast24Hours()).isEqualTo(2L);
    }

    // ===== Test 3: Несколько telemetry записей =====
    @Test
    @DisplayName("Test 3 - несколько записей: средняя и максимальная вычисляются корректно")
    void shouldComputeStatsForMultipleTelemetry() {
        Asset asset = createAsset();

        TelemetryRecord t1 = telemetry(
                new BigDecimal("70"), new BigDecimal("50"), 1,
                FIXED_NOW.minus(20, java.time.temporal.ChronoUnit.HOURS)
        );
        TelemetryRecord t2 = telemetry(
                new BigDecimal("80"), new BigDecimal("70"), 0,
                FIXED_NOW.minus(12, java.time.temporal.ChronoUnit.HOURS)
        );
        TelemetryRecord t3 = telemetry(
                new BigDecimal("90"), new BigDecimal("90"), 3,
                FIXED_NOW.minus(3, java.time.temporal.ChronoUnit.HOURS)
        );

        List<TelemetryRecord> records = List.of(t1, t2, t3);

        when(telemetryRepository.findFirstByAssetIdOrderByTimestampDesc(TEST_ASSET_ID))
                .thenReturn(Optional.of(t3));
        when(telemetryRepository.findByAssetIdAndTimestampRange(TEST_ASSET_ID, FROM_24H_AGO, FIXED_NOW))
                .thenReturn(records);
        when(maintenanceRepository.countByAssetIdAndRepairDateGreaterThanEqual(TEST_ASSET_ID, LocalDate.of(2024, 6, 15)))
                .thenReturn(0L);

        RiskFeatures features = extractor.extract(asset);

        // Latest = last (t3)
        assertThat(features.latestTemperatureCelsius()).isEqualByComparingTo(new BigDecimal("90"));
        assertThat(features.latestLoadPercent()).isEqualByComparingTo(new BigDecimal("90"));

        // Stats: (70+80+90)/3 = 80, max = 90
        assertThat(features.averageTemperatureCelsius()).isEqualByComparingTo(new BigDecimal("80"));
        assertThat(features.maxTemperatureCelsius()).isEqualByComparingTo(new BigDecimal("90"));
        assertThat(features.averageLoadPercent()).isEqualByComparingTo(new BigDecimal("70"));
        assertThat(features.maxLoadPercent()).isEqualByComparingTo(new BigDecimal("90"));

        // Overheating: 1 + 0 + 3 = 4 (but we sum only non-zero: 1 + 3 = 4)
        assertThat(features.overheatingEventsLast24Hours()).isEqualTo(4L);
    }

    // ===== Test 4: Telemetry старше 24 часов =====
    @Test
    @DisplayName("Test 4 - telemetry старше 24 часов: записи игнорируются в статистике")
    void shouldIgnoreTelemetryOlderThan24Hours() {
        Asset asset = createAsset();

        // Older than 24h — should NOT be in 24h window
        TelemetryRecord oldRecord = telemetry(
                new BigDecimal("95"), new BigDecimal("99"), 10,
                FIXED_NOW.minus(48, java.time.temporal.ChronoUnit.HOURS)
        );

        // No latest telemetry within window (but old one exists globally)
        when(telemetryRepository.findFirstByAssetIdOrderByTimestampDesc(TEST_ASSET_ID))
                .thenReturn(Optional.of(oldRecord));
        when(telemetryRepository.findByAssetIdAndTimestampRange(TEST_ASSET_ID, FROM_24H_AGO, FIXED_NOW))
                .thenReturn(List.of()); // empty within 24h window
        when(maintenanceRepository.countByAssetIdAndRepairDateGreaterThanEqual(TEST_ASSET_ID, LocalDate.of(2024, 6, 15)))
                .thenReturn(2L);

        RiskFeatures features = extractor.extract(asset);

        // Latest will be the old record (findFirst returns oldest by timestamp desc)
        assertThat(features.latestTemperatureCelsius()).isEqualByComparingTo(new BigDecimal("95"));

        // But stats should be null (no telemetry in 24h window)
        assertThat(features.averageTemperatureCelsius()).isNull();
        assertThat(features.maxTemperatureCelsius()).isNull();
        assertThat(features.averageLoadPercent()).isNull();
        assertThat(features.maxLoadPercent()).isNull();
        assertThat(features.overheatingEventsLast24Hours()).isZero();
    }

    // ===== Test 5: Mixed null values =====
    @Test
    @DisplayName("Test 5 - mixed null значения: статистика только по существующим ненулевым")
    void shouldComputeStatsOnlyOverNonNullNonZeroValues() {
        Asset asset = createAsset();

        // temperature: 70, null, 90 → average over non-null non-zero = (70+90)/2 = 80
        // load: null, 50, 80 → average = (50+80)/2 = 65
        TelemetryRecord t1 = telemetry(
                new BigDecimal("70"), null, 0,
                FIXED_NOW.minus(20, java.time.temporal.ChronoUnit.HOURS)
        );
        TelemetryRecord t2 = telemetry(null, new BigDecimal("50"), 0,
                FIXED_NOW.minus(12, java.time.temporal.ChronoUnit.HOURS)
        );
        TelemetryRecord t3 = telemetry(
                new BigDecimal("90"), new BigDecimal("80"), 1,
                FIXED_NOW.minus(3, java.time.temporal.ChronoUnit.HOURS)
        );

        List<TelemetryRecord> records = List.of(t1, t2, t3);

        when(telemetryRepository.findFirstByAssetIdOrderByTimestampDesc(TEST_ASSET_ID))
                .thenReturn(Optional.of(t3));
        when(telemetryRepository.findByAssetIdAndTimestampRange(TEST_ASSET_ID, FROM_24H_AGO, FIXED_NOW))
                .thenReturn(records);
        when(maintenanceRepository.countByAssetIdAndRepairDateGreaterThanEqual(TEST_ASSET_ID, LocalDate.of(2024, 6, 15)))
                .thenReturn(0L);

        RiskFeatures features = extractor.extract(asset);

        // Temperature: non-null non-zero values = [70, 90], avg = 80, max = 90
        assertThat(features.averageTemperatureCelsius()).isEqualByComparingTo(new BigDecimal("80"));
        assertThat(features.maxTemperatureCelsius()).isEqualByComparingTo(new BigDecimal("90"));

        // Load: non-null non-zero values = [50, 80], avg = 65, max = 80
        assertThat(features.averageLoadPercent()).isEqualByComparingTo(new BigDecimal("65"));
        assertThat(features.maxLoadPercent()).isEqualByComparingTo(new BigDecimal("80"));
    }

    // ===== Test 6: Deterministic clock =====
    @Test
    @DisplayName("Test 6 - deterministic clock: два вызова с одинаковым clock дают одинаковые результаты")
    void shouldProduceDeterministicResultsWithFixedClock() {
        Asset asset = createAsset();
        Instant ts = FIXED_NOW.minus(1, java.time.temporal.ChronoUnit.HOURS);

        TelemetryRecord record = telemetry(
                new BigDecimal("75"), new BigDecimal("60"), 1, ts
        );

        when(telemetryRepository.findFirstByAssetIdOrderByTimestampDesc(TEST_ASSET_ID))
                .thenReturn(Optional.of(record));
        when(telemetryRepository.findByAssetIdAndTimestampRange(TEST_ASSET_ID, FROM_24H_AGO, FIXED_NOW))
                .thenReturn(List.of(record));
        when(maintenanceRepository.countByAssetIdAndRepairDateGreaterThanEqual(TEST_ASSET_ID, LocalDate.of(2024, 6, 15)))
                .thenReturn(5L);

        RiskFeatures result1 = extractor.extract(asset);
        RiskFeatures result2 = extractor.extract(asset);

        assertThat(result1).isEqualTo(result2);
        assertThat(result1.averageTemperatureCelsius()).isEqualByComparingTo(new BigDecimal("75"));
        assertThat(result1.maxTemperatureCelsius()).isEqualByComparingTo(new BigDecimal("75"));
        assertThat(result1.averageLoadPercent()).isEqualByComparingTo(new BigDecimal("60"));
        assertThat(result1.maxLoadPercent()).isEqualByComparingTo(new BigDecimal("60"));
        assertThat(result1.overheatingEventsLast24Hours()).isEqualTo(1L);
        assertThat(result1.repairsLastYear()).isEqualTo(5L);
        assertThat(result1.assetAgeYears()).isEqualTo(15);
    }
}
