package com.powerassetintelligence.application.service;

import com.powerassetintelligence.application.dto.RiskAssessmentDetailsResponse;
import com.powerassetintelligence.application.dto.RiskAssessmentResponse;
import com.powerassetintelligence.application.dto.RiskFactorResponse;
import com.powerassetintelligence.application.dto.RiskFeaturesResponse;
import com.powerassetintelligence.application.port.out.PageRequest;
import com.powerassetintelligence.application.port.out.PageResult;
import com.powerassetintelligence.application.port.out.RiskAssessmentRepositoryPort;
import com.powerassetintelligence.core.ai.CoreRiskScoringPort;
import com.powerassetintelligence.core.ai.RiskFeatures;
import com.powerassetintelligence.core.ai.RiskScoringResult;
import com.powerassetintelligence.domain.model.Asset;
import com.powerassetintelligence.domain.model.RiskAssessment;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class RiskAnalysisService {

    private final AssetService assetService;
    private final RiskFeaturesExtractor riskFeaturesExtractor;
    private final RiskAssessmentRepositoryPort riskAssessmentRepository;
    private final CoreRiskScoringPort riskEngine;
    private final java.time.Clock clock;

    public RiskAnalysisService(
            AssetService assetService,
            RiskFeaturesExtractor riskFeaturesExtractor,
            RiskAssessmentRepositoryPort riskAssessmentRepository,
            CoreRiskScoringPort riskEngine,
            java.time.Clock clock
    ) {
        this.assetService = assetService;
        this.riskFeaturesExtractor = riskFeaturesExtractor;
        this.riskAssessmentRepository = riskAssessmentRepository;
        this.riskEngine = riskEngine;
        this.clock = clock;
    }

    @Transactional
    public RiskAssessmentDetailsResponse assess(UUID assetId) {
        Asset asset = assetService.getAsset(assetId);
        RiskFeatures features = riskFeaturesExtractor.extract(asset);
        RiskScoringResult scoringResult = riskEngine.score(features);
        RiskAssessment assessment = new RiskAssessment(
                UUID.randomUUID(),
                asset.getId(),
                Instant.now(clock),
                scoringResult.riskScore(),
                scoringResult.riskLevel(),
                scoringResult.structuredRiskFactors(),
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

    private RiskAssessmentResponse toResponse(RiskAssessment assessment) {
        return new RiskAssessmentResponse(
                assessment.id(),
                assessment.assetId(),
                assessment.assessedAt(),
                assessment.riskScore(),
                assessment.riskLevel(),
                assessment.riskFactors().stream()
                        .map(RiskFactorResponse::from)
                        .toList(),
                assessment.recommendations(),
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
                features.repairsLastYear(),
                features.averageTemperatureCelsius(),
                features.maxTemperatureCelsius(),
                features.averageLoadPercent(),
                features.maxLoadPercent(),
                features.overheatingEventsLast24Hours(),
                features.temperatureTrendCelsiusPerHour(),
                features.loadTrendPercentPerHour()
        );
    }
}
