package com.powerassetintelligence.core.ai;

import com.powerassetintelligence.application.dto.RiskFeatures;
import com.powerassetintelligence.application.dto.RiskScoringResult;

public interface CoreRiskScoringPort {
    RiskScoringResult score(RiskFeatures features);
}
