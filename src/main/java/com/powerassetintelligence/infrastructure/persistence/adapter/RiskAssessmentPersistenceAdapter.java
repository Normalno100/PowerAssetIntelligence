package com.powerassetintelligence.infrastructure.persistence.adapter;

import com.powerassetintelligence.application.port.out.PageRequest;
import com.powerassetintelligence.application.port.out.PageResult;
import com.powerassetintelligence.application.port.out.RiskAssessmentRepositoryPort;
import com.powerassetintelligence.domain.model.RiskAssessment;
import com.powerassetintelligence.infrastructure.persistence.mapper.PersistenceMapper;
import com.powerassetintelligence.infrastructure.persistence.repository.RiskAssessmentRepository;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class RiskAssessmentPersistenceAdapter implements RiskAssessmentRepositoryPort {
    private final RiskAssessmentRepository repository;
    public RiskAssessmentPersistenceAdapter(RiskAssessmentRepository repository) { this.repository = repository; }
    @Override public RiskAssessment save(RiskAssessment assessment) {
        var entity = new com.powerassetintelligence.infrastructure.persistence.entity.RiskAssessment(assessment.id(), null,
                assessment.assessedAt(), assessment.riskScore(), assessment.riskLevel(), assessment.riskFactors(),
                assessment.recommendations(), assessment.modelVersion(), assessment.explanation());
        return PersistenceMapper.toDomain(repository.save(entity));
    }
    @Override public Optional<RiskAssessment> findFirstByAssetIdOrderByAssessedAtDesc(UUID assetId) { return repository.findFirstByAssetIdOrderByAssessedAtDesc(assetId).map(PersistenceMapper::toDomain); }
    @Override public PageResult<RiskAssessment> findByAssetId(UUID assetId, PageRequest pageRequest) {
        var springPageable = PersistenceMapper.toSpringPageable(pageRequest);
        var result = repository.findByAssetId(assetId, springPageable);
        return new PageResult<>(result.getContent().stream().map(PersistenceMapper::toDomain).toList(),
                result.getNumber(), result.getSize(), result.getTotalElements(), result.getTotalPages());
    }
    @Override public PageResult<RiskAssessment> findAll(PageRequest pageRequest) {
        var springPageable = PersistenceMapper.toSpringPageable(pageRequest);
        var result = repository.findAll(springPageable);
        return new PageResult<>(result.getContent().stream().map(PersistenceMapper::toDomain).toList(),
                result.getNumber(), result.getSize(), result.getTotalElements(), result.getTotalPages());
    }
}
