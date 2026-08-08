package com.powerassetintelligence.infrastructure.web.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * HTTP request DTO for telemetry creation.
 *
 * This DTO belongs to the infrastructure layer as it represents
 * an HTTP request contract. Validation annotations are enforced
 * by Spring MVC's @Valid in the controller.
 */
public record TelemetryCreateRequest(
        @NotNull UUID assetId,
        @NotNull @PastOrPresent Instant timestamp,
        @DecimalMin("-80.00") @DecimalMax("250.00") BigDecimal temperatureCelsius,
        @DecimalMin("0.00") @DecimalMax("150.00") BigDecimal loadPercent,
        @PositiveOrZero BigDecimal voltageKv,
        @PositiveOrZero BigDecimal currentAmpere,
        @PositiveOrZero BigDecimal vibrationMmSec,
        @PositiveOrZero Integer overheatingCount,
        @NotBlank @Size(max = 128) String sourceSensorId,
        @Size(max = 128) String externalTelemetryId
) {
}
