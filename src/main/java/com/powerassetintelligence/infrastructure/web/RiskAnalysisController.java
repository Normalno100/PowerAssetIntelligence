package com.powerassetintelligence.infrastructure.web;

import com.powerassetintelligence.application.dto.RiskAssessmentComparisonResponse;
import com.powerassetintelligence.application.dto.RiskAssessmentDetailsResponse;
import com.powerassetintelligence.application.dto.RiskAssessmentResponse;
import com.powerassetintelligence.application.port.out.PageResult;
import com.powerassetintelligence.application.service.RiskAnalysisService;
import com.powerassetintelligence.application.service.RiskAssessmentComparisonService;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.powerassetintelligence.infrastructure.web.WebPageMapper;

@RestController
@RequestMapping("/api/v1")
public class RiskAnalysisController {

    private final RiskAnalysisService riskAnalysisService;
    private final RiskAssessmentComparisonService comparisonService;

    public RiskAnalysisController(
            RiskAnalysisService riskAnalysisService,
            RiskAssessmentComparisonService comparisonService
    ) {
        this.riskAnalysisService = riskAnalysisService;
        this.comparisonService = comparisonService;
    }

    @PostMapping("/assets/{assetId}/risk-assessments")
    public RiskAssessmentDetailsResponse createAssessment(@PathVariable UUID assetId) {
        return riskAnalysisService.assess(assetId);
    }

    @GetMapping("/assets/{assetId}/risk-assessments/latest")
    public RiskAssessmentResponse getLatest(@PathVariable UUID assetId) {
        return riskAnalysisService.getLatest(assetId);
    }

    @GetMapping("/assets/{assetId}/risk-assessments/latest/comparison")
    public RiskAssessmentComparisonResponse getLatestComparison(@PathVariable UUID assetId) {
        return comparisonService.compareLatest(assetId);
    }

    @GetMapping("/assets/{assetId}/risk-assessments")
    public PageResult<RiskAssessmentResponse> findByAsset(
            @PathVariable UUID assetId,
            @PageableDefault(size = 20, sort = "assessedAt") Pageable pageable
    ) {
        var pageRequest = WebPageMapper.toPageRequest(pageable);
        var result = riskAnalysisService.findByAsset(assetId, pageRequest);
        return new PageResult<>(result.content().stream().toList(), result.page(), result.size(), result.totalElements(), result.totalPages());
    }

    @GetMapping("/risk-assessments/top-risky")
    public PageResult<RiskAssessmentResponse> findTopRisky(
            @PageableDefault(size = 20, sort = "riskScore", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        var pageRequest = WebPageMapper.toPageRequest(pageable);
        var result = riskAnalysisService.findTopRisky(pageRequest);
        return new PageResult<>(result.content().stream().toList(), result.page(), result.size(), result.totalElements(), result.totalPages());
    }
}
