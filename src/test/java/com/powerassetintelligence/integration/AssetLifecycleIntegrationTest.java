package com.powerassetintelligence.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.powerassetintelligence.domain.model.AssetCriticality;
import com.powerassetintelligence.domain.model.AssetStatus;
import com.powerassetintelligence.domain.model.AssetType;
import com.powerassetintelligence.infrastructure.persistence.entity.AssetEntity;
import com.powerassetintelligence.infrastructure.persistence.repository.AssetRepository;
import com.powerassetintelligence.testsupport.BaseIntegrationTest;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class AssetLifecycleIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private AssetRepository assetRepository;

    @Test
    void endToEndCreateUpdateDecommission() {
        UUID assetId = UUID.randomUUID();

        AssetEntity saved = assetRepository.saveAndFlush(new AssetEntity(
                assetId,
                AssetType.TRANSFORMER,
                "TX-999",
                LocalDate.of(2022, 1, 1),
                AssetStatus.ACTIVE,
                "Substation-North",
                "ABB",
                AssetCriticality.HIGH,
                30,
                Map.of("kv", "110", "phase", "3")
        ));

        assertNotNull(saved.getId());
        assertEquals("TX-999", saved.getName());
        assertEquals("Substation-North", saved.getLocation());

        AssetEntity updated = assetRepository.findById(assetId).orElseThrow();
        updated.setLocation("Substation-South");
        updated.setManufacturer("Siemens");
        assetRepository.saveAndFlush(updated);

        AssetEntity reloaded = assetRepository.findById(assetId).orElseThrow();
        assertEquals("Substation-South", reloaded.getLocation());
        assertEquals("Siemens", reloaded.getManufacturer());

        reloaded.setStatus(AssetStatus.DECOMMISSIONED);
        assetRepository.saveAndFlush(reloaded);

        AssetEntity decommissioned = assetRepository.findById(assetId).orElseThrow();
        assertEquals("TX-999", decommissioned.getName());
    }
}
