package com.legalmetrology.trace.controller;

import com.legalmetrology.common.response.ApiResponse;
import com.legalmetrology.trace.dto.DecisionTraceStepResponse;
import com.legalmetrology.trace.mapper.DecisionTraceStepMapper;
import com.legalmetrology.trace.service.DecisionTraceService;
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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@Tag(name = "Decision Trace", description = "The ordered, replayable log of every pipeline stage executed for an inspection — image quality, OCR, vision, fusion, rule evaluation, scoring, evidence, and report generation")
@RestController
@RequestMapping("/api/v1/inspections")
@RequiredArgsConstructor
public class DecisionTraceController {

    private final DecisionTraceService decisionTraceService;
    private final DecisionTraceStepMapper decisionTraceStepMapper;

    @Operation(summary = "Full decision trace for an inspection, in execution order — the timeline/replay view",
            description = "Every pipeline stage (image quality, OCR, vision, fusion, rule evaluation, scoring, evidence, report generation) that has executed "
                    + "so far for this inspection, oldest first, including failed/skipped steps with their reason. The list grows as the pipeline progresses.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Decision trace (may be empty if no pipeline stage has run yet)", content = @Content(mediaType = "application/json", examples = @ExampleObject(value = """
                    {"success":true,"message":"Request completed successfully","data":[{"id":"01234567-89ab-4cde-9f01-23456789abcd","inspectionId":"7b1e2f3a-4b5c-4d6e-8f90-1a2b3c4d5e6f","stepName":"RULE_EVALUATION","module":"InspectionEvaluationServiceImpl","inputSummary":"12 active rules, 8 fused declarations","outputSummary":"12 rules evaluated, 2 violations found","confidence":0.93,"executionTimeMs":142,"startedAt":"2026-01-15T09:44:00Z","status":"SUCCESS","reason":null,"referencedRuleId":null,"referencedEvidenceId":null,"referencedImageId":null,"createdAt":"2026-01-15T09:44:00Z"}],"timestamp":"2026-01-15T10:30:00Z"}
                    """))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Missing or invalid JWT"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "No inspection exists with the given id")
    })
    @GetMapping("/{inspectionId}/decision-trace")
    public ResponseEntity<ApiResponse<List<DecisionTraceStepResponse>>> getTrace(@PathVariable UUID inspectionId) {
        var trace = decisionTraceService.getTrace(inspectionId).stream().map(decisionTraceStepMapper::toResponse).toList();
        return ResponseUtil.ok(trace);
    }
}
