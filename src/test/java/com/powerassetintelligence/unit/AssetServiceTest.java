package com.powerassetintelligence.unit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.powerassetintelligence.application.dto.AssetCreateRequest;
import com.powerassetintelligence.application.dto.AssetResponse;
import com.powerassetintelligence.application.service.AssetService;
import com.powerassetintelligence.domain.model.AssetCriticality;
import com.powerassetintelligence.domain.model.AssetType;
import com.powerassetintelligence.infrastructure.persistence.entity.Asset;
import com.powerassetintelligence.infrastructure.persistence.repository.AssetRepository;
import java.time.LocalDate;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AssetServiceTest {

    @Mock
    private AssetRepository assetRepository;

    @InjectMocks
    private AssetService assetService;

    @Test
    void createShouldTrimFieldsAndPersistAsset() {
        AssetCreateRequest request = new AssetCreateRequest(
                AssetType.TRANSFORMER,
                "  TX-101  ",
                LocalDate.of(2022, 5, 3),
                "  North substation  ",
                "  ABB  ",
                AssetCriticality.HIGH,
                30,
                Map.of("kv", "110")
        );

        when(assetRepository.save(org.mockito.ArgumentMatchers.any(Asset.class)))
                .thenAnswer(inv -> inv.getArgument(0, Asset.class));

        AssetResponse response = assetService.create(request);

        assertEquals("TX-101", response.name());
        assertEquals("North substation", response.location());
        assertEquals("ABB", response.manufacturer());
        assertEquals("110", response.technicalParameters().get("kv"));
        verify(assetRepository).save(org.mockito.ArgumentMatchers.any(Asset.class));
    }

    @Test
    void getByIdShouldReturnSavedAsset() {
        UUID id = UUID.randomUUID();
        Asset asset = new Asset(id, AssetType.BREAKER, "BRK-1", LocalDate.of(2021, 1, 1),
                com.powerassetintelligence.domain.model.AssetStatus.ACTIVE,
                "Site-A", "GE", AssetCriticality.MEDIUM, 25, Map.of());
        when(assetRepository.findById(id)).thenReturn(Optional.of(asset));

        AssetResponse response = assetService.getById(id);

        assertEquals(id, response.id());
        assertTrue(response.technicalParameters().isEmpty());
    }
}
