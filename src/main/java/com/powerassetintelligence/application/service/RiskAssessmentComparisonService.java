package com.powerassetintelligence.application.service;

import com.powerassetintelligence.application.dto.RiskAssessmentComparisonResponse;
import com.powerassetintelligence.application.dto.RiskFactorChangeResponse;
import com.powerassetintelligence.application.dto.RiskFactorResponse;
import com.powerassetintelligence.application.port.out.PageRequest;
import com.powerassetintelligence.application.port.out.PageResult;
import com.powerassetintelligence.application.port.out.RiskAssessmentRepositoryPort;
import com.powerassetintelligence.core.ai.RiskChangeDirection;
import com.powerassetintelligence.domain.model.RiskAssessment;
import com.powerassetintelligence.domain.model.RiskLevel;
import com.powerassetintelligence.core.ai.RiskFactor;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service responsible for comparing risk assessments over time.
 */
@Service
@Transactional(readOnly = true)
public class RiskAssessmentComparisonService {

    private final RiskAssessmentRepositoryPort repository;

    public RiskAssessmentComparisonService(RiskAssessmentRepositoryPort repository) {
        this.repository = repository;
    }

    /**
     * Compares the latest assessment with the previous one for the given asset.
     */
    public RiskAssessmentComparisonResponse compareLatest(UUID assetId) {
        Optional<RiskAssessment> currentOpt = repository.findFirstByAssetIdOrderByAssessedAtDesc(assetId);
        if (currentOpt.isEmpty()) {
            throw new IllegalStateException("No risk assessment found for asset: " + assetId);
        }
        RiskAssessment current = currentOpt.get();

        // Get previous assessment (second newest)
        Optional<RiskAssessment> previousOpt = findPreviousAssessment(assetId);

        BigDecimal currentScore = current.riskScore();
        BigDecimal previousScore = previousOpt.map(RiskAssessment::riskScore).orElse(null);
        BigDecimal scoreDelta = calculateScoreDelta(currentScore, previousScore);

        RiskLevel currentLevel = current.riskLevel();
        RiskLevel previousLevel = previousOpt.map(RiskAssessment::riskLevel).orElse(null);
        RiskChangeDirection direction = calculateDirection(currentScore, previousScore, currentLevel, previousLevel);

        List<RiskFactorChangeResponse> factorChanges = compareFactors(
                previousOpt.map(RiskAssessment::riskFactors).orElse(List.of()),
                current.riskFactors()
        );

        return new RiskAssessmentComparisonResponse(
                currentScore,
                previousScore,
                scoreDelta,
                currentLevel,
                previousLevel,
                direction,
                factorChanges
        );
    }

    /**
     * Finds the assessment immediately before the latest one.
     * Since assessments are fetched DESC by assessedAt, the previous is at index 1.
     */
    private Optional<RiskAssessment> findPreviousAssessment(UUID assetId) {
        var pageRequest = new PageRequest(0, 1000, List.of(
                new PageRequest.SortOrder("assessedAt", PageRequest.SortOrder.Direction.DESC)
        ));

        PageResult<RiskAssessment> pageResult = repository.findByAssetId(assetId, pageRequest);
        List<RiskAssessment> allAssessments = pageResult.content();

        // With DESC sorting: index 0 = most recent, index 1 = previous
        if (allAssessments.size() <= 1) {
            return Optional.empty();
        }

        return Optional.of(allAssessments.get(1));
    }

    private BigDecimal calculateScoreDelta(BigDecimal currentScore, BigDecimal previousScore) {
        if (previousScore == null) {
            return null;
        }
        return currentScore.subtract(previousScore);
    }

    private RiskChangeDirection calculateDirection(BigDecimal currentScore, BigDecimal previousScore,
            RiskLevel currentLevel, RiskLevel previousLevel) {
        if (previousScore == null) {
            return RiskChangeDirection.NO_PREVIOUS_ASSESSMENT;
        }

        int delta = currentScore.compareTo(previousScore);
        if (delta > 0) {
            return RiskChangeDirection.INCREASED;
        } else if (delta < 0) {
            return RiskChangeDirection.DECREASED;
        } else {
            if (currentLevel != previousLevel && riskLevelRank(currentLevel) > riskLevelRank(previousLevel)) {
                return RiskChangeDirection.INCREASED;
            } else if (currentLevel != previousLevel) {
                return RiskChangeDirection.DECREASED;
            }
            return RiskChangeDirection.UNCHANGED;
        }
    }

    private List<RiskFactorChangeResponse> compareFactors(List<RiskFactor> previousFactors,
            List<RiskFactor> currentFactors) {
        Map<String, RiskFactor> previousMap = factorMap(previousFactors);
        Map<String, RiskFactor> currentMap = factorMap(currentFactors);

        Set<String> allCodes = new HashSet<>();
        allCodes.addAll(previousMap.keySet());
        allCodes.addAll(currentMap.keySet());

        List<RiskFactorChangeResponse> changes = new ArrayList<>();

        for (String code : allCodes) {
            RiskFactor previous = previousMap.get(code);
            RiskFactor current = currentMap.get(code);

            BigDecimal contributionDelta = null;
            if (previous != null && current != null
                    && previous.contribution() != null && current.contribution() != null) {
                contributionDelta = current.contribution().subtract(previous.contribution());
            }

            changes.add(new RiskFactorChangeResponse(
                    code,
                    RiskFactorResponse.from(previous),
                    RiskFactorResponse.from(current),
                    contributionDelta
            ));
        }

        // Sort: appeared factors first, then changed, then disappeared
        changes.sort((a, b) -> {
            boolean aAppeared = a.previous() == null && a.current() != null;
            boolean bAppeared = b.previous() == null && b.current() != null;
            if (aAppeared && !bAppeared) return -1;
            if (!aAppeared && bAppeared) return 1;
            return a.code().compareTo(b.code());
        });

        return changes;
    }

    private Map<String, RiskFactor> factorMap(List<RiskFactor> factors) {
        Map<String, RiskFactor> map = new HashMap<>();
        if (factors != null) {
            for (RiskFactor factor : factors) {
                map.put(factor.code(), factor);
            }
        }
        return map;
    }

    private int riskLevelRank(RiskLevel level) {
        return switch (level) {
            case LOW -> 1;
            case MEDIUM -> 2;
            case HIGH -> 3;
            case CRITICAL -> 4;
        };
    }
}
