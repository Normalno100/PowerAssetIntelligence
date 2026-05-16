package com.powerassetintelligence.application.service;

import com.powerassetintelligence.application.dto.RiskAssessmentDetailsResponse;
import com.powerassetintelligence.application.dto.RiskAssessmentResponse;
import com.powerassetintelligence.application.dto.RiskFeaturesResponse;
import com.powerassetintelligence.core.ai.RiskFeatures;
import com.powerassetintelligence.core.ai.RiskScoringResult;
import com.powerassetintelligence.core.ai.RuleBasedRiskEngine;
import com.powerassetintelligence.infrastructure.persistence.entity.Asset;
import com.powerassetintelligence.infrastructure.persistence.entity.RiskAssessment;
import com.powerassetintelligence.infrastructure.persistence.entity.TelemetryRecord;
import com.powerassetintelligence.infrastructure.persistence.repository.MaintenanceRecordRepository;
import com.powerassetintelligence.infrastructure.persistence.repository.RiskAssessmentRepository;
import com.powerassetintelligence.infrastructure.persistence.repository.TelemetryRecordRepository;
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
    private final TelemetryRecordRepository telemetryRecordRepository;
    private final MaintenanceRecordRepository maintenanceRecordRepository;
    private final RiskAssessmentRepository riskAssessmentRepository;
    private final RuleBasedRiskEngine riskEngine;
    private final Clock clock;

    public RiskAnalysisService(
            AssetService assetService,
            TelemetryRecordRepository telemetryRecordRepository,
            MaintenanceRecordRepository maintenanceRecordRepository,
            RiskAssessmentRepository riskAssessmentRepository,
            RuleBasedRiskEngine riskEngine
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
        RiskFeatures features = extractFeatures(asset);
        RiskScoringResult scoringResult = riskEngine.score(features);
        RiskAssessment assessment = new RiskAssessment(
                UUID.randomUUID(),
                asset,
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
                toFeaturesResponse(features)
        );
    }

    public RiskAssessmentResponse getLatest(UUID assetId) {
        assetService.getAsset(assetId);
        return riskAssessmentRepository.findFirstByAssetIdOrderByAssessedAtDesc(assetId)
                .map(this::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Risk assessment not found for asset: " + assetId));
    }

    public Page<RiskAssessmentResponse> findByAsset(UUID assetId, Pageable pageable) {
        assetService.getAsset(assetId);
        return riskAssessmentRepository.findByAssetId(assetId, pageable).map(this::toResponse);
    }

    public Page<RiskAssessmentResponse> findTopRisky(Pageable pageable) {
        return riskAssessmentRepository.findAll(pageable).map(this::toResponse);
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
                latestTelemetry == null ? null : latestTelemetry.getTemperatureCelsius(),
                latestTelemetry == null ? null : latestTelemetry.getLoadPercent(),
                latestTelemetry == null ? null : latestTelemetry.getOverheatingCount(),
                repairsLastYear
        );
    }

    private RiskAssessmentResponse toResponse(RiskAssessment assessment) {
        return new RiskAssessmentResponse(
                assessment.getId(),
                assessment.getAsset().getId(),
                assessment.getAssessedAt(),
                assessment.getRiskScore(),
                assessment.getRiskLevel(),
                List.copyOf(assessment.getRiskFactors()),
                List.copyOf(assessment.getRecommendations()),
                assessment.getModelVersion(),
                assessment.getExplanation(),
                assessment.getCreatedAt()
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
