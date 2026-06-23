package com.powerassetintelligence.application.port.out;

import com.powerassetintelligence.domain.model.Asset;
import com.powerassetintelligence.domain.model.RiskAssessment;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface RiskAssessmentRepositoryPort {
    RiskAssessment save(RiskAssessment assessment, Asset asset);
    Optional<RiskAssessment> findFirstByAssetIdOrderByAssessedAtDesc(UUID assetId);
    Page<RiskAssessment> findByAssetId(UUID assetId, Pageable pageable);
    Page<RiskAssessment> findAll(Pageable pageable);
}
