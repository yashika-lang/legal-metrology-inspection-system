package com.legalmetrology.inspection.controller;

import com.legalmetrology.common.response.ApiResponse;
import com.legalmetrology.common.response.PagedResponse;
import com.legalmetrology.inspection.dto.InspectionRequest;
import com.legalmetrology.inspection.dto.InspectionResponse;
import com.legalmetrology.inspection.dto.InspectionStatusHistoryResponse;
import com.legalmetrology.inspection.dto.InspectionStatusUpdateRequest;
import com.legalmetrology.inspection.service.InspectionService;
import com.legalmetrology.security.SecurityUtils;
import com.legalmetrology.utils.ResponseUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@Tag(name = "Inspections", description = "Inspection lifecycle — the aggregate root the whole AI pipeline attaches to")
@RestController
@RequestMapping("/api/v1/inspections")
@RequiredArgsConstructor
public class InspectionController {

    private final InspectionService inspectionService;
    private final SecurityUtils securityUtils;

    @Operation(summary = "Start a new inspection",
            description = "Creates the inspection in DRAFT status and records the initial status-history entry. " +
                    "productId/locationLat/locationLng/region are all optional — an inspection can be started before a " +
                    "product or GPS fix is available. The inspecting officer is taken from the authenticated caller, not the request body. " +
                    "Authorization: any authenticated user.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Inspection created",
                    content = @Content(mediaType = "application/json", examples = @ExampleObject(value = """
                            {
                              "success": true,
                              "message": "Resource created successfully",
                              "data": {
                                "id": "a1b2c3d4-5e6f-4a7b-8c9d-0e1f2a3b4c5d",
                                "inspectorId": "b3f1c2d4-5e6f-4a7b-8c9d-0e1f2a3b4c5d",
                                "inspectorName": "Priya Sharma",
                                "productId": "d4e5f6a7-8b9c-4d0e-9f1a-2b3c4d5e6f7a",
                                "productName": "Tata Salt 1kg",
                                "status": "DRAFT",
                                "locationLat": 28.6139,
                                "locationLng": 77.2090,
                                "region": "New Delhi",
                                "complianceScore": null,
                                "fraudRisk": null,
                                "startedAt": "2026-01-15T10:30:00Z",
                                "completedAt": null
                              },
                              "timestamp": "2026-01-15T10:30:00Z"
                            }
                            """))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400",
                    description = "Validation failed — locationLat outside [-90, 90], locationLng outside [-180, 180], or region exceeds 100 characters"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Missing or invalid access token"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "productId was supplied but no product exists with that id")
    })
    @PostMapping
    public ResponseEntity<ApiResponse<InspectionResponse>> create(@Valid @RequestBody InspectionRequest request) {
        return ResponseUtil.created(inspectionService.create(request, securityUtils.getCurrentUserId()));
    }

    @Operation(summary = "List inspections (paginated); pass mine=true to scope to the current inspector",
            description = "Authorization: any authenticated user — note that mine=false (the default) returns inspections " +
                    "belonging to every inspector, not just the caller's own. Pass evaluatedOnly=true to exclude " +
                    "inspections the AI Pipeline hasn't evaluated yet (compliance_score still null) — e.g. a draft " +
                    "created via 'New Inspection' and abandoned before any photo was uploaded. Reports and the " +
                    "Dashboard's Recent Inspections widget both pass this so incomplete drafts don't show up there.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Page of inspections",
                    content = @Content(mediaType = "application/json", examples = @ExampleObject(value = """
                            {
                              "success": true,
                              "message": "Request completed successfully",
                              "data": {
                                "items": [
                                  {
                                    "id": "a1b2c3d4-5e6f-4a7b-8c9d-0e1f2a3b4c5d",
                                    "inspectorId": "b3f1c2d4-5e6f-4a7b-8c9d-0e1f2a3b4c5d",
                                    "inspectorName": "Priya Sharma",
                                    "productId": "d4e5f6a7-8b9c-4d0e-9f1a-2b3c4d5e6f7a",
                                    "productName": "Tata Salt 1kg",
                                    "status": "IN_PROGRESS",
                                    "locationLat": 28.6139,
                                    "locationLng": 77.2090,
                                    "region": "New Delhi",
                                    "complianceScore": null,
                                    "fraudRisk": null,
                                    "startedAt": "2026-01-15T10:30:00Z",
                                    "completedAt": null
                                  }
                                ],
                                "page": 0,
                                "size": 20,
                                "totalItems": 1,
                                "totalPages": 1,
                                "last": true
                              },
                              "timestamp": "2026-01-15T10:30:00Z"
                            }
                            """))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Missing or invalid access token")
    })
    @GetMapping
    public ResponseEntity<ApiResponse<PagedResponse<InspectionResponse>>> list(
            @Parameter(description = "Scope the list to inspections started by the current caller") @RequestParam(defaultValue = "false") boolean mine,
            @Parameter(description = "Only include inspections the AI Pipeline has evaluated (compliance score present)")
            @RequestParam(defaultValue = "false") boolean evaluatedOnly,
            @PageableDefault(size = 20, sort = "startedAt") Pageable pageable) {
        UUID inspectorId = mine ? securityUtils.getCurrentUserId() : null;
        return ResponseUtil.ok(PagedResponse.of(inspectionService.list(inspectorId, evaluatedOnly, pageable)));
    }

    @Operation(summary = "Get a single inspection by id",
            description = "Authorization: any authenticated user.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "The requested inspection",
                    content = @Content(mediaType = "application/json", examples = @ExampleObject(value = """
                            {
                              "success": true,
                              "message": "Request completed successfully",
                              "data": {
                                "id": "a1b2c3d4-5e6f-4a7b-8c9d-0e1f2a3b4c5d",
                                "inspectorId": "b3f1c2d4-5e6f-4a7b-8c9d-0e1f2a3b4c5d",
                                "inspectorName": "Priya Sharma",
                                "productId": "d4e5f6a7-8b9c-4d0e-9f1a-2b3c4d5e6f7a",
                                "productName": "Tata Salt 1kg",
                                "status": "COMPLETED",
                                "locationLat": 28.6139,
                                "locationLng": 77.2090,
                                "region": "New Delhi",
                                "complianceScore": 82.50,
                                "fraudRisk": "LOW",
                                "startedAt": "2026-01-15T10:30:00Z",
                                "completedAt": "2026-01-15T11:15:00Z"
                              },
                              "timestamp": "2026-01-15T10:30:00Z"
                            }
                            """))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Missing or invalid access token"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "No inspection exists with the given id",
                    content = @Content(mediaType = "application/json", examples = @ExampleObject(value = """
                            {"success": false, "message": "Inspection not found with id: a1b2c3d4-5e6f-4a7b-8c9d-0e1f2a3b4c5d", "timestamp": "2026-01-15T10:30:00Z"}
                            """)))
    })
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<InspectionResponse>> getById(
            @Parameter(description = "Inspection id", example = "a1b2c3d4-5e6f-4a7b-8c9d-0e1f2a3b4c5d") @PathVariable UUID id) {
        return ResponseUtil.ok(inspectionService.getById(id));
    }

    @Operation(summary = "Advance the inspection to a new lifecycle status",
            description = "Only forward transitions on the fixed lifecycle are allowed: DRAFT -> IN_PROGRESS -> " +
                    "PENDING_REVIEW -> COMPLETED -> CLOSED (PENDING_REVIEW may also go back to IN_PROGRESS). Any other " +
                    "transition is rejected. Transitioning to COMPLETED sets completedAt and permanently finalizes/locks " +
                    "the inspection's evidence. Authorization: any authenticated user.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Inspection status updated successfully",
                    content = @Content(mediaType = "application/json", examples = @ExampleObject(value = """
                            {
                              "success": true,
                              "message": "Inspection status updated successfully",
                              "data": {
                                "id": "a1b2c3d4-5e6f-4a7b-8c9d-0e1f2a3b4c5d",
                                "inspectorId": "b3f1c2d4-5e6f-4a7b-8c9d-0e1f2a3b4c5d",
                                "inspectorName": "Priya Sharma",
                                "productId": "d4e5f6a7-8b9c-4d0e-9f1a-2b3c4d5e6f7a",
                                "productName": "Tata Salt 1kg",
                                "status": "PENDING_REVIEW",
                                "locationLat": 28.6139,
                                "locationLng": 77.2090,
                                "region": "New Delhi",
                                "complianceScore": null,
                                "fraudRisk": null,
                                "startedAt": "2026-01-15T10:30:00Z",
                                "completedAt": null
                              },
                              "timestamp": "2026-01-15T10:30:00Z"
                            }
                            """))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400",
                    description = "Validation failed (status missing, or note exceeds 500 characters), or the requested transition is not allowed from the inspection's current status",
                    content = @Content(mediaType = "application/json", examples = @ExampleObject(value = """
                            {"success": false, "message": "Cannot transition inspection from DRAFT to COMPLETED", "timestamp": "2026-01-15T10:30:00Z"}
                            """))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Missing or invalid access token"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "No inspection exists with the given id")
    })
    @PatchMapping("/{id}/status")
    public ResponseEntity<ApiResponse<InspectionResponse>> updateStatus(
            @Parameter(description = "Inspection id", example = "a1b2c3d4-5e6f-4a7b-8c9d-0e1f2a3b4c5d") @PathVariable UUID id,
            @Valid @RequestBody InspectionStatusUpdateRequest request) {
        return ResponseUtil.ok("Inspection status updated successfully",
                inspectionService.updateStatus(id, request, securityUtils.getCurrentUserId()));
    }

    @Operation(summary = "Get the full status change history for an inspection",
            description = "Returned oldest-first. Authorization: any authenticated user.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Chronological list of status changes",
                    content = @Content(mediaType = "application/json", examples = @ExampleObject(value = """
                            {
                              "success": true,
                              "message": "Request completed successfully",
                              "data": [
                                {
                                  "id": "e5f6a7b8-9c0d-4e1f-8a2b-3c4d5e6f7a8b",
                                  "fromStatus": null,
                                  "toStatus": "DRAFT",
                                  "changedByName": "Priya Sharma",
                                  "note": "Inspection created",
                                  "createdAt": "2026-01-15T10:30:00Z"
                                },
                                {
                                  "id": "f6a7b8c9-0d1e-4f2a-8b3c-4d5e6f7a8b9c",
                                  "fromStatus": "DRAFT",
                                  "toStatus": "IN_PROGRESS",
                                  "changedByName": "Priya Sharma",
                                  "note": "Started label capture",
                                  "createdAt": "2026-01-15T10:35:00Z"
                                }
                              ],
                              "timestamp": "2026-01-15T10:30:00Z"
                            }
                            """))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Missing or invalid access token"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "No inspection exists with the given id")
    })
    @GetMapping("/{id}/status-history")
    public ResponseEntity<ApiResponse<List<InspectionStatusHistoryResponse>>> statusHistory(
            @Parameter(description = "Inspection id", example = "a1b2c3d4-5e6f-4a7b-8c9d-0e1f2a3b4c5d") @PathVariable UUID id) {
        return ResponseUtil.ok(inspectionService.getStatusHistory(id));
    }
}
