package com.legalmetrology.history.controller;

import com.legalmetrology.common.response.ApiResponse;
import com.legalmetrology.common.response.PagedResponse;
import com.legalmetrology.history.dto.AuditLogResponse;
import com.legalmetrology.history.service.AuditLogService;
import com.legalmetrology.utils.ResponseUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@Tag(name = "History", description = "System-wide audit trail (who did what, when)")
@RestController
@RequestMapping("/api/v1/history")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'SENIOR_OFFICER')")
public class AuditLogController {

    private final AuditLogService auditLogService;

    @Operation(summary = "List audit log entries (paginated), optionally filtered by entity or user",
            description = "Filtering precedence when multiple query params are supplied: entityType+entityId together " +
                    "take priority, then userId, otherwise all entries are returned. entityType and entityId must both " +
                    "be supplied together to filter by entity. Authorization: requires ADMIN or SENIOR_OFFICER (class-level).")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Page of audit log entries",
                    content = @Content(mediaType = "application/json", examples = @ExampleObject(value = """
                            {
                              "success": true,
                              "message": "Request completed successfully",
                              "data": {
                                "items": [
                                  {
                                    "id": "c1d2e3f4-5a6b-4c7d-8e9f-0a1b2c3d4e5f",
                                    "userId": "b3f1c2d4-5e6f-4a7b-8c9d-0e1f2a3b4c5d",
                                    "userName": "Priya Sharma",
                                    "action": "INSPECTION_STATUS_UPDATED",
                                    "entityType": "Inspection",
                                    "entityId": "a1b2c3d4-5e6f-4a7b-8c9d-0e1f2a3b4c5d",
                                    "metadata": "{\\"fromStatus\\":\\"IN_PROGRESS\\",\\"toStatus\\":\\"PENDING_REVIEW\\"}",
                                    "ipAddress": "10.20.30.40",
                                    "createdAt": "2026-01-15T10:30:00Z"
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
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid query parameter (e.g. entityId/userId not a valid UUID)"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Missing or invalid access token"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Caller is not ADMIN or SENIOR_OFFICER")
    })
    @GetMapping("/audit-logs")
    public ResponseEntity<ApiResponse<PagedResponse<AuditLogResponse>>> list(
            @Parameter(description = "Filter by entity type, e.g. \"Inspection\" or \"User\" — must be supplied together with entityId", example = "Inspection")
            @RequestParam(required = false) String entityType,
            @Parameter(description = "Filter by entity id — must be supplied together with entityType", example = "a1b2c3d4-5e6f-4a7b-8c9d-0e1f2a3b4c5d")
            @RequestParam(required = false) UUID entityId,
            @Parameter(description = "Filter by the user who performed the action", example = "b3f1c2d4-5e6f-4a7b-8c9d-0e1f2a3b4c5d")
            @RequestParam(required = false) UUID userId,
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable) {

        var page = entityType != null && entityId != null
                ? auditLogService.listByEntity(entityType, entityId, pageable)
                : userId != null
                ? auditLogService.listByUser(userId, pageable)
                : auditLogService.listAll(pageable);

        return ResponseUtil.ok(PagedResponse.of(page));
    }
}
