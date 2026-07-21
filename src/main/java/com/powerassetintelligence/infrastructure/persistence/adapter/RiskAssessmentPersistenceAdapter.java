package com.powerassetintelligence.infrastructure.persistence.adapter;

import com.powerassetintelligence.application.port.out.RiskAssessmentRepositoryPort;
import com.powerassetintelligence.domain.model.Asset;
import com.powerassetintelligence.domain.model.RiskAssessment;
import com.powerassetintelligence.infrastructure.persistence.mapper.PersistenceMapper;
import com.powerassetintelligence.infrastructure.persistence.repository.AssetRepository;
import com.powerassetintelligence.infrastructure.persistence.repository.RiskAssessmentRepository;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

@Component
public class RiskAssessmentPersistenceAdapter implements RiskAssessmentRepositoryPort {
    private final RiskAssessmentRepository repository;
    private final AssetRepository assetRepository;
    public RiskAssessmentPersistenceAdapter(RiskAssessmentRepository repository, AssetRepository assetRepository) { this.repository = repository; this.assetRepository = assetRepository; }
    @Override public RiskAssessment save(RiskAssessment assessment, Asset asset) {
        var assetEntity = assetRepository.findById(asset.getId()).orElseThrow();
        assetEntity.setType(asset.getType()); assetEntity.setName(asset.getName()); assetEntity.setInstallationDate(asset.getInstallationDate());
        assetEntity.setStatus(asset.getStatus()); assetEntity.setLocation(asset.getLocation()); assetEntity.setManufacturer(asset.getManufacturer());
        assetEntity.setCriticality(asset.getCriticality()); assetEntity.setExpectedServiceLifeYears(asset.getExpectedServiceLifeYears());
        assetEntity.setTechnicalParameters(asset.getTechnicalParameters());
        var entity = new com.powerassetintelligence.infrastructure.persistence.entity.RiskAssessment(assessment.id(), assetEntity,
                assessment.assessedAt(), assessment.riskScore(), assessment.riskLevel(), assessment.riskFactors(),
                assessment.recommendations(), assessment.modelVersion(), assessment.explanation());
        return PersistenceMapper.toDomain(repository.save(entity));
    }
    @Override public Optional<RiskAssessment> findFirstByAssetIdOrderByAssessedAtDesc(UUID assetId) { return repository.findFirstByAssetIdOrderByAssessedAtDesc(assetId).map(PersistenceMapper::toDomain); }
    @Override public Page<RiskAssessment> findByAssetId(UUID assetId, Pageable pageable) { return repository.findByAssetId(assetId, pageable).map(PersistenceMapper::toDomain); }
    @Override public Page<RiskAssessment> findAll(Pageable pageable) { return repository.findAll(pageable).map(PersistenceMapper::toDomain); }
}
