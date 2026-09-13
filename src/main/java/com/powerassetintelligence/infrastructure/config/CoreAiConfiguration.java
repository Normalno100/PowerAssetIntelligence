package com.powerassetintelligence.infrastructure.config;

import com.powerassetintelligence.core.ai.CoreRiskScoringPort;
import com.powerassetintelligence.core.ai.DeterministicRiskExplanationService;
import com.powerassetintelligence.core.ai.RuleBasedRiskEngine;
import com.powerassetintelligence.core.ai.RiskExplanationService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

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
    CoreRiskScoringPort coreRiskScoringPort() {
        return new RuleBasedRiskEngine();
    }

    @Bean
    RiskExplanationService riskExplanationService() {
        return new DeterministicRiskExplanationService();
    }
}
