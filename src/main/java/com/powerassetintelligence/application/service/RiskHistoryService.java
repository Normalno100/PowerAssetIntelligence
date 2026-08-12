package com.powerassetintelligence.application.service;

import com.powerassetintelligence.application.dto.RiskTrendPoint;
import com.powerassetintelligence.application.dto.RiskTrendResponse;
import com.powerassetintelligence.application.port.out.RiskAssessmentRepositoryPort;
import com.powerassetintelligence.core.ai.RiskTrendCalculator;
import com.powerassetintelligence.core.ai.TrendDirection;
import com.powerassetintelligence.domain.model.RiskAssessment;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Application service for risk trend history analysis.
 * <p>
 * Responsibilities:
 * <ul>
 *   <li>Validate asset existence via {@link AssetService}</li>
 *   <li>Fetch the last N risk assessments chronologically (oldest → newest)</li>
 *   <li>Delegate pure calculation to {@link RiskTrendCalculator}</li>
 *   <li>Map domain models to DTOs</li>
 * </ul>
 */
@Service
@Transactional(readOnly = true)
public class RiskHistoryService {

    private final AssetService assetService;
    private final RiskAssessmentRepositoryPort riskAssessmentRepositoryPort;

    public RiskHistoryService(AssetService assetService,
                              RiskAssessmentRepositoryPort riskAssessmentRepositoryPort) {
        this.assetService = assetService;
        this.riskAssessmentRepositoryPort = riskAssessmentRepositoryPort;
    }

    /**
     * Returns the risk trend for a given asset.
     *
     * @param assetId the asset to analyze
     * @param limit   maximum number of <b>latest</b> assessments to include
     * @return risk trend response with points and summary statistics
     */
    public RiskTrendResponse getTrend(UUID assetId, int limit) {
        // 1. Verify asset exists
        assetService.getAsset(assetId);

        // 2. Get assessments in chronological order (oldest → newest)
        List<RiskAssessment> allAssessments =
                riskAssessmentRepositoryPort.findByAssetIdOrderByAssessedAtAsc(assetId);

        // 3. Take the last N (most recent) assessments
        List<RiskAssessment> assessments = limitAssessments(allAssessments, limit);

        // 4. Delegate pure calculation to RiskTrendCalculator
        List<BigDecimal> scores = assessments.stream()
                .map(RiskAssessment::riskScore)
                .collect(Collectors.toList());

        RiskTrendCalculator.TrendResult result = RiskTrendCalculator.calculate(scores);

        // 5. Build trend points from calculator result
        List<RiskTrendPoint> points = buildPoints(assessments, result.scoreChanges());

        // 6. Build and return response
        return new RiskTrendResponse(
                assetId,
                points,
                result.currentScore(),
                result.previousScore(),
                result.totalChange(),
                result.averageChange(),
                result.direction()
        );
    }

    private List<RiskAssessment> limitAssessments(List<RiskAssessment> all, int limit) {
        if (all.isEmpty()) {
            return Collections.emptyList();
        }
        if (all.size() > limit) {
            return all.subList(all.size() - limit, all.size());
        }
        return all;
    }

    private List<RiskTrendPoint> buildPoints(List<RiskAssessment> assessments,
                                             List<RiskTrendCalculator.ScoreChange> scoreChanges) {
        List<RiskTrendPoint> points = new ArrayList<>(assessments.size());
        for (int i = 0; i < assessments.size(); i++) {
            RiskAssessment assessment = assessments.get(i);
            RiskTrendCalculator.ScoreChange change = scoreChanges.get(i);

            points.add(new RiskTrendPoint(
                    assessment.id(),
                    assessment.assetId(),
                    assessment.assessedAt(),
                    assessment.riskScore(),
                    assessment.riskLevel(),
                    change.delta(),
                    change.trend() != null ? change.trend() : TrendDirection.STABLE
            ));
        }
        return points;
    }
}
