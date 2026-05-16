package com.powerassetintelligence.infrastructure.persistence.repository;

import com.powerassetintelligence.infrastructure.persistence.entity.RiskAssessment;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RiskAssessmentRepository extends JpaRepository<RiskAssessment, UUID> {

    Page<RiskAssessment> findByAssetId(UUID assetId, Pageable pageable);

    Optional<RiskAssessment> findFirstByAssetIdOrderByAssessedAtDesc(UUID assetId);
}
