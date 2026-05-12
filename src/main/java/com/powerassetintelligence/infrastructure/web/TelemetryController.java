package com.powerassetintelligence.infrastructure.web;

import com.powerassetintelligence.application.dto.TelemetryCreateRequest;
import com.powerassetintelligence.application.dto.TelemetryResponse;
import com.powerassetintelligence.application.service.TelemetryService;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class TelemetryController {

    private final TelemetryService telemetryService;

    public TelemetryController(TelemetryService telemetryService) {
        this.telemetryService = telemetryService;
    }

    @PostMapping("/telemetry")
    public ResponseEntity<TelemetryResponse> create(@Valid @RequestBody TelemetryCreateRequest request) {
        TelemetryResponse response = telemetryService.create(request);
        return ResponseEntity.created(URI.create("/api/v1/assets/" + response.assetId() + "/telemetry/" + response.id()))
                .body(response);
    }

    @GetMapping("/assets/{assetId}/telemetry")
    public Page<TelemetryResponse> findByAsset(
            @PathVariable UUID assetId,
            @PageableDefault(size = 100, sort = "timestamp") Pageable pageable
    ) {
        return telemetryService.findByAsset(assetId, pageable);
    }

    @GetMapping("/assets/{assetId}/telemetry/latest")
    public TelemetryResponse getLatest(@PathVariable UUID assetId) {
        return telemetryService.getLatest(assetId);
    }
}
