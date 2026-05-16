package com.powerassetintelligence.core.ai;

import java.util.Optional;

public interface RiskRule {

    Optional<RiskRuleResult> evaluate(RiskFeatures features);
}
