package com.powerassetintelligence.application.dto;

import com.powerassetintelligence.domain.model.AssetCriticality;
import com.powerassetintelligence.domain.model.AssetStatus;
import com.powerassetintelligence.domain.model.AssetType;
import java.math.BigDecimal;
import java.util.UUID;

public record RiskFeaturesResponse(
        UUID assetId,
        AssetType assetType,
        AssetStatus assetStatus,
        AssetCriticality criticality,
        int assetAgeYears,
        BigDecimal latestTemperatureCelsius,
        BigDecimal latestLoadPercent,
        Integer latestOverheatingCount,
        long repairsLastYear
) {
}
