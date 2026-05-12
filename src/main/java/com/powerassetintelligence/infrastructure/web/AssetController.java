package com.powerassetintelligence.infrastructure.web;

import com.powerassetintelligence.application.dto.AssetCreateRequest;
import com.powerassetintelligence.application.dto.AssetResponse;
import com.powerassetintelligence.application.dto.AssetUpdateRequest;
import com.powerassetintelligence.application.service.AssetService;
import com.powerassetintelligence.domain.model.AssetCriticality;
import com.powerassetintelligence.domain.model.AssetStatus;
import com.powerassetintelligence.domain.model.AssetType;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/assets")
public class AssetController {

    private final AssetService assetService;

    public AssetController(AssetService assetService) {
        this.assetService = assetService;
    }

    @PostMapping
    public ResponseEntity<AssetResponse> create(@Valid @RequestBody AssetCreateRequest request) {
        AssetResponse response = assetService.create(request);
        return ResponseEntity.created(URI.create("/api/v1/assets/" + response.id())).body(response);
    }

    @GetMapping
    public Page<AssetResponse> search(
            @RequestParam(required = false) AssetType type,
            @RequestParam(required = false) AssetStatus status,
            @RequestParam(required = false) AssetCriticality criticality,
            @RequestParam(required = false) String location,
            @PageableDefault(size = 20, sort = "name") Pageable pageable
    ) {
        return assetService.search(type, status, criticality, location, pageable);
    }

    @GetMapping("/{assetId}")
    public AssetResponse getById(@PathVariable UUID assetId) {
        return assetService.getById(assetId);
    }

    @PatchMapping("/{assetId}")
    public AssetResponse update(@PathVariable UUID assetId, @Valid @RequestBody AssetUpdateRequest request) {
        return assetService.update(assetId, request);
    }

    @PostMapping("/{assetId}/decommission")
    public AssetResponse decommission(@PathVariable UUID assetId) {
        return assetService.decommission(assetId);
    }
}
