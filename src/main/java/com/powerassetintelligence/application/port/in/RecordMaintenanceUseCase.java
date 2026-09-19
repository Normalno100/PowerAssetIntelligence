package com.powerassetintelligence.application.port.in;

import com.powerassetintelligence.application.dto.MaintenanceCreateCommand;
import com.powerassetintelligence.application.dto.MaintenanceResponse;
import java.util.UUID;

/**
 * Input port for recording a maintenance event.
 */
public interface RecordMaintenanceUseCase {
    MaintenanceResponse execute(UUID assetId, MaintenanceCreateCommand command);
}
