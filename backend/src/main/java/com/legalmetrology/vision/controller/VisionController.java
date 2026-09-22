package com.legalmetrology.vision.controller;

import com.legalmetrology.common.response.ApiResponse;
import com.legalmetrology.utils.ResponseUtil;
import com.legalmetrology.vision.dto.LabelDetectionResponse;
import com.legalmetrology.vision.mapper.LabelDetectionMapper;
import com.legalmetrology.vision.service.VisionAiService;
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

@Tag(name = "Vision AI", description = "Step 4/6 — multimodal declaration detection and font analysis for a single image")
@RestController
@RequestMapping("/api/v1/vision")
@RequiredArgsConstructor
public class VisionController {

    private final VisionAiService visionAiService;
    private final LabelDetectionMapper labelDetectionMapper;

    @Operation(summary = "Run Vision AI analysis on an already-uploaded image and persist the detections",
            description = "Downloads the image from Supabase Storage and sends it to the configured multimodal Vision AI provider for declaration "
                    + "detection and font analysis (Step 4/6). Re-running this replaces the image's previously persisted Vision AI detections. Any authenticated user may trigger this.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Vision AI analysis completed", content = @Content(mediaType = "application/json", examples = @ExampleObject(value = """
                    {"success":true,"message":"Vision AI analysis completed","data":[{"id":"a1b2c3d4-2222-4a2e-9c1a-1e2f3a4b5c6d","imageId":"c2d3e4f5-6789-4abc-9def-0123456789ab","declarationType":"MRP","detectedValue":"45.00","confidence":0.95,"source":"VISION_AI","boundingBox":{"x":0.12,"y":0.34,"w":0.25,"h":0.08},"present":true,"labelSection":"FRONT","fontSizeEstimate":3.2,"readabilityScore":0.88,"contrastScore":0.91,"fontIssue":"NONE"}],"timestamp":"2026-01-15T10:30:00Z"}
                    """))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Missing or invalid JWT"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "No image exists with the given id"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "502", description = "Downloading the image from Supabase Storage or calling the Vision AI provider failed")
    })
    @PostMapping("/images/{imageId}/analyze")
    public ResponseEntity<ApiResponse<List<LabelDetectionResponse>>> analyze(@PathVariable UUID imageId) {
        var detections = visionAiService.analyzeAndPersist(imageId).stream()
                .map(labelDetectionMapper::toResponse)
                .toList();
        return ResponseUtil.ok("Vision AI analysis completed", detections);
    }

    @Operation(summary = "Get the Vision AI detections already persisted for an image",
            description = "Returns an empty list (not a 404) if `/analyze` hasn't been called yet, or if the image id doesn't exist — this endpoint does not validate the image.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Vision AI detections (may be empty)", content = @Content(mediaType = "application/json", examples = @ExampleObject(value = """
                    {"success":true,"message":"Request completed successfully","data":[{"id":"a1b2c3d4-2222-4a2e-9c1a-1e2f3a4b5c6d","imageId":"c2d3e4f5-6789-4abc-9def-0123456789ab","declarationType":"MRP","detectedValue":"45.00","confidence":0.95,"source":"VISION_AI","boundingBox":{"x":0.12,"y":0.34,"w":0.25,"h":0.08},"present":true,"labelSection":"FRONT","fontSizeEstimate":3.2,"readabilityScore":0.88,"contrastScore":0.91,"fontIssue":"NONE"}],"timestamp":"2026-01-15T10:30:00Z"}
                    """))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Missing or invalid JWT")
    })
    @GetMapping("/images/{imageId}/detections")
    public ResponseEntity<ApiResponse<List<LabelDetectionResponse>>> getDetections(@PathVariable UUID imageId) {
        var detections = visionAiService.getDetections(imageId).stream()
                .map(labelDetectionMapper::toResponse)
                .toList();
        return ResponseUtil.ok(detections);
    }
}
