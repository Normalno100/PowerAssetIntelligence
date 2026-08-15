package com.powerassetintelligence.infrastructure.persistence.adapter;

import com.powerassetintelligence.application.port.out.PageRequest;
import com.powerassetintelligence.application.port.out.PageResult;
import com.powerassetintelligence.application.port.out.RiskAssessmentRepositoryPort;
import com.powerassetintelligence.infrastructure.persistence.entity.AssetEntity;
import com.powerassetintelligence.infrastructure.persistence.entity.RiskAssessment;
import com.powerassetintelligence.infrastructure.persistence.mapper.PersistenceMapper;
import com.powerassetintelligence.infrastructure.persistence.repository.AssetRepository;
import com.powerassetintelligence.infrastructure.persistence.repository.RiskAssessmentRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class RiskAssessmentPersistenceAdapter implements RiskAssessmentRepositoryPort {

    private final RiskAssessmentRepository riskAssessmentRepository;
    private final AssetRepository assetRepository;

    public RiskAssessmentPersistenceAdapter(
            RiskAssessmentRepository riskAssessmentRepository,
            AssetRepository assetRepository
    ) {
        this.riskAssessmentRepository = riskAssessmentRepository;
        this.assetRepository = assetRepository;
    }

    @Override
    public com.powerassetintelligence.domain.model.RiskAssessment save(
            com.powerassetintelligence.domain.model.RiskAssessment domainAssessment) {
        AssetEntity asset = assetRepository.findById(domainAssessment.assetId())
                .orElseThrow(() -> new IllegalArgumentException("Asset not found: " + domainAssessment.assetId()));

        var entity = new RiskAssessment(
                domainAssessment.id(),
                asset,
                domainAssessment.assessedAt(),
                domainAssessment.riskScore(),
                domainAssessment.riskLevel(),
                domainAssessment.riskFactors(),
                domainAssessment.recommendations(),
                domainAssessment.modelVersion(),
                domainAssessment.explanation(),
                domainAssessment.createdAt(),
                domainAssessment.snapshot()
        );
        return PersistenceMapper.toDomain(riskAssessmentRepository.save(entity));
    }

    @Override
    public Optional<com.powerassetintelligence.domain.model.RiskAssessment> findFirstByAssetIdOrderByAssessedAtDesc(
            UUID assetId) {
        return riskAssessmentRepository.findFirstByAssetIdOrderByAssessedAtDesc(assetId)
                .map(PersistenceMapper::toDomain);
    }

    @Override
    public Optional<com.powerassetintelligence.domain.model.RiskAssessment> findFirstByAssetIdOrderByAssessedAtAsc(
            UUID assetId) {
        return riskAssessmentRepository.findFirstByAssetIdOrderByAssessedAtAsc(assetId)
                .map(PersistenceMapper::toDomain);
    }

    @Override
    public PageResult<com.powerassetintelligence.domain.model.RiskAssessment> findByAssetId(
            UUID assetId, PageRequest pageRequest) {
        var springPageable = PersistenceMapper.toSpringPageable(pageRequest);
        var result = riskAssessmentRepository.findByAssetId(assetId, springPageable);
        return new PageResult<>(
                result.getContent().stream().map(PersistenceMapper::toDomain).toList(),
                result.getNumber(), result.getSize(), result.getTotalElements(), result.getTotalPages());
    }

    @Override
    public PageResult<com.powerassetintelligence.domain.model.RiskAssessment> findAll(PageRequest pageRequest) {
        var springPageable = PersistenceMapper.toSpringPageable(pageRequest);
        var result = riskAssessmentRepository.findAll(springPageable);
        return new PageResult<>(
                result.getContent().stream().map(PersistenceMapper::toDomain).toList(),
                result.getNumber(), result.getSize(), result.getTotalElements(), result.getTotalPages());
    }

    @Override
    public List<com.powerassetintelligence.domain.model.RiskAssessment> findByAssetIdOrderByAssessedAtAsc(UUID assetId) {
        return riskAssessmentRepository.findByAssetIdOrderByAssessedAtAsc(assetId)
                .stream()
                .map(PersistenceMapper::toDomain)
                .toList();
    }
}
