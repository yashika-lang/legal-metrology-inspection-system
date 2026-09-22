package com.legalmetrology.scanner.controller;

import com.legalmetrology.common.response.ApiResponse;
import com.legalmetrology.scanner.dto.ScanRequest;
import com.legalmetrology.scanner.dto.ScanResponse;
import com.legalmetrology.scanner.service.ScannerService;
import com.legalmetrology.utils.ResponseUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@Tag(name = "Scans", description = "Barcode/QR/manual scan logging against an inspection")
@RestController
@RequestMapping("/api/v1/scans")
@RequiredArgsConstructor
public class ScannerController {

    private final ScannerService scannerService;

    @Operation(summary = "Record a barcode, QR, or manual scan for an inspection",
            description = "If `scanType` is `BARCODE`, the scan value is looked up against product master data and, on a match, the response includes "
                    + "`matchedProductId`/`matchedProductName` — no match is not an error, those fields are simply null.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Scan recorded", content = @Content(mediaType = "application/json", examples = @ExampleObject(value = """
                    {"success":true,"message":"Resource created successfully","data":{"id":"12345678-90ab-4cde-9f01-23456789abcd","inspectionId":"7b1e2f3a-4b5c-4d6e-8f90-1a2b3c4d5e6f","scanType":"BARCODE","scanValue":"8901058854488","scannedAt":"2026-01-15T10:30:00Z","matchedProductId":"3f2a9c10-6b1e-4a2e-9c1a-1e2f3a4b5c6d","matchedProductName":"Aashirvaad Atta 5kg"},"timestamp":"2026-01-15T10:30:00Z"}
                    """))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Validation failed — `inspectionId`/`scanType` missing, or `scanValue` blank"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Missing or invalid JWT"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "No inspection exists with the given inspectionId")
    })
    @PostMapping
    public ResponseEntity<ApiResponse<ScanResponse>> recordScan(@Valid @RequestBody ScanRequest request) {
        return ResponseUtil.created(scannerService.recordScan(request));
    }

    @Operation(summary = "List all scans recorded for an inspection",
            description = "Returns an empty list (not a 404) if no scans have been recorded yet, or if the inspection id doesn't exist — this endpoint does not validate the inspection.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Scans for the inspection (may be empty)", content = @Content(mediaType = "application/json", examples = @ExampleObject(value = """
                    {"success":true,"message":"Request completed successfully","data":[{"id":"12345678-90ab-4cde-9f01-23456789abcd","inspectionId":"7b1e2f3a-4b5c-4d6e-8f90-1a2b3c4d5e6f","scanType":"BARCODE","scanValue":"8901058854488","scannedAt":"2026-01-15T10:30:00Z","matchedProductId":"3f2a9c10-6b1e-4a2e-9c1a-1e2f3a4b5c6d","matchedProductName":"Aashirvaad Atta 5kg"}],"timestamp":"2026-01-15T10:30:00Z"}
                    """))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Missing or invalid JWT")
    })
    @GetMapping("/inspections/{inspectionId}")
    public ResponseEntity<ApiResponse<List<ScanResponse>>> listByInspection(@PathVariable UUID inspectionId) {
        return ResponseUtil.ok(scannerService.listByInspection(inspectionId));
    }
}
