package com.powerassetintelligence.application.port.out;

import com.powerassetintelligence.core.ai.RiskFeatures;
import com.powerassetintelligence.core.ai.RiskScoringResult;

public interface RiskScoringPort {
    RiskScoringResult score(RiskFeatures features);
}
