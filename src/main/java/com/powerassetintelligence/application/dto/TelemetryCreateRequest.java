package com.powerassetintelligence.application.dto;

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
