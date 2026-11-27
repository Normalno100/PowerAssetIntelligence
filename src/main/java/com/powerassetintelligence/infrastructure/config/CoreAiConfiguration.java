package com.powerassetintelligence.infrastructure.config;

import com.powerassetintelligence.core.ai.CoreRiskScoringPort;
import com.powerassetintelligence.core.ai.DeterministicRiskExplanationService;
import com.powerassetintelligence.core.ai.RuleBasedRiskEngine;
import com.powerassetintelligence.core.ai.RiskExplanationService;
import com.powerassetintelligence.infrastructure.ai.ml.MlScoringService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * Infrastructure configuration that wires core AI components.
 *
 * <p>Core AI classes ({@link RuleBasedRiskEngine},
 * {@link DeterministicRiskExplanationService}) have no Spring annotations —
 * this configuration class is the single place where they are registered as
 * Spring beans, preserving the purity of the {@code core.ai} package.
 */
@Configuration
public class CoreAiConfiguration {

    @Bean
    @Primary
    CoreRiskScoringPort coreRiskScoringPort(MlScoringService mlScoringService) {
        return mlScoringService;
    }

    /**
     * Rule-based engine is still registered as a bean so it can be injected
     * into {@link MlScoringService} and used as a fallback.
     */
    @Bean
    RuleBasedRiskEngine ruleBasedRiskEngine() {
        return new RuleBasedRiskEngine();
    }

    @Bean
    RiskExplanationService riskExplanationService() {
        return new DeterministicRiskExplanationService();
    }
}
