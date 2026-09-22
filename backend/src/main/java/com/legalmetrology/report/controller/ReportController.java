package com.legalmetrology.report.controller;

import com.legalmetrology.common.response.ApiResponse;
import com.legalmetrology.report.dto.ComplianceReportResponse;
import com.legalmetrology.report.model.ReportData;
import com.legalmetrology.report.service.ReportService;
import com.legalmetrology.security.SecurityUtils;
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

@Tag(name = "Reports", description = "Smart Reports — Executive Summary, evidence, decision trace, compliance/risk score, and AI recommendations, exported as PDF, DOCX, or JSON")
@RestController
@RequestMapping("/api/v1/reports")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;
    private final SecurityUtils securityUtils;

    @Operation(summary = "Get a compliance report by id")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Report found", content = @Content(mediaType = "application/json", examples = @ExampleObject(value = """
                    {"success":true,"message":"Request completed successfully","data":{"id":"b1c2d3e4-3333-4a2e-9c1a-1e2f3a4b5c6d","inspectionId":"7b1e2f3a-4b5c-4d6e-8f90-1a2b3c4d5e6f","reportNumber":"LM-20260115-0042","pdfUrl":"https://xyzcompany.supabase.co/storage/v1/object/sign/reports/LM-20260115-0042.pdf?token=...","docxUrl":"https://xyzcompany.supabase.co/storage/v1/object/sign/reports/LM-20260115-0042.docx?token=...","generatedByName":"Inspector Priya Sharma","generatedAt":"2026-01-15T10:30:00Z"},"timestamp":"2026-01-15T10:30:00Z"}
                    """))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Missing or invalid JWT"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "No compliance report exists with the given id")
    })
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ComplianceReportResponse>> getById(@PathVariable UUID id) {
        return ResponseUtil.ok(reportService.getById(id));
    }

    @Operation(summary = "List all reports generated for an inspection",
            description = "Returns an empty list (not a 404) if no report has been generated yet, or if the inspection id doesn't exist — this endpoint does not validate the inspection.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Reports for the inspection (may be empty)", content = @Content(mediaType = "application/json", examples = @ExampleObject(value = """
                    {"success":true,"message":"Request completed successfully","data":[{"id":"b1c2d3e4-3333-4a2e-9c1a-1e2f3a4b5c6d","inspectionId":"7b1e2f3a-4b5c-4d6e-8f90-1a2b3c4d5e6f","reportNumber":"LM-20260115-0042","pdfUrl":"https://xyzcompany.supabase.co/storage/v1/object/sign/reports/LM-20260115-0042.pdf?token=...","docxUrl":"https://xyzcompany.supabase.co/storage/v1/object/sign/reports/LM-20260115-0042.docx?token=...","generatedByName":"Inspector Priya Sharma","generatedAt":"2026-01-15T10:30:00Z"}],"timestamp":"2026-01-15T10:30:00Z"}
                    """))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Missing or invalid JWT")
    })
    @GetMapping("/inspections/{inspectionId}")
    public ResponseEntity<ApiResponse<List<ComplianceReportResponse>>> listByInspection(@PathVariable UUID inspectionId) {
        return ResponseUtil.ok(reportService.listByInspection(inspectionId));
    }

    @Operation(summary = "Generate a new Smart Report (PDF + DOCX) for an inspection and persist it",
            description = "Assembles the Executive Summary, evidence, decision trace, compliance/risk score, and AI recommendations, renders both a PDF and a DOCX, "
                    + "uploads both to Supabase Storage, and persists a new `ComplianceReport` row. The narrative sections fall back to a deterministic summary "
                    + "if the LLM provider is unavailable, so this only fails on a genuine inspection lookup or storage failure. Any authenticated user may trigger this.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Report generated and persisted", content = @Content(mediaType = "application/json", examples = @ExampleObject(value = """
                    {"success":true,"message":"Resource created successfully","data":{"id":"b1c2d3e4-3333-4a2e-9c1a-1e2f3a4b5c6d","inspectionId":"7b1e2f3a-4b5c-4d6e-8f90-1a2b3c4d5e6f","reportNumber":"LM-20260115-0042","pdfUrl":"https://xyzcompany.supabase.co/storage/v1/object/sign/reports/LM-20260115-0042.pdf?token=...","docxUrl":"https://xyzcompany.supabase.co/storage/v1/object/sign/reports/LM-20260115-0042.docx?token=...","generatedByName":"Inspector Priya Sharma","generatedAt":"2026-01-15T10:30:00Z"},"timestamp":"2026-01-15T10:30:00Z"}
                    """))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Missing or invalid JWT"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "No inspection exists with the given id"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "502", description = "Uploading the generated PDF/DOCX to Supabase Storage failed")
    })
    @PostMapping("/inspections/{inspectionId}/generate")
    public ResponseEntity<ApiResponse<ComplianceReportResponse>> generate(@PathVariable UUID inspectionId) {
        var report = reportService.generate(inspectionId, securityUtils.getCurrentUserId());
        return ResponseUtil.created(report);
    }

    @Operation(summary = "The same report data as JSON — assembled fresh on every call, nothing is stored for this export",
            description = "Useful for previewing report contents before committing to `/generate`, or for programmatic consumption. "
                    + "`reportNumber` in the response is a fixed placeholder (\"(preview — not persisted)\") since no report row is created.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Assembled report data", content = @Content(mediaType = "application/json", examples = @ExampleObject(value = """
                    {"success":true,"message":"Request completed successfully","data":{"inspectionId":"7b1e2f3a-4b5c-4d6e-8f90-1a2b3c4d5e6f","reportNumber":"(preview — not persisted)","generatedAt":"2026-01-15T10:30:00Z","inspection":{"inspectorName":"Priya Sharma","status":"COMPLETED","region":"South","locationLat":12.9716,"locationLng":77.5946,"startedAt":"2026-01-15T09:00:00Z","completedAt":"2026-01-15T09:45:00Z"},"product":{"productName":"Aashirvaad Atta 5kg","categoryName":"Food & Grocery","manufacturerName":"ITC Limited","manufacturerAddress":"37 J.C. Road, Bengaluru, Karnataka","barcode":"8901058854488"},"compliance":{"complianceScore":72.5,"fraudRisk":"MEDIUM","violationCount":2,"criticalCount":1},"declarations":[{"type":"MRP","present":true,"value":"45.00","confidence":0.93,"labelSection":"FRONT"}],"violations":[{"violationId":"d3e4f5a6-789a-4bcd-9ef0-123456789abc","ruleCode":"LM-6.1.E","ruleTitle":"MRP must be declared","severity":"CRITICAL","field":"MRP","description":"MRP declaration not found on any image face","actualValue":"Missing","expectedValue":"Present and legible","suggestedFix":"Add MRP declaration to the front label","legalReference":"LM Rule 6(1)(e)"}],"evidence":[{"evidenceId":"e1a2b3c4-1111-4a2e-9c1a-1e2f3a4b5c6d","violationId":"d3e4f5a6-789a-4bcd-9ef0-123456789abc","ruleCode":"LM-6.1.E","severity":"CRITICAL","reason":"MRP declaration not found on any image face","sha256Hash":"a94a8fe5ccb19ba61c4c0873d391e987982fbbd3","originalImageUrl":"https://xyzcompany.supabase.co/storage/v1/object/sign/images/original.jpg?token=...","annotatedImageUrl":"https://xyzcompany.supabase.co/storage/v1/object/sign/images/annotated.jpg?token=..."}],"decisionTrace":[{"stepName":"RULE_EVALUATION","module":"InspectionEvaluationServiceImpl","status":"SUCCESS","confidence":0.93,"executionTimeMs":142,"startedAt":"2026-01-15T09:44:00Z","outputSummary":"12 rules evaluated, 2 violations found"}],"timeline":[{"fromStatus":"IN_PROGRESS","toStatus":"COMPLETED","changedByName":"Priya Sharma","note":"Inspection completed on-site","occurredAt":"2026-01-15T09:45:00Z"}],"legalReferences":["LM Rule 6(1)(e)"],"executiveSummary":"The inspection identified 2 violations, 1 of which is critical (missing MRP declaration).","recommendations":"Manufacturer should be issued a compliance notice for the missing MRP declaration."},"timestamp":"2026-01-15T10:30:00Z"}
                    """))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Missing or invalid JWT"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "No inspection exists with the given id")
    })
    @GetMapping("/inspections/{inspectionId}/json")
    public ResponseEntity<ApiResponse<ReportData>> getReportJson(@PathVariable UUID inspectionId) {
        return ResponseUtil.ok(reportService.getReportData(inspectionId, securityUtils.getCurrentUserId()));
    }
}
