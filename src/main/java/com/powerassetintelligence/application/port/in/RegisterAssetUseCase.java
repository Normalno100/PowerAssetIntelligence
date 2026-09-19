package com.powerassetintelligence.application.port.in;

import com.powerassetintelligence.application.dto.AssetCreateCommand;
import com.powerassetintelligence.application.dto.AssetResponse;

/**
 * Input port for registering a new asset.
 */
public interface RegisterAssetUseCase {
    AssetResponse execute(AssetCreateCommand command);
}
