package com.powerassetintelligence.infrastructure.web;

import com.powerassetintelligence.application.dto.MaintenanceCreateRequest;
import com.powerassetintelligence.application.dto.MaintenanceResponse;
import com.powerassetintelligence.application.service.MaintenanceService;
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
@RequestMapping("/api/v1/assets/{assetId}/maintenance-records")
public class MaintenanceController {

    private final MaintenanceService maintenanceService;

    public MaintenanceController(MaintenanceService maintenanceService) {
        this.maintenanceService = maintenanceService;
    }

    @PostMapping
    public ResponseEntity<MaintenanceResponse> create(
            @PathVariable UUID assetId,
            @Valid @RequestBody MaintenanceCreateRequest request
    ) {
        MaintenanceResponse response = maintenanceService.create(assetId, request);
        return ResponseEntity.created(URI.create("/api/v1/assets/" + assetId + "/maintenance-records/" + response.id()))
                .body(response);
    }

    @GetMapping
    public Page<MaintenanceResponse> findByAsset(
            @PathVariable UUID assetId,
            @PageableDefault(size = 50, sort = "repairDate") Pageable pageable
    ) {
        return maintenanceService.findByAsset(assetId, pageable);
    }
}
