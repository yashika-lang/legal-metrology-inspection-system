package com.legalmetrology.inspection.controller;

import com.legalmetrology.common.enums.ImageType;
import com.legalmetrology.common.response.ApiResponse;
import com.legalmetrology.inspection.dto.ImageResponse;
import com.legalmetrology.inspection.service.ImageService;
import com.legalmetrology.utils.ResponseUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@Tag(name = "Images", description = "Label photo capture/upload for an inspection, backed by Supabase Storage")
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class ImageController {

    private final ImageService imageService;

    @Operation(summary = "Upload a label photo for an inspection",
            description = "Accepts JPEG/PNG/WEBP/HEIC up to 15MB. imageType defaults to OTHER if omitted. The image is " +
                    "quality-checked immediately after storage (Step 1 of the AI pipeline) — the response's qualityScore, " +
                    "qualityWarnings, and recommendedAction reflect that check. Authorization: any authenticated user.",
            requestBody = @RequestBody(content = @Content(mediaType = MediaType.MULTIPART_FORM_DATA_VALUE)))
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Image stored and quality-analyzed",
                    content = @Content(mediaType = "application/json", examples = @ExampleObject(value = """
                            {
                              "success": true,
                              "message": "Resource created successfully",
                              "data": {
                                "id": "f1e2d3c4-5b6a-4978-8f6e-5d4c3b2a1f0e",
                                "inspectionId": "a1b2c3d4-5e6f-4a7b-8c9d-0e1f2a3b4c5d",
                                "storagePath": "a1b2c3d4-5e6f-4a7b-8c9d-0e1f2a3b4c5d/front/9f8e7d6c-1a2b-4c3d-9e0f-1a2b3c4d5e6f.jpg",
                                "signedUrl": "https://xyzcompany.supabase.co/storage/v1/object/sign/images/...token=abc123",
                                "imageType": "FRONT",
                                "qualityScore": 87.50,
                                "qualityWarnings": [],
                                "recommendedAction": "NONE",
                                "uploadedAt": "2026-01-15T10:30:00Z"
                              },
                              "timestamp": "2026-01-15T10:30:00Z"
                            }
                            """))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400",
                    description = "No file was uploaded, the file exceeds 15MB, or its content type is not one of JPEG/PNG/WEBP/HEIC",
                    content = @Content(mediaType = "application/json", examples = @ExampleObject(value = """
                            {"success": false, "message": "Unsupported image type. Allowed types: JPEG, PNG, WEBP, HEIC", "timestamp": "2026-01-15T10:30:00Z"}
                            """))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Missing or invalid access token"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "No inspection exists with the given id"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "502", description = "The underlying Supabase Storage upload failed")
    })
    @PostMapping(value = "/inspections/{inspectionId}/images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<ImageResponse>> upload(
            @Parameter(description = "Inspection id", example = "a1b2c3d4-5e6f-4a7b-8c9d-0e1f2a3b4c5d") @PathVariable UUID inspectionId,
            @Parameter(description = "The image file (JPEG/PNG/WEBP/HEIC, max 15MB)") @RequestParam("file") MultipartFile file,
            @Parameter(description = "Which side/angle of the label this photo captures; defaults to OTHER if omitted", example = "FRONT")
            @RequestParam(required = false) ImageType imageType) {
        return ResponseUtil.created(imageService.upload(inspectionId, file, imageType));
    }

    @Operation(summary = "List all images captured for an inspection",
            description = "Each image's signedUrl is freshly generated on every call. Authorization: any authenticated user.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Images for the inspection",
                    content = @Content(mediaType = "application/json", examples = @ExampleObject(value = """
                            {
                              "success": true,
                              "message": "Request completed successfully",
                              "data": [
                                {
                                  "id": "f1e2d3c4-5b6a-4978-8f6e-5d4c3b2a1f0e",
                                  "inspectionId": "a1b2c3d4-5e6f-4a7b-8c9d-0e1f2a3b4c5d",
                                  "storagePath": "a1b2c3d4-5e6f-4a7b-8c9d-0e1f2a3b4c5d/front/9f8e7d6c-1a2b-4c3d-9e0f-1a2b3c4d5e6f.jpg",
                                  "signedUrl": "https://xyzcompany.supabase.co/storage/v1/object/sign/images/...token=abc123",
                                  "imageType": "FRONT",
                                  "qualityScore": 87.50,
                                  "qualityWarnings": [],
                                  "recommendedAction": "NONE",
                                  "uploadedAt": "2026-01-15T10:30:00Z"
                                }
                              ],
                              "timestamp": "2026-01-15T10:30:00Z"
                            }
                            """))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Missing or invalid access token")
    })
    @GetMapping("/inspections/{inspectionId}/images")
    public ResponseEntity<ApiResponse<List<ImageResponse>>> listByInspection(
            @Parameter(description = "Inspection id", example = "a1b2c3d4-5e6f-4a7b-8c9d-0e1f2a3b4c5d") @PathVariable UUID inspectionId) {
        return ResponseUtil.ok(imageService.listByInspection(inspectionId));
    }

    @Operation(summary = "Get a single image (with a fresh signed URL)",
            description = "Authorization: any authenticated user.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "The requested image",
                    content = @Content(mediaType = "application/json", examples = @ExampleObject(value = """
                            {
                              "success": true,
                              "message": "Request completed successfully",
                              "data": {
                                "id": "f1e2d3c4-5b6a-4978-8f6e-5d4c3b2a1f0e",
                                "inspectionId": "a1b2c3d4-5e6f-4a7b-8c9d-0e1f2a3b4c5d",
                                "storagePath": "a1b2c3d4-5e6f-4a7b-8c9d-0e1f2a3b4c5d/front/9f8e7d6c-1a2b-4c3d-9e0f-1a2b3c4d5e6f.jpg",
                                "signedUrl": "https://xyzcompany.supabase.co/storage/v1/object/sign/images/...token=abc123",
                                "imageType": "FRONT",
                                "qualityScore": 87.50,
                                "qualityWarnings": [],
                                "recommendedAction": "NONE",
                                "uploadedAt": "2026-01-15T10:30:00Z"
                              },
                              "timestamp": "2026-01-15T10:30:00Z"
                            }
                            """))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Missing or invalid access token"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "No image exists with the given id",
                    content = @Content(mediaType = "application/json", examples = @ExampleObject(value = """
                            {"success": false, "message": "Image not found with id: f1e2d3c4-5b6a-4978-8f6e-5d4c3b2a1f0e", "timestamp": "2026-01-15T10:30:00Z"}
                            """)))
    })
    @GetMapping("/images/{id}")
    public ResponseEntity<ApiResponse<ImageResponse>> getById(
            @Parameter(description = "Image id", example = "f1e2d3c4-5b6a-4978-8f6e-5d4c3b2a1f0e") @PathVariable UUID id) {
        return ResponseUtil.ok(imageService.getById(id));
    }

    @Operation(summary = "Delete an image",
            description = "Removes the file from Supabase Storage and its database record. " +
                    "Authorization: the inspecting officer who owns the parent inspection, or ADMIN/SENIOR_OFFICER.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Image deleted successfully",
                    content = @Content(mediaType = "application/json", examples = @ExampleObject(value = """
                            {"success": true, "message": "Image deleted successfully", "timestamp": "2026-01-15T10:30:00Z"}
                            """))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Missing or invalid access token"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403",
                    description = "Caller is neither the owning inspector nor ADMIN/SENIOR_OFFICER",
                    content = @Content(mediaType = "application/json", examples = @ExampleObject(value = """
                            {"success": false, "message": "Only the inspecting officer or an admin/senior officer may delete this image", "timestamp": "2026-01-15T10:30:00Z"}
                            """))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "No image exists with the given id")
    })
    @DeleteMapping("/images/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(
            @Parameter(description = "Image id", example = "f1e2d3c4-5b6a-4978-8f6e-5d4c3b2a1f0e") @PathVariable UUID id) {
        imageService.delete(id);
        return ResponseUtil.noContent("Image deleted successfully");
    }
}
