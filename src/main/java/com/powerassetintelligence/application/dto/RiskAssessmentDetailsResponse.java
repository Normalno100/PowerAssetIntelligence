package com.powerassetintelligence.application.dto;

public record RiskAssessmentDetailsResponse(
        RiskAssessmentResponse assessment,
        RiskFeaturesResponse features
) {
}
