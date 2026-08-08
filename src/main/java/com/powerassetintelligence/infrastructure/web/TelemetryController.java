package com.powerassetintelligence.infrastructure.web;

import com.powerassetintelligence.application.dto.TelemetryAcceptedResponse;
import com.powerassetintelligence.application.dto.TelemetryCreateCommand;
import com.powerassetintelligence.application.dto.TelemetryResponse;
import com.powerassetintelligence.application.port.out.PageResult;
import com.powerassetintelligence.application.service.TelemetryService;
import com.powerassetintelligence.infrastructure.web.dto.TelemetryCreateRequest;
import jakarta.validation.Valid;
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

    /**
     * Accepts telemetry data from HTTP clients.
     *
     * Flow:
     *   TelemetryCreateRequest (HTTP DTO)
     *     → TelemetryWebMapper → TelemetryCreateCommand (Application)
     *       → TelemetryService.ingest() → TelemetryEventPublisher (port)
     *         → TelemetryKafkaProducer (adapter) → Kafka
     *
     * Returns 202 Accepted because telemetry is accepted for asynchronous processing.
     * This does NOT mean telemetry is persisted to the database — persistence
     * occurs later via the Kafka consumer pipeline.
     */
    @PostMapping("/telemetry")
    public ResponseEntity<TelemetryAcceptedResponse> create(@Valid @RequestBody TelemetryCreateRequest request) {
        TelemetryCreateCommand command = TelemetryWebMapper.toCommand(request);
        TelemetryAcceptedResponse response = telemetryService.ingest(command);
        return ResponseEntity.accepted().body(response);
    }

    @GetMapping("/assets/{assetId}/telemetry")
    public PageResult<TelemetryResponse> findByAsset(
            @PathVariable UUID assetId,
            @PageableDefault(size = 100, sort = "timestamp") Pageable pageable
    ) {
        var pageRequest = WebPageMapper.toPageRequest(pageable);
        var result = telemetryService.findByAsset(assetId, pageRequest);
        return new PageResult<>(result.content().stream().toList(), result.page(), result.size(), result.totalElements(), result.totalPages());
    }

    @GetMapping("/assets/{assetId}/telemetry/latest")
    public TelemetryResponse getLatest(@PathVariable UUID assetId) {
        return telemetryService.getLatest(assetId);
    }
}
