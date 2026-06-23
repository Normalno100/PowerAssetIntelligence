package com.powerassetintelligence.infrastructure.ai;

import com.powerassetintelligence.application.port.out.RiskScoringPort;
import com.powerassetintelligence.core.ai.RiskFeatures;
import com.powerassetintelligence.core.ai.RiskScoringResult;
import com.powerassetintelligence.core.ai.RuleBasedRiskEngine;
import org.springframework.stereotype.Component;

@Component
public class RuleBasedRiskScoringAdapter implements RiskScoringPort {
    private final RuleBasedRiskEngine engine = new RuleBasedRiskEngine();
    @Override public RiskScoringResult score(RiskFeatures features) { return engine.score(features); }
}
