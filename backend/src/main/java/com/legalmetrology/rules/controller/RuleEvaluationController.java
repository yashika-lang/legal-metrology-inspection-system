package com.legalmetrology.rules.controller;

import com.legalmetrology.common.response.ApiResponse;
import com.legalmetrology.rules.dto.EvaluationResponse;
import com.legalmetrology.rules.dto.ValidationResultResponse;
import com.legalmetrology.rules.dto.ViolationResponse;
import com.legalmetrology.rules.mapper.ViolationMapper;
import com.legalmetrology.rules.service.InspectionEvaluationService;
import com.legalmetrology.rules.violation.ViolationGenerationService;
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

@Tag(name = "Rule Evaluation", description = "Runs the rule engine against an inspection's fused declarations — violations, compliance score, and explainability")
@RestController
@RequestMapping("/api/v1/rules/inspections")
@RequiredArgsConstructor
public class RuleEvaluationController {

    private final InspectionEvaluationService inspectionEvaluationService;
    private final ViolationGenerationService violationGenerationService;
    private final ViolationMapper violationMapper;

    @Operation(summary = "Evaluate every active rule against the inspection, persist violations, and compute the compliance score",
            description = "Builds the rule evaluation context from the inspection's fused declarations, runs every currently-active rule, persists a "
                    + "`Violation` row for each failure, and computes the compliance score and fraud risk. Re-running replaces previously persisted violations. Any authenticated user may trigger this.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Evaluation completed", content = @Content(mediaType = "application/json", examples = @ExampleObject(value = """
                    {"success":true,"message":"Evaluation completed","data":{"inspectionId":"7b1e2f3a-4b5c-4d6e-8f90-1a2b3c4d5e6f","complianceScore":72.5,"fraudRisk":"MEDIUM","violations":[{"id":"d3e4f5a6-789a-4bcd-9ef0-123456789abc","inspectionId":"7b1e2f3a-4b5c-4d6e-8f90-1a2b3c4d5e6f","ruleId":"11223344-5566-4a2e-9c1a-1e2f3a4b5c6d","ruleCode":"LM-6.1.E","ruleTitle":"MRP must be declared","severity":"CRITICAL","field":"MRP","description":"MRP declaration not found on any image face","actualValue":"Missing","expectedValue":"Present and legible","confidence":0.0,"boundingBox":null,"suggestedFix":"Add MRP declaration to the front label","legalReference":"LM Rule 6(1)(e)","status":"OPEN","createdAt":"2026-01-15T10:30:00Z"}],"results":[{"ruleId":"11223344-5566-4a2e-9c1a-1e2f3a4b5c6d","ruleCode":"LM-6.1.E","ruleTitle":"MRP must be declared","severity":"CRITICAL","passed":false,"field":"MRP","actualValue":"Missing","expectedValue":"Present and legible","confidence":0.0,"boundingBox":null,"explanation":"No fused declaration found for MRP"}]},"timestamp":"2026-01-15T10:30:00Z"}
                    """))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Missing or invalid JWT"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "No inspection exists with the given id")
    })
    @PostMapping("/{inspectionId}/evaluate")
    public ResponseEntity<ApiResponse<EvaluationResponse>> evaluate(@PathVariable UUID inspectionId) {
        return ResponseUtil.ok("Evaluation completed", inspectionEvaluationService.evaluateAndScore(inspectionId));
    }

    @Operation(summary = "Explainability: every rule's pass/fail outcome, why, and what data was used — without persisting anything",
            description = "Same evaluation logic as `/evaluate`, but nothing is written to the database — useful for previewing outcomes or debugging a rule. "
                    + "Returns results built from an empty context (most rules failing as \"data not found\") rather than a 404 if the inspection id doesn't exist or has no fused declarations yet.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Every active rule's outcome", content = @Content(mediaType = "application/json", examples = @ExampleObject(value = """
                    {"success":true,"message":"Request completed successfully","data":[{"ruleId":"11223344-5566-4a2e-9c1a-1e2f3a4b5c6d","ruleCode":"LM-6.1.E","ruleTitle":"MRP must be declared","severity":"CRITICAL","passed":false,"field":"MRP","actualValue":"Missing","expectedValue":"Present and legible","confidence":0.0,"boundingBox":null,"explanation":"No fused declaration found for MRP"}],"timestamp":"2026-01-15T10:30:00Z"}
                    """))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Missing or invalid JWT")
    })
    @GetMapping("/{inspectionId}/evaluation")
    public ResponseEntity<ApiResponse<List<ValidationResultResponse>>> explain(@PathVariable UUID inspectionId) {
        var results = inspectionEvaluationService.explain(inspectionId).stream().map(violationMapper::toResponse).toList();
        return ResponseUtil.ok(results);
    }

    @Operation(summary = "Get the violations already persisted for an inspection",
            description = "Returns an empty list (not a 404) if `/evaluate` hasn't been called yet, or if the inspection id doesn't exist — this endpoint does not validate the inspection.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Persisted violations (may be empty)", content = @Content(mediaType = "application/json", examples = @ExampleObject(value = """
                    {"success":true,"message":"Request completed successfully","data":[{"id":"d3e4f5a6-789a-4bcd-9ef0-123456789abc","inspectionId":"7b1e2f3a-4b5c-4d6e-8f90-1a2b3c4d5e6f","ruleId":"11223344-5566-4a2e-9c1a-1e2f3a4b5c6d","ruleCode":"LM-6.1.E","ruleTitle":"MRP must be declared","severity":"CRITICAL","field":"MRP","description":"MRP declaration not found on any image face","actualValue":"Missing","expectedValue":"Present and legible","confidence":0.0,"boundingBox":null,"suggestedFix":"Add MRP declaration to the front label","legalReference":"LM Rule 6(1)(e)","status":"OPEN","createdAt":"2026-01-15T10:30:00Z"}],"timestamp":"2026-01-15T10:30:00Z"}
                    """))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Missing or invalid JWT")
    })
    @GetMapping("/{inspectionId}/violations")
    public ResponseEntity<ApiResponse<List<ViolationResponse>>> getViolations(@PathVariable UUID inspectionId) {
        var violations = violationGenerationService.getViolations(inspectionId).stream().map(violationMapper::toResponse).toList();
        return ResponseUtil.ok(violations);
    }
}
