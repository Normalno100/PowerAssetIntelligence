package com.powerassetintelligence.core.ai;

import com.powerassetintelligence.domain.model.RiskFeatures;
import java.util.Optional;

public interface RiskRule {

    Optional<RiskRuleResult> evaluate(RiskFeatures features);
}
