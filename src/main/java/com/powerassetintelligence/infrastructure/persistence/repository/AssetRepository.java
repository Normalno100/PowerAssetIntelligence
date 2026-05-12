package com.powerassetintelligence.infrastructure.persistence.repository;

import com.powerassetintelligence.domain.model.AssetCriticality;
import com.powerassetintelligence.domain.model.AssetStatus;
import com.powerassetintelligence.domain.model.AssetType;
import com.powerassetintelligence.infrastructure.persistence.entity.Asset;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AssetRepository extends JpaRepository<Asset, UUID> {

    @Query("""
            select a from Asset a
            where (:type is null or a.type = :type)
              and (:status is null or a.status = :status)
              and (:criticality is null or a.criticality = :criticality)
              and (:location is null or lower(a.location) like lower(concat('%', :location, '%')))
            """)
    Page<Asset> search(
            @Param("type") AssetType type,
            @Param("status") AssetStatus status,
            @Param("criticality") AssetCriticality criticality,
            @Param("location") String location,
            Pageable pageable
    );
}
