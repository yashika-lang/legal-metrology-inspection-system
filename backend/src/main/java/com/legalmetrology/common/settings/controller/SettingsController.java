package com.legalmetrology.common.settings.controller;

import com.legalmetrology.common.response.ApiResponse;
import com.legalmetrology.common.settings.dto.SettingsRequest;
import com.legalmetrology.common.settings.dto.SettingsResponse;
import com.legalmetrology.common.settings.service.SettingsService;
import com.legalmetrology.security.SecurityUtils;
import com.legalmetrology.utils.ResponseUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Settings", description = "System-wide configuration (admin only)")
@RestController
@RequestMapping("/api/v1/settings")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class SettingsController {

    private final SettingsService settingsService;
    private final SecurityUtils securityUtils;

    @Operation(summary = "List all system settings")
    @GetMapping
    public ResponseEntity<ApiResponse<List<SettingsResponse>>> listAll() {
        return ResponseUtil.ok(settingsService.listAll());
    }

    @Operation(summary = "Get a single setting by key")
    @GetMapping("/{key}")
    public ResponseEntity<ApiResponse<SettingsResponse>> getByKey(@PathVariable String key) {
        return ResponseUtil.ok(settingsService.getByKey(key));
    }

    @Operation(summary = "Create or update a setting")
    @PutMapping
    public ResponseEntity<ApiResponse<SettingsResponse>> upsert(@Valid @RequestBody SettingsRequest request) {
        return ResponseUtil.ok("Setting saved successfully", settingsService.upsert(request, securityUtils.getCurrentUserId()));
    }
}
