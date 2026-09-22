package com.legalmetrology.evidence.controller;

import com.legalmetrology.common.response.ApiResponse;
import com.legalmetrology.evidence.dto.EvidenceResponse;
import com.legalmetrology.evidence.entity.Evidence;
import com.legalmetrology.evidence.mapper.EvidenceMapper;
import com.legalmetrology.evidence.service.EvidenceService;
import com.legalmetrology.storage.dto.StorageBucket;
import com.legalmetrology.storage.service.StorageService;
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

@Tag(name = "Evidence", description = "The Explainable Evidence Framework — one legally-traceable, hash-verifiable record per violation, with cropped and annotated image snippets")
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class EvidenceController {

    private final EvidenceService evidenceService;
    private final EvidenceMapper evidenceMapper;
    private final StorageService storageService;

    @Operation(summary = "Generate (or refresh) evidence for every violation on an inspection that doesn't already have immutable evidence",
            description = "For each violation, crops and annotates the source image region, hashes it, and persists one immutable-once-finalized evidence record. "
                    + "Evidence already marked immutable (the inspection has been finalized) is left untouched. Returns an empty list (not a 404) if the "
                    + "inspection id doesn't exist or has no violations. Any authenticated user may trigger this.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Evidence generated/refreshed (may be empty)", content = @Content(mediaType = "application/json", examples = @ExampleObject(value = """
                    {"success":true,"message":"Evidence generated","data":[{"id":"e1a2b3c4-1111-4a2e-9c1a-1e2f3a4b5c6d","sha256Hash":"a94a8fe5ccb19ba61c4c0873d391e987982fbbd3","inspectionId":"7b1e2f3a-4b5c-4d6e-8f90-1a2b3c4d5e6f","imageId":"c2d3e4f5-6789-4abc-9def-0123456789ab","violationId":"d3e4f5a6-789a-4bcd-9ef0-123456789abc","originalImageUrl":"https://xyzcompany.supabase.co/storage/v1/object/sign/images/original.jpg?token=...","annotatedImageUrl":"https://xyzcompany.supabase.co/storage/v1/object/sign/images/annotated.jpg?token=...","boundingBox":{"x":0.12,"y":0.34,"w":0.25,"h":0.08},"ocrText":"MRP Rs 45.00","normalizedValue":"45.00","visionConfidence":0.94,"ocrConfidence":0.89,"fusedConfidence":0.92,"expectedValue":"Present and legible","actualValue":"Missing","reason":"MRP declaration not found on any image face","suggestedFix":"Add MRP declaration to the front label","legalRuleReference":"LM Rule 6(1)(e)","severity":"CRITICAL","immutable":false,"createdAt":"2026-01-15T10:30:00Z"}],"timestamp":"2026-01-15T10:30:00Z"}
                    """))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Missing or invalid JWT"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "502", description = "Uploading/reading the cropped image from Supabase Storage failed")
    })
    @PostMapping("/inspections/{inspectionId}/evidence/generate")
    public ResponseEntity<ApiResponse<List<EvidenceResponse>>> generate(@PathVariable UUID inspectionId) {
        var evidence = evidenceService.generateForInspection(inspectionId).stream().map(this::toResponse).toList();
        return ResponseUtil.ok("Evidence generated", evidence);
    }

    @Operation(summary = "Get all evidence for an inspection")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Evidence list (may be empty if none has been generated yet)", content = @Content(mediaType = "application/json", examples = @ExampleObject(value = """
                    {"success":true,"message":"Request completed successfully","data":[{"id":"e1a2b3c4-1111-4a2e-9c1a-1e2f3a4b5c6d","sha256Hash":"a94a8fe5ccb19ba61c4c0873d391e987982fbbd3","inspectionId":"7b1e2f3a-4b5c-4d6e-8f90-1a2b3c4d5e6f","imageId":"c2d3e4f5-6789-4abc-9def-0123456789ab","violationId":"d3e4f5a6-789a-4bcd-9ef0-123456789abc","originalImageUrl":"https://xyzcompany.supabase.co/storage/v1/object/sign/images/original.jpg?token=...","annotatedImageUrl":"https://xyzcompany.supabase.co/storage/v1/object/sign/images/annotated.jpg?token=...","boundingBox":{"x":0.12,"y":0.34,"w":0.25,"h":0.08},"ocrText":"MRP Rs 45.00","normalizedValue":"45.00","visionConfidence":0.94,"ocrConfidence":0.89,"fusedConfidence":0.92,"expectedValue":"Present and legible","actualValue":"Missing","reason":"MRP declaration not found on any image face","suggestedFix":"Add MRP declaration to the front label","legalRuleReference":"LM Rule 6(1)(e)","severity":"CRITICAL","immutable":true,"createdAt":"2026-01-15T10:30:00Z"}],"timestamp":"2026-01-15T10:30:00Z"}
                    """))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Missing or invalid JWT"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "502", description = "Generating signed URLs for the stored images via Supabase Storage failed")
    })
    @GetMapping("/inspections/{inspectionId}/evidence")
    public ResponseEntity<ApiResponse<List<EvidenceResponse>>> getByInspection(@PathVariable UUID inspectionId) {
        var evidence = evidenceService.getByInspection(inspectionId).stream().map(this::toResponse).toList();
        return ResponseUtil.ok(evidence);
    }

    @Operation(summary = "Generate (or refresh) evidence for one specific violation",
            description = "Fails with 400 if evidence already exists for this violation and has been marked immutable (the inspection was finalized) — "
                    + "immutable evidence can never be regenerated, only read.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Evidence generated/refreshed", content = @Content(mediaType = "application/json", examples = @ExampleObject(value = """
                    {"success":true,"message":"Evidence generated","data":{"id":"e1a2b3c4-1111-4a2e-9c1a-1e2f3a4b5c6d","sha256Hash":"a94a8fe5ccb19ba61c4c0873d391e987982fbbd3","inspectionId":"7b1e2f3a-4b5c-4d6e-8f90-1a2b3c4d5e6f","imageId":"c2d3e4f5-6789-4abc-9def-0123456789ab","violationId":"d3e4f5a6-789a-4bcd-9ef0-123456789abc","originalImageUrl":"https://xyzcompany.supabase.co/storage/v1/object/sign/images/original.jpg?token=...","annotatedImageUrl":"https://xyzcompany.supabase.co/storage/v1/object/sign/images/annotated.jpg?token=...","boundingBox":{"x":0.12,"y":0.34,"w":0.25,"h":0.08},"ocrText":"MRP Rs 45.00","normalizedValue":"45.00","visionConfidence":0.94,"ocrConfidence":0.89,"fusedConfidence":0.92,"expectedValue":"Present and legible","actualValue":"Missing","reason":"MRP declaration not found on any image face","suggestedFix":"Add MRP declaration to the front label","legalRuleReference":"LM Rule 6(1)(e)","severity":"CRITICAL","immutable":false,"createdAt":"2026-01-15T10:30:00Z"},"timestamp":"2026-01-15T10:30:00Z"}
                    """))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Evidence for this violation already exists and is immutable — the inspection has been finalized", content = @Content(mediaType = "application/json", examples = @ExampleObject(value = """
                    {"success":false,"message":"Evidence for violation d3e4f5a6-789a-4bcd-9ef0-123456789abc is immutable — the inspection has been finalized","timestamp":"2026-01-15T10:30:00Z"}
                    """))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Missing or invalid JWT"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "No violation exists with the given id"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "502", description = "Uploading/reading the cropped image from Supabase Storage failed")
    })
    @PostMapping("/violations/{violationId}/evidence/generate")
    public ResponseEntity<ApiResponse<EvidenceResponse>> generateForViolation(@PathVariable UUID violationId) {
        return ResponseUtil.ok("Evidence generated", toResponse(evidenceService.generateForViolation(violationId)));
    }

    @Operation(summary = "Get one evidence record by id")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Evidence found", content = @Content(mediaType = "application/json", examples = @ExampleObject(value = """
                    {"success":true,"message":"Request completed successfully","data":{"id":"e1a2b3c4-1111-4a2e-9c1a-1e2f3a4b5c6d","sha256Hash":"a94a8fe5ccb19ba61c4c0873d391e987982fbbd3","inspectionId":"7b1e2f3a-4b5c-4d6e-8f90-1a2b3c4d5e6f","imageId":"c2d3e4f5-6789-4abc-9def-0123456789ab","violationId":"d3e4f5a6-789a-4bcd-9ef0-123456789abc","originalImageUrl":"https://xyzcompany.supabase.co/storage/v1/object/sign/images/original.jpg?token=...","annotatedImageUrl":"https://xyzcompany.supabase.co/storage/v1/object/sign/images/annotated.jpg?token=...","boundingBox":{"x":0.12,"y":0.34,"w":0.25,"h":0.08},"ocrText":"MRP Rs 45.00","normalizedValue":"45.00","visionConfidence":0.94,"ocrConfidence":0.89,"fusedConfidence":0.92,"expectedValue":"Present and legible","actualValue":"Missing","reason":"MRP declaration not found on any image face","suggestedFix":"Add MRP declaration to the front label","legalRuleReference":"LM Rule 6(1)(e)","severity":"CRITICAL","immutable":true,"createdAt":"2026-01-15T10:30:00Z"},"timestamp":"2026-01-15T10:30:00Z"}
                    """))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Missing or invalid JWT"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "No evidence record exists with the given id"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "502", description = "Generating signed URLs for the stored images via Supabase Storage failed")
    })
    @GetMapping("/evidence/{evidenceId}")
    public ResponseEntity<ApiResponse<EvidenceResponse>> getById(@PathVariable UUID evidenceId) {
        return ResponseUtil.ok(toResponse(evidenceService.getById(evidenceId)));
    }

    private EvidenceResponse toResponse(Evidence evidence) {
        String originalUrl = evidence.getOriginalImagePath() != null
                ? storageService.generateSignedUrl(StorageBucket.EVIDENCE, evidence.getOriginalImagePath()) : null;
        String annotatedUrl = evidence.getAnnotatedImagePath() != null
                ? storageService.generateSignedUrl(StorageBucket.EVIDENCE, evidence.getAnnotatedImagePath()) : null;
        return evidenceMapper.toResponse(evidence, originalUrl, annotatedUrl);
    }
}
