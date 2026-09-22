package com.legalmetrology.ocr.controller;

import com.legalmetrology.common.response.ApiResponse;
import com.legalmetrology.ocr.dto.FusedDeclarationResponse;
import com.legalmetrology.ocr.dto.OcrHistoryEntryResponse;
import com.legalmetrology.ocr.dto.OcrRunResponse;
import com.legalmetrology.ocr.fusion.DeclarationFusionService;
import com.legalmetrology.ocr.mapper.FusedDeclarationMapper;
import com.legalmetrology.ocr.mapper.OcrHistoryMapper;
import com.legalmetrology.ocr.service.OcrService;
import com.legalmetrology.utils.ResponseUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * Steps 2-3: OCR extraction, correction, and declaration classification for
 * a single image, plus the cross-image/cross-source fusion (Step: "merge
 * OCR results intelligently" / "confidence fusion") for a whole inspection.
 * Every call here is a thin delegation to {@code ocr.service.OcrService} /
 * {@code ocr.fusion.DeclarationFusionService} — no OCR logic lives in this
 * controller itself.
 */
@Tag(name = "OCR", description = "Steps 2-3 — provider-agnostic OCR, AI-assisted correction, and multi-image declaration fusion")
@RestController
@RequestMapping("/api/v1/ocr")
@RequiredArgsConstructor
public class OcrController {

    private final OcrService ocrService;
    private final DeclarationFusionService declarationFusionService;
    private final OcrHistoryMapper ocrHistoryMapper;
    private final FusedDeclarationMapper fusedDeclarationMapper;

    @Operation(summary = "Run OCR (with automatic provider fallback), correction, and field classification on an image",
            description = "Calls the configured OCR provider (falling back to the next configured provider if the first fails), "
                    + "runs AI-assisted text correction, and classifies fields into declaration types. Every run is appended to the image's OCR history — nothing is overwritten.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "OCR run completed", content = @Content(mediaType = "application/json", examples = @ExampleObject(value = """
                    {"success":true,"message":"OCR run completed","data":{"id":"f1e2d3c4-5678-4abc-9def-0123456789ab","imageId":"c2d3e4f5-6789-4abc-9def-0123456789ab","provider":"GOOGLE_VISION","detectedLanguage":"en","rawText":"MRP Rs.45.00 Net Qty 1kg","correctedText":"MRP Rs. 45.00 Net Qty 1kg","ocrConfidence":0.91,"correctionConfidence":0.96,"correctionChangesSummary":"Added missing space after 'Rs.'","fields":[{"id":"a1b2c3d4-1111-4a2e-9c1a-1e2f3a4b5c6d","imageId":"c2d3e4f5-6789-4abc-9def-0123456789ab","declarationType":"MRP","detectedValue":"45.00","confidence":0.91,"source":"OCR_TEXT","boundingBox":null,"present":true,"labelSection":"FRONT","fontSizeEstimate":null,"readabilityScore":null,"contrastScore":null,"fontIssue":"NONE"}],"runAt":"2026-01-15T10:30:00Z"},"timestamp":"2026-01-15T10:30:00Z"}
                    """))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Missing or invalid JWT"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "No image exists with the given id"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "502", description = "Every configured OCR provider failed, or downloading the image from Supabase Storage failed")
    })
    @PostMapping("/images/{imageId}/run")
    public ResponseEntity<ApiResponse<OcrRunResponse>> run(@PathVariable UUID imageId) {
        return ResponseUtil.ok("OCR run completed", ocrService.run(imageId));
    }

    @Operation(summary = "Get the full OCR run history for an image, most recent first",
            description = "Returns an empty list (not a 404) if OCR hasn't run yet, or if the image id doesn't exist — this endpoint does not validate the image.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "OCR history (may be empty)", content = @Content(mediaType = "application/json", examples = @ExampleObject(value = """
                    {"success":true,"message":"Request completed successfully","data":[{"id":"f1e2d3c4-5678-4abc-9def-0123456789ab","provider":"GOOGLE_VISION","detectedLanguage":"en","rawText":"MRP Rs.45.00 Net Qty 1kg","correctedText":"MRP Rs. 45.00 Net Qty 1kg","confidence":0.91,"correctionConfidence":0.96,"runAt":"2026-01-15T10:30:00Z"}],"timestamp":"2026-01-15T10:30:00Z"}
                    """))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Missing or invalid JWT")
    })
    @GetMapping("/images/{imageId}/history")
    public ResponseEntity<ApiResponse<List<OcrHistoryEntryResponse>>> history(@PathVariable UUID imageId) {
        var history = ocrService.getHistory(imageId).stream().map(ocrHistoryMapper::toResponse).toList();
        return ResponseUtil.ok(history);
    }

    @Operation(summary = "Recompute the fused declarations for an inspection (merges every image's OCR + Vision AI detections)",
            description = "Replaces any previously computed fused declarations for this inspection with a fresh cross-image, cross-source (OCR + Vision AI) merge. "
                    + "Requires that OCR and/or Vision AI has already run on at least one image of the inspection.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Declarations fused", content = @Content(mediaType = "application/json", examples = @ExampleObject(value = """
                    {"success":true,"message":"Declarations fused successfully","data":[{"id":"9a8b7c6d-5e4f-4321-8765-abcdef012345","inspectionId":"7b1e2f3a-4b5c-4d6e-8f90-1a2b3c4d5e6f","declarationType":"MRP","fusedValue":"45.00","fusedConfidence":0.93,"present":true,"ocrValue":"45.00","ocrConfidence":0.91,"visionValue":"45.00","visionConfidence":0.95,"agreement":true}],"timestamp":"2026-01-15T10:30:00Z"}
                    """))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Missing or invalid JWT"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "No inspection exists with the given id")
    })
    @PostMapping("/inspections/{inspectionId}/fuse")
    public ResponseEntity<ApiResponse<List<FusedDeclarationResponse>>> fuse(@PathVariable UUID inspectionId) {
        var fused = declarationFusionService.fuseInspection(inspectionId).stream()
                .map(fusedDeclarationMapper::toResponse)
                .toList();
        return ResponseUtil.ok("Declarations fused successfully", fused);
    }

    @Operation(summary = "Get the fused declarations already computed for an inspection",
            description = "Returns an empty list (not a 404) if `/fuse` hasn't been called yet, or if the inspection id doesn't exist — this endpoint does not validate the inspection.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Fused declarations (may be empty)", content = @Content(mediaType = "application/json", examples = @ExampleObject(value = """
                    {"success":true,"message":"Request completed successfully","data":[{"id":"9a8b7c6d-5e4f-4321-8765-abcdef012345","inspectionId":"7b1e2f3a-4b5c-4d6e-8f90-1a2b3c4d5e6f","declarationType":"MRP","fusedValue":"45.00","fusedConfidence":0.93,"present":true,"ocrValue":"45.00","ocrConfidence":0.91,"visionValue":"45.00","visionConfidence":0.95,"agreement":true}],"timestamp":"2026-01-15T10:30:00Z"}
                    """))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Missing or invalid JWT")
    })
    @GetMapping("/inspections/{inspectionId}/fused")
    public ResponseEntity<ApiResponse<List<FusedDeclarationResponse>>> getFused(@PathVariable UUID inspectionId) {
        var fused = declarationFusionService.getFusedDeclarations(inspectionId).stream()
                .map(fusedDeclarationMapper::toResponse)
                .toList();
        return ResponseUtil.ok(fused);
    }
}
