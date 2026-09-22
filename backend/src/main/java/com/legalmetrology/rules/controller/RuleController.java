package com.legalmetrology.rules.controller;

import com.legalmetrology.common.response.ApiResponse;
import com.legalmetrology.common.response.PagedResponse;
import com.legalmetrology.rules.dto.RuleRequest;
import com.legalmetrology.rules.dto.RuleResponse;
import com.legalmetrology.rules.mapper.RuleMapper;
import com.legalmetrology.rules.service.RuleService;
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
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * Step 5's rule master-data API. Every write here goes through
 * {@code rules.service.RuleService}'s versioning policy — see that
 * interface's Javadoc — and refreshes the Rule Loader's cache so changes
 * take effect immediately, without an application restart.
 */
@Tag(name = "Rules", description = "Legal Metrology rule master data — fully database-driven, versioned")
@RestController
@RequestMapping("/api/v1/rules")
@RequiredArgsConstructor
public class RuleController {

    private final RuleService ruleService;
    private final RuleMapper ruleMapper;

    @Operation(summary = "List rules (paginated), optionally filtered to only currently-active versions")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Page of rules", content = @Content(mediaType = "application/json", examples = @ExampleObject(value = """
                    {"success":true,"message":"Request completed successfully","data":{"content":[{"id":"11223344-5566-4a2e-9c1a-1e2f3a4b5c6d","ruleCode":"LM-6.1.E","title":"MRP must be declared","description":"Every pre-packaged commodity must declare its Maximum Retail Price","legalReference":"Legal Metrology (Packaged Commodities) Rules, 2011","category":"Mandatory Declaration","severity":"CRITICAL","validationType":"FIELD_EXISTS","validationExpression":"{\\"field\\":\\"MRP\\"}","mandatory":true,"suggestion":"Add MRP declaration to the front label","penaltyReference":"Section 36","version":1,"effectiveDate":"2026-01-01","active":true}],"page":0,"size":20,"totalElements":1,"totalPages":1,"last":true},"timestamp":"2026-01-15T10:30:00Z"}
                    """))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Missing or invalid JWT")
    })
    @GetMapping
    public ResponseEntity<ApiResponse<PagedResponse<RuleResponse>>> list(
            @Parameter(description = "When true, only currently-active rule versions are returned", example = "true")
            @RequestParam(required = false) Boolean activeOnly,
            @PageableDefault(size = 20, sort = "ruleCode") Pageable pageable) {
        var page = ruleService.list(activeOnly, pageable).map(ruleMapper::toResponse);
        return ResponseUtil.ok(PagedResponse.of(page));
    }

    @Operation(summary = "Get a single rule (any version) by id")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Rule found", content = @Content(mediaType = "application/json", examples = @ExampleObject(value = """
                    {"success":true,"message":"Request completed successfully","data":{"id":"11223344-5566-4a2e-9c1a-1e2f3a4b5c6d","ruleCode":"LM-6.1.E","title":"MRP must be declared","description":"Every pre-packaged commodity must declare its Maximum Retail Price","legalReference":"Legal Metrology (Packaged Commodities) Rules, 2011","category":"Mandatory Declaration","severity":"CRITICAL","validationType":"FIELD_EXISTS","validationExpression":"{\\"field\\":\\"MRP\\"}","mandatory":true,"suggestion":"Add MRP declaration to the front label","penaltyReference":"Section 36","version":1,"effectiveDate":"2026-01-01","active":true},"timestamp":"2026-01-15T10:30:00Z"}
                    """))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Missing or invalid JWT"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "No rule exists with the given id")
    })
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<RuleResponse>> getById(@PathVariable UUID id) {
        return ResponseUtil.ok(ruleMapper.toResponse(ruleService.getById(id)));
    }

    @Operation(summary = "Get the full version history for a rule code, newest first",
            description = "Returns an empty list (not a 404) if no rule with this code has ever existed — this endpoint does not validate the rule code.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Version history (may be empty)", content = @Content(mediaType = "application/json", examples = @ExampleObject(value = """
                    {"success":true,"message":"Request completed successfully","data":[{"id":"11223344-5566-4a2e-9c1a-1e2f3a4b5c6d","ruleCode":"LM-6.1.E","title":"MRP must be declared","description":"Every pre-packaged commodity must declare its Maximum Retail Price","legalReference":"Legal Metrology (Packaged Commodities) Rules, 2011","category":"Mandatory Declaration","severity":"CRITICAL","validationType":"FIELD_EXISTS","validationExpression":"{\\"field\\":\\"MRP\\"}","mandatory":true,"suggestion":"Add MRP declaration to the front label","penaltyReference":"Section 36","version":2,"effectiveDate":"2026-01-15","active":true}],"timestamp":"2026-01-15T10:30:00Z"}
                    """))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Missing or invalid JWT")
    })
    @GetMapping("/by-code/{ruleCode}/history")
    public ResponseEntity<ApiResponse<List<RuleResponse>>> versionHistory(
            @Parameter(description = "The rule's stable code, shared across all its versions", example = "LM-6.1.E")
            @PathVariable String ruleCode) {
        var history = ruleService.getVersionHistory(ruleCode).stream().map(ruleMapper::toResponse).toList();
        return ResponseUtil.ok(history);
    }

    @Operation(summary = "Create a new rule (version 1). Admin only.",
            description = "Fails with 409 if an active rule already exists for `ruleCode` — use PUT `/{id}` to create a new version of an existing rule instead. "
                    + "`validationType` selects which generic validator the rule engine dispatches to; `validationExpression` is that validator's JSON parameter blob.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Rule created", content = @Content(mediaType = "application/json", examples = @ExampleObject(value = """
                    {"success":true,"message":"Resource created successfully","data":{"id":"11223344-5566-4a2e-9c1a-1e2f3a4b5c6d","ruleCode":"LM-6.1.E","title":"MRP must be declared","description":"Every pre-packaged commodity must declare its Maximum Retail Price","legalReference":"Legal Metrology (Packaged Commodities) Rules, 2011","category":"Mandatory Declaration","severity":"CRITICAL","validationType":"FIELD_EXISTS","validationExpression":"{\\"field\\":\\"MRP\\"}","mandatory":true,"suggestion":"Add MRP declaration to the front label","penaltyReference":"Section 36","version":1,"effectiveDate":"2026-01-15","active":true},"timestamp":"2026-01-15T10:30:00Z"}
                    """))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Validation failed — e.g. `ruleCode`/`title`/`validationExpression` blank, or `severity`/`validationType` missing"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Missing or invalid JWT"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Caller is not ADMIN or SENIOR_OFFICER"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "An active rule already exists for this rule_code", content = @Content(mediaType = "application/json", examples = @ExampleObject(value = """
                    {"success":false,"message":"An active rule already exists for rule_code LM-6.1.E — use PUT to create a new version instead","timestamp":"2026-01-15T10:30:00Z"}
                    """)))
    })
    @PreAuthorize("hasAnyRole('ADMIN', 'SENIOR_OFFICER')")
    @PostMapping
    public ResponseEntity<ApiResponse<RuleResponse>> create(@Valid @RequestBody RuleRequest request) {
        return ResponseUtil.created(ruleMapper.toResponse(ruleService.create(request)));
    }

    @Operation(summary = "Update a rule — creates a new version and deactivates the previous one; never mutates an already-evaluated rule row. Admin only.",
            description = "`ruleCode` is immutable — the request body's `ruleCode` must match the existing rule's, or this fails with 400.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "New version created", content = @Content(mediaType = "application/json", examples = @ExampleObject(value = """
                    {"success":true,"message":"Rule updated to a new version successfully","data":{"id":"22334455-6677-4a2e-9c1a-1e2f3a4b5c6d","ruleCode":"LM-6.1.E","title":"MRP must be declared clearly","description":"Every pre-packaged commodity must declare its Maximum Retail Price in a readable font","legalReference":"Legal Metrology (Packaged Commodities) Rules, 2011","category":"Mandatory Declaration","severity":"CRITICAL","validationType":"FIELD_EXISTS","validationExpression":"{\\"field\\":\\"MRP\\"}","mandatory":true,"suggestion":"Add MRP declaration to the front label","penaltyReference":"Section 36","version":2,"effectiveDate":"2026-01-15","active":true},"timestamp":"2026-01-15T10:30:00Z"}
                    """))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Validation failed, or the request body's ruleCode does not match the existing rule's (rule_code is immutable)"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Missing or invalid JWT"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Caller is not ADMIN or SENIOR_OFFICER"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "No rule exists with the given id")
    })
    @PreAuthorize("hasAnyRole('ADMIN', 'SENIOR_OFFICER')")
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<RuleResponse>> update(@PathVariable UUID id, @Valid @RequestBody RuleRequest request) {
        return ResponseUtil.ok("Rule updated to a new version successfully", ruleMapper.toResponse(ruleService.update(id, request)));
    }

    @Operation(summary = "Deactivate a rule (soft delete — historical violations keep their reference). Admin only.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Rule deactivated", content = @Content(mediaType = "application/json", examples = @ExampleObject(value = """
                    {"success":true,"message":"Rule deactivated successfully","timestamp":"2026-01-15T10:30:00Z"}
                    """))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Missing or invalid JWT"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Caller is not ADMIN or SENIOR_OFFICER"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "No rule exists with the given id")
    })
    @PreAuthorize("hasAnyRole('ADMIN', 'SENIOR_OFFICER')")
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID id) {
        ruleService.delete(id);
        return ResponseUtil.noContent("Rule deactivated successfully");
    }

    @Operation(summary = "Reload the Rule Loader's in-memory cache from the database. Admin only.",
            description = "Use after a direct database change to rule data, or if a previous write's cache refresh is suspected to have failed. Takes effect immediately, no restart required.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Cache refreshed", content = @Content(mediaType = "application/json", examples = @ExampleObject(value = """
                    {"success":true,"message":"Rule cache refreshed successfully","timestamp":"2026-01-15T10:30:00Z"}
                    """))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Missing or invalid JWT"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Caller is not ADMIN or SENIOR_OFFICER")
    })
    @PreAuthorize("hasAnyRole('ADMIN', 'SENIOR_OFFICER')")
    @PostMapping("/refresh-cache")
    public ResponseEntity<ApiResponse<Void>> refreshCache() {
        ruleService.refreshCache();
        return ResponseUtil.noContent("Rule cache refreshed successfully");
    }
}
