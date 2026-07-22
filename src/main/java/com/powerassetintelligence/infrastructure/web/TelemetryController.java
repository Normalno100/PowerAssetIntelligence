package com.powerassetintelligence.infrastructure.web;

import com.powerassetintelligence.application.dto.TelemetryAcceptedResponse;
import com.powerassetintelligence.application.dto.TelemetryCreateRequest;
import com.powerassetintelligence.application.dto.TelemetryResponse;
import com.powerassetintelligence.application.port.out.PageResult;
import com.powerassetintelligence.application.service.TelemetryService;
import com.powerassetintelligence.infrastructure.messaging.kafka.TelemetryKafkaProducer;
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
import com.powerassetintelligence.infrastructure.web.WebPageMapper;

@RestController
@RequestMapping("/api/v1")
public class TelemetryController {

    private final TelemetryService telemetryService;
    private final TelemetryKafkaProducer telemetryKafkaProducer;

    public TelemetryController(TelemetryService telemetryService, TelemetryKafkaProducer telemetryKafkaProducer) {
        this.telemetryService = telemetryService;
        this.telemetryKafkaProducer = telemetryKafkaProducer;
    }

    @PostMapping("/telemetry")
    public ResponseEntity<TelemetryAcceptedResponse> create(@Valid @RequestBody TelemetryCreateRequest request) {
        TelemetryAcceptedResponse response = telemetryKafkaProducer.publish(request);
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
