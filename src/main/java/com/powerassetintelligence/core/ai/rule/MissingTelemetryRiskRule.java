package com.powerassetintelligence.core.ai.rule;

import com.powerassetintelligence.core.ai.RiskFactor;
import com.powerassetintelligence.core.ai.RiskFactorSeverity;
import com.powerassetintelligence.core.ai.RiskFeatures;
import com.powerassetintelligence.core.ai.RiskRule;
import com.powerassetintelligence.core.ai.RiskRuleResult;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public class MissingTelemetryRiskRule implements RiskRule {

    @Override
    public Optional<RiskRuleResult> evaluate(RiskFeatures features) {
        if (!features.hasTelemetry()) {
            RiskFactor factor = RiskFactor.of(
                    "MISSING_TELEMETRY",
                    RiskFactorSeverity.MEDIUM,
                    "No telemetry is available for risk scoring",
                    BigDecimal.valueOf(15)
            );
            return Optional.of(RiskRuleResult.of(
                    "MISSING_TELEMETRY",
                    factor,
                    List.of("Upload telemetry or perform field diagnostics")
            ));
        }
        return Optional.empty();
    }
}
