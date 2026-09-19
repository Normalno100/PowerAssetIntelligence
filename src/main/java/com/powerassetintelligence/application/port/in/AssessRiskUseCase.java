package com.powerassetintelligence.application.port.in;

import com.powerassetintelligence.application.dto.RiskAssessmentDetailsResponse;
import java.util.UUID;

/**
 * Input port for triggering risk assessment on an asset.
 */
public interface AssessRiskUseCase {
    RiskAssessmentDetailsResponse execute(UUID assetId);
}
