package com.powerassetintelligence.application.service;

import com.powerassetintelligence.application.dto.RiskAssessmentDetailsResponse;
import com.powerassetintelligence.application.dto.RiskAssessmentResponse;
import com.powerassetintelligence.application.dto.RiskFeatures;
import com.powerassetintelligence.application.dto.RiskFeaturesResponse;
import com.powerassetintelligence.application.dto.RiskScoringResult;
import com.powerassetintelligence.application.port.out.RiskScoringPort;
import com.powerassetintelligence.domain.model.Asset;
import com.powerassetintelligence.domain.model.RiskAssessment;
import com.powerassetintelligence.domain.model.TelemetryRecord;
import com.powerassetintelligence.application.port.out.MaintenanceRepositoryPort;
import com.powerassetintelligence.application.port.out.RiskAssessmentRepositoryPort;
import com.powerassetintelligence.application.port.out.TelemetryRepositoryPort;
import com.powerassetintelligence.application.port.out.PageRequest;
import com.powerassetintelligence.application.port.out.PageResult;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.Period;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class RiskAnalysisService {

    private final AssetService assetService;
    private final TelemetryRepositoryPort telemetryRecordRepository;
    private final MaintenanceRepositoryPort maintenanceRecordRepository;
    private final RiskAssessmentRepositoryPort riskAssessmentRepository;
    private final RiskScoringPort riskEngine;
    private final Clock clock;

    public RiskAnalysisService(
            AssetService assetService,
            TelemetryRepositoryPort telemetryRecordRepository,
            MaintenanceRepositoryPort maintenanceRecordRepository,
            RiskAssessmentRepositoryPort riskAssessmentRepository,
            RiskScoringPort riskEngine
    ) {
        this.assetService = assetService;
        this.telemetryRecordRepository = telemetryRecordRepository;
        this.maintenanceRecordRepository = maintenanceRecordRepository;
        this.riskAssessmentRepository = riskAssessmentRepository;
        this.riskEngine = riskEngine;
        this.clock = Clock.systemUTC();
    }

    @Transactional
    public RiskAssessmentDetailsResponse assess(UUID assetId) {
        Asset asset = assetService.getAsset(assetId);
        var dtoFeatures = extractFeatures(asset);
        var scoringResult = riskEngine.score(dtoFeatures);
        RiskAssessment assessment = new RiskAssessment(
                UUID.randomUUID(),
                asset.getId(),
                Instant.now(clock),
                scoringResult.riskScore(),
                scoringResult.riskLevel(),
                scoringResult.riskFactors(),
                scoringResult.recommendations(),
                scoringResult.modelVersion(),
                scoringResult.explanation()
        );
        return new RiskAssessmentDetailsResponse(
                toResponse(riskAssessmentRepository.save(assessment)),
                toFeaturesResponse(dtoFeatures)
        );
    }

    public RiskAssessmentResponse getLatest(UUID assetId) {
        assetService.getAsset(assetId);
        return riskAssessmentRepository.findFirstByAssetIdOrderByAssessedAtDesc(assetId)
                .map(this::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Risk assessment not found for asset: " + assetId));
    }

    public PageResult<RiskAssessmentResponse> findByAsset(UUID assetId, PageRequest pageRequest) {
        assetService.getAsset(assetId);
        var result = riskAssessmentRepository.findByAssetId(assetId, pageRequest);
        var content = result.content().stream().map(this::toResponse).toList();
        return new PageResult<>(content, result.page(), result.size(), result.totalElements(), result.totalPages());
    }

    public PageResult<RiskAssessmentResponse> findTopRisky(PageRequest pageRequest) {
        var result = riskAssessmentRepository.findAll(pageRequest);
        var content = result.content().stream().map(this::toResponse).toList();
        return new PageResult<>(content, result.page(), result.size(), result.totalElements(), result.totalPages());
    }

    private RiskFeatures extractFeatures(Asset asset) {
        TelemetryRecord latestTelemetry = telemetryRecordRepository.findFirstByAssetIdOrderByTimestampDesc(asset.getId())
                .orElse(null);
        LocalDate today = LocalDate.now(clock);
        long repairsLastYear = maintenanceRecordRepository.countByAssetIdAndRepairDateGreaterThanEqual(
                asset.getId(),
                today.minusYears(1)
        );
        int ageYears = Math.max(0, Period.between(asset.getInstallationDate(), today).getYears());

        return new RiskFeatures(
                asset.getId(),
                asset.getType(),
                asset.getStatus(),
                asset.getCriticality(),
                ageYears,
                latestTelemetry == null ? null : latestTelemetry.temperatureCelsius(),
                latestTelemetry == null ? null : latestTelemetry.loadPercent(),
                latestTelemetry == null ? null : latestTelemetry.overheatingCount(),
                repairsLastYear
        );
    }

    private RiskAssessmentResponse toResponse(RiskAssessment assessment) {
        return new RiskAssessmentResponse(
                assessment.id(),
                assessment.assetId(),
                assessment.assessedAt(),
                assessment.riskScore(),
                assessment.riskLevel(),
                List.copyOf(assessment.riskFactors()),
                List.copyOf(assessment.recommendations()),
                assessment.modelVersion(),
                assessment.explanation(),
                assessment.createdAt()
        );
    }

    private RiskFeaturesResponse toFeaturesResponse(RiskFeatures features) {
        return new RiskFeaturesResponse(
                features.assetId(),
                features.assetType(),
                features.assetStatus(),
                features.criticality(),
                features.assetAgeYears(),
                features.latestTemperatureCelsius(),
                features.latestLoadPercent(),
                features.latestOverheatingCount(),
                features.repairsLastYear()
        );
    }
}
