package com.powerassetintelligence.core.ai;

public interface CoreRiskScoringPort {
    RiskScoringResult score(RiskFeatures features);
}
