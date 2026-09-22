package com.legalmetrology.auth.controller;

import com.legalmetrology.auth.dto.AssignRolesRequest;
import com.legalmetrology.auth.dto.UpdateProfileRequest;
import com.legalmetrology.auth.dto.UpdateUserStatusRequest;
import com.legalmetrology.auth.dto.UserResponse;
import com.legalmetrology.auth.service.UserService;
import com.legalmetrology.common.response.ApiResponse;
import com.legalmetrology.common.response.PagedResponse;
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
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@Tag(name = "Users", description = "User directory and administration (role assignment, activation)")
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @Operation(summary = "List users (paginated)",
            description = "Authorization: requires ADMIN or SENIOR_OFFICER.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Page of users",
                    content = @Content(mediaType = "application/json", examples = @ExampleObject(value = """
                            {
                              "success": true,
                              "message": "Request completed successfully",
                              "data": {
                                "items": [
                                  {
                                    "id": "b3f1c2d4-5e6f-4a7b-8c9d-0e1f2a3b4c5d",
                                    "fullName": "Priya Sharma",
                                    "email": "priya.sharma@legalmetrology.gov.in",
                                    "employeeCode": "LM-2024-0142",
                                    "phone": "+91-9876543210",
                                    "preferredLocale": "en-IN",
                                    "active": true,
                                    "roles": ["INSPECTOR"],
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
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Missing or invalid access token"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Caller is not ADMIN or SENIOR_OFFICER")
    })
    @PreAuthorize("hasAnyRole('ADMIN', 'SENIOR_OFFICER')")
    @GetMapping
    public ResponseEntity<ApiResponse<PagedResponse<UserResponse>>> list(
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable) {
        return ResponseUtil.ok(PagedResponse.of(userService.list(pageable)));
    }

    @Operation(summary = "Get a single user by id",
            description = "Authorization: requires ADMIN or SENIOR_OFFICER.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "The requested user",
                    content = @Content(mediaType = "application/json", examples = @ExampleObject(value = """
                            {
                              "success": true,
                              "message": "Request completed successfully",
                              "data": {
                                "id": "b3f1c2d4-5e6f-4a7b-8c9d-0e1f2a3b4c5d",
                                "fullName": "Priya Sharma",
                                "email": "priya.sharma@legalmetrology.gov.in",
                                "employeeCode": "LM-2024-0142",
                                "phone": "+91-9876543210",
                                "preferredLocale": "en-IN",
                                "active": true,
                                "roles": ["INSPECTOR"],
                                "createdAt": "2026-01-15T10:30:00Z"
                              },
                              "timestamp": "2026-01-15T10:30:00Z"
                            }
                            """))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Missing or invalid access token"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Caller is not ADMIN or SENIOR_OFFICER"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "No user exists with the given id",
                    content = @Content(mediaType = "application/json", examples = @ExampleObject(value = """
                            {"success": false, "message": "User not found with id: b3f1c2d4-5e6f-4a7b-8c9d-0e1f2a3b4c5d", "timestamp": "2026-01-15T10:30:00Z"}
                            """)))
    })
    @PreAuthorize("hasAnyRole('ADMIN', 'SENIOR_OFFICER')")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<UserResponse>> getById(
            @Parameter(description = "User id", example = "b3f1c2d4-5e6f-4a7b-8c9d-0e1f2a3b4c5d") @PathVariable UUID id) {
        return ResponseUtil.ok(userService.getById(id));
    }

    @Operation(summary = "Update a user's profile fields (self, or admin/senior officer on behalf of anyone)",
            description = "All fields are optional; only non-blank fields supplied are applied (a blank/omitted field " +
                    "leaves the existing value unchanged). Authorization: the user themselves, or ADMIN/SENIOR_OFFICER on behalf of anyone.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Profile updated successfully",
                    content = @Content(mediaType = "application/json", examples = @ExampleObject(value = """
                            {
                              "success": true,
                              "message": "Profile updated successfully",
                              "data": {
                                "id": "b3f1c2d4-5e6f-4a7b-8c9d-0e1f2a3b4c5d",
                                "fullName": "Priya Sharma Rao",
                                "email": "priya.sharma@legalmetrology.gov.in",
                                "employeeCode": "LM-2024-0142",
                                "phone": "+91-9876543211",
                                "preferredLocale": "hi-IN",
                                "active": true,
                                "roles": ["INSPECTOR"],
                                "createdAt": "2026-01-15T10:30:00Z"
                              },
                              "timestamp": "2026-01-15T10:30:00Z"
                            }
                            """))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400",
                    description = "Validation failed — fullName/phone/preferredLocale exceed their maximum length"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Missing or invalid access token"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Caller is neither the target user nor ADMIN/SENIOR_OFFICER"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "No user exists with the given id")
    })
    @PreAuthorize("#id == authentication.principal.id or hasAnyRole('ADMIN', 'SENIOR_OFFICER')")
    @PatchMapping("/{id}")
    public ResponseEntity<ApiResponse<UserResponse>> updateProfile(
            @Parameter(description = "User id", example = "b3f1c2d4-5e6f-4a7b-8c9d-0e1f2a3b4c5d") @PathVariable UUID id,
            @Valid @RequestBody UpdateProfileRequest request) {
        return ResponseUtil.ok("Profile updated successfully", userService.updateProfile(id, request));
    }

    @Operation(summary = "Replace a user's roles (admin only)",
            description = "Fully replaces the user's role set with the roles supplied — this is not additive. " +
                    "Authorization: requires ADMIN.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Roles updated successfully",
                    content = @Content(mediaType = "application/json", examples = @ExampleObject(value = """
                            {
                              "success": true,
                              "message": "Roles updated successfully",
                              "data": {
                                "id": "b3f1c2d4-5e6f-4a7b-8c9d-0e1f2a3b4c5d",
                                "fullName": "Priya Sharma",
                                "email": "priya.sharma@legalmetrology.gov.in",
                                "employeeCode": "LM-2024-0142",
                                "phone": "+91-9876543210",
                                "preferredLocale": "en-IN",
                                "active": true,
                                "roles": ["SENIOR_OFFICER"],
                                "createdAt": "2026-01-15T10:30:00Z"
                              },
                              "timestamp": "2026-01-15T10:30:00Z"
                            }
                            """))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Validation failed — roles is empty/missing"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Missing or invalid access token"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Caller is not ADMIN"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "No user exists with the given id, or one of the supplied roles does not exist")
    })
    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/{id}/roles")
    public ResponseEntity<ApiResponse<UserResponse>> assignRoles(
            @Parameter(description = "User id", example = "b3f1c2d4-5e6f-4a7b-8c9d-0e1f2a3b4c5d") @PathVariable UUID id,
            @Valid @RequestBody AssignRolesRequest request) {
        return ResponseUtil.ok("Roles updated successfully", userService.assignRoles(id, request));
    }

    @Operation(summary = "Activate or deactivate a user account (admin only)",
            description = "Authorization: requires ADMIN.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "User status updated successfully",
                    content = @Content(mediaType = "application/json", examples = @ExampleObject(value = """
                            {
                              "success": true,
                              "message": "User status updated successfully",
                              "data": {
                                "id": "b3f1c2d4-5e6f-4a7b-8c9d-0e1f2a3b4c5d",
                                "fullName": "Priya Sharma",
                                "email": "priya.sharma@legalmetrology.gov.in",
                                "employeeCode": "LM-2024-0142",
                                "phone": "+91-9876543210",
                                "preferredLocale": "en-IN",
                                "active": false,
                                "roles": ["INSPECTOR"],
                                "createdAt": "2026-01-15T10:30:00Z"
                              },
                              "timestamp": "2026-01-15T10:30:00Z"
                            }
                            """))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Validation failed — active flag is missing"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Missing or invalid access token"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Caller is not ADMIN"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "No user exists with the given id")
    })
    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/{id}/status")
    public ResponseEntity<ApiResponse<UserResponse>> updateStatus(
            @Parameter(description = "User id", example = "b3f1c2d4-5e6f-4a7b-8c9d-0e1f2a3b4c5d") @PathVariable UUID id,
            @Valid @RequestBody UpdateUserStatusRequest request) {
        return ResponseUtil.ok("User status updated successfully", userService.updateStatus(id, request));
    }
}
