package com.powerassetintelligence.application.port.out;

import com.powerassetintelligence.domain.model.RiskAssessment;
import java.util.Optional;
import java.util.UUID;

public interface RiskAssessmentRepositoryPort {
    RiskAssessment save(RiskAssessment assessment);
    Optional<RiskAssessment> findFirstByAssetIdOrderByAssessedAtDesc(UUID assetId);
    PageResult<RiskAssessment> findByAssetId(UUID assetId, PageRequest pageRequest);
    PageResult<RiskAssessment> findAll(PageRequest pageRequest);
}
