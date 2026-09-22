package com.legalmetrology.analytics.controller;

import com.legalmetrology.analytics.dto.ManufacturerDrillDownResponse;
import com.legalmetrology.analytics.model.DimensionMetric;
import com.legalmetrology.analytics.model.FieldCategoryCount;
import com.legalmetrology.analytics.model.KpiSnapshot;
import com.legalmetrology.analytics.model.MonthlyMetric;
import com.legalmetrology.analytics.model.RegionSeverityCount;
import com.legalmetrology.analytics.risk.RiskIndexService;
import com.legalmetrology.analytics.service.AnalyticsQueryService;
import com.legalmetrology.common.response.ApiResponse;
import com.legalmetrology.exception.ResourceNotFoundException;
import com.legalmetrology.utils.ResponseUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * Step 12 — the raw "Analytics" layer of the dashboard: KPIs, time series,
 * heatmaps, treemaps, leaderboards, and drill-down. Consumes only
 * {@code AnalyticsQueryService} — structured, persisted inspection/violation
 * data — never OCR/Vision output or the rule engine's live evaluation logic.
 */
@Tag(name = "Analytics Dashboard", description = "Step 12 — KPIs, time series, heatmaps, treemaps, leaderboards, drill-down")
@RestController
@RequestMapping("/api/v1/analytics")
@RequiredArgsConstructor
public class AnalyticsController {

    private final AnalyticsQueryService analyticsQueryService;
    private final RiskIndexService riskIndexService;

    @Operation(summary = "Top-line KPI snapshot: inspections, violations, compliance rate, active officers/rules",
            description = "Authorization: any authenticated user.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Current KPI snapshot",
                    content = @Content(mediaType = "application/json", examples = @ExampleObject(value = """
                            {
                              "success": true,
                              "message": "Request completed successfully",
                              "data": {
                                "totalInspections": 1240,
                                "completedInspections": 980,
                                "totalViolations": 312,
                                "criticalViolations": 47,
                                "averageComplianceScore": 78.35,
                                "complianceRatePercent": 74.84,
                                "activeInspectors": 26,
                                "activeRules": 18
                              },
                              "timestamp": "2026-01-15T10:30:00Z"
                            }
                            """))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Missing or invalid access token")
    })
    @GetMapping("/kpis")
    public ResponseEntity<ApiResponse<KpiSnapshot>> kpis() {
        return ResponseUtil.ok(analyticsQueryService.getKpiSnapshot());
    }

    @Operation(summary = "Monthly violation count time series",
            description = "Authorization: any authenticated user.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Monthly violation counts, oldest first",
                    content = @Content(mediaType = "application/json", examples = @ExampleObject(value = """
                            {
                              "success": true,
                              "message": "Request completed successfully",
                              "data": [
                                {"month": "2025-08", "count": 41, "averageValue": null},
                                {"month": "2025-09", "count": 53, "averageValue": null}
                              ],
                              "timestamp": "2026-01-15T10:30:00Z"
                            }
                            """))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "months is not a valid integer"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Missing or invalid access token")
    })
    @GetMapping("/timeseries/violations")
    public ResponseEntity<ApiResponse<List<MonthlyMetric>>> violationTimeSeries(
            @Parameter(description = "How many trailing months to include", example = "6") @RequestParam(defaultValue = "6") int months) {
        return ResponseUtil.ok(analyticsQueryService.monthlyViolationCounts(months));
    }

    @Operation(summary = "Monthly inspection count time series (workload)",
            description = "Authorization: any authenticated user.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Monthly inspection counts, oldest first",
                    content = @Content(mediaType = "application/json", examples = @ExampleObject(value = """
                            {
                              "success": true,
                              "message": "Request completed successfully",
                              "data": [
                                {"month": "2025-08", "count": 95, "averageValue": null},
                                {"month": "2025-09", "count": 110, "averageValue": null}
                              ],
                              "timestamp": "2026-01-15T10:30:00Z"
                            }
                            """))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "months is not a valid integer"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Missing or invalid access token")
    })
    @GetMapping("/timeseries/inspections")
    public ResponseEntity<ApiResponse<List<MonthlyMetric>>> inspectionTimeSeries(
            @Parameter(description = "How many trailing months to include", example = "6") @RequestParam(defaultValue = "6") int months) {
        return ResponseUtil.ok(analyticsQueryService.monthlyInspectionCounts(months));
    }

    @Operation(summary = "Monthly average compliance score time series",
            description = "Authorization: any authenticated user.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Monthly average compliance scores, oldest first",
                    content = @Content(mediaType = "application/json", examples = @ExampleObject(value = """
                            {
                              "success": true,
                              "message": "Request completed successfully",
                              "data": [
                                {"month": "2025-08", "count": 95, "averageValue": 76.20},
                                {"month": "2025-09", "count": 110, "averageValue": 79.45}
                              ],
                              "timestamp": "2026-01-15T10:30:00Z"
                            }
                            """))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "months is not a valid integer"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Missing or invalid access token")
    })
    @GetMapping("/timeseries/compliance-score")
    public ResponseEntity<ApiResponse<List<MonthlyMetric>>> complianceScoreTimeSeries(
            @Parameter(description = "How many trailing months to include", example = "6") @RequestParam(defaultValue = "6") int months) {
        return ResponseUtil.ok(analyticsQueryService.monthlyAverageComplianceScore(months));
    }

    @Operation(summary = "Heatmap: violation count by region x severity",
            description = "Authorization: any authenticated user.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "One row per (region, severity) cell",
                    content = @Content(mediaType = "application/json", examples = @ExampleObject(value = """
                            {
                              "success": true,
                              "message": "Request completed successfully",
                              "data": [
                                {"region": "New Delhi", "severity": "CRITICAL", "count": 12},
                                {"region": "Mumbai", "severity": "MAJOR", "count": 8}
                              ],
                              "timestamp": "2026-01-15T10:30:00Z"
                            }
                            """))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Missing or invalid access token")
    })
    @GetMapping("/heatmap/region-severity")
    public ResponseEntity<ApiResponse<List<RegionSeverityCount>>> regionSeverityHeatmap() {
        return ResponseUtil.ok(analyticsQueryService.severityCountsByRegion());
    }

    @Operation(summary = "Heatmap: violation count by declared field x product category",
            description = "Authorization: any authenticated user.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "One row per (field, category) cell",
                    content = @Content(mediaType = "application/json", examples = @ExampleObject(value = """
                            {
                              "success": true,
                              "message": "Request completed successfully",
                              "data": [
                                {"field": "NET_QUANTITY", "category": "Food", "count": 34},
                                {"field": "MRP", "category": "Electronics", "count": 19}
                              ],
                              "timestamp": "2026-01-15T10:30:00Z"
                            }
                            """))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Missing or invalid access token")
    })
    @GetMapping("/heatmap/field-category")
    public ResponseEntity<ApiResponse<List<FieldCategoryCount>>> fieldCategoryHeatmap() {
        return ResponseUtil.ok(analyticsQueryService.violationFieldCountsByCategory());
    }

    @Operation(summary = "Treemap: violation share by manufacturer",
            description = "Authorization: any authenticated user.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "One row per manufacturer",
                    content = @Content(mediaType = "application/json", examples = @ExampleObject(value = """
                            {
                              "success": true,
                              "message": "Request completed successfully",
                              "data": [
                                {"entityId": "d4e5f6a7-8b9c-4d0e-9f1a-2b3c4d5e6f7a", "label": "Tata Consumer Products", "count": 22, "averageScore": null}
                              ],
                              "timestamp": "2026-01-15T10:30:00Z"
                            }
                            """))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Missing or invalid access token")
    })
    @GetMapping("/treemap/manufacturers")
    public ResponseEntity<ApiResponse<List<DimensionMetric>>> manufacturerTreemap() {
        return ResponseUtil.ok(analyticsQueryService.violationCountsByManufacturer());
    }

    @Operation(summary = "Treemap: violation share by product category",
            description = "Authorization: any authenticated user.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "One row per product category",
                    content = @Content(mediaType = "application/json", examples = @ExampleObject(value = """
                            {
                              "success": true,
                              "message": "Request completed successfully",
                              "data": [
                                {"entityId": null, "label": "Food", "count": 128, "averageScore": null}
                              ],
                              "timestamp": "2026-01-15T10:30:00Z"
                            }
                            """))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Missing or invalid access token")
    })
    @GetMapping("/treemap/categories")
    public ResponseEntity<ApiResponse<List<DimensionMetric>>> categoryTreemap() {
        return ResponseUtil.ok(analyticsQueryService.violationCountsByCategory());
    }

    @Operation(summary = "Leaderboard: most-violated rules",
            description = "Authorization: any authenticated user.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Rules ranked by violation count, descending",
                    content = @Content(mediaType = "application/json", examples = @ExampleObject(value = """
                            {
                              "success": true,
                              "message": "Request completed successfully",
                              "data": [
                                {"entityId": "9c8b7a6f-5e4d-4c3b-9a1f-0e2d3c4b5a6f", "label": "Net Quantity Declaration", "count": 61, "averageScore": null}
                              ],
                              "timestamp": "2026-01-15T10:30:00Z"
                            }
                            """))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "limit is not a valid integer"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Missing or invalid access token")
    })
    @GetMapping("/leaderboard/rules")
    public ResponseEntity<ApiResponse<List<DimensionMetric>>> ruleLeaderboard(
            @Parameter(description = "Maximum number of rules to return", example = "10") @RequestParam(defaultValue = "10") int limit) {
        return ResponseUtil.ok(analyticsQueryService.mostViolatedRules(limit));
    }

    @Operation(summary = "Leaderboard: manufacturers ranked by violation count",
            description = "Authorization: any authenticated user.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Manufacturers ranked by violation count, descending",
                    content = @Content(mediaType = "application/json", examples = @ExampleObject(value = """
                            {
                              "success": true,
                              "message": "Request completed successfully",
                              "data": [
                                {"entityId": "d4e5f6a7-8b9c-4d0e-9f1a-2b3c4d5e6f7a", "label": "Tata Consumer Products", "count": 22, "averageScore": null}
                              ],
                              "timestamp": "2026-01-15T10:30:00Z"
                            }
                            """))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Missing or invalid access token")
    })
    @GetMapping("/leaderboard/manufacturers")
    public ResponseEntity<ApiResponse<List<DimensionMetric>>> manufacturerLeaderboard() {
        return ResponseUtil.ok(analyticsQueryService.violationCountsByManufacturer());
    }

    @Operation(summary = "Leaderboard: inspectors ranked by inspection activity",
            description = "Authorization: any authenticated user.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Inspectors ranked by inspection count, descending",
                    content = @Content(mediaType = "application/json", examples = @ExampleObject(value = """
                            {
                              "success": true,
                              "message": "Request completed successfully",
                              "data": [
                                {"entityId": "b3f1c2d4-5e6f-4a7b-8c9d-0e1f2a3b4c5d", "label": "Priya Sharma", "count": 47, "averageScore": null}
                              ],
                              "timestamp": "2026-01-15T10:30:00Z"
                            }
                            """))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Missing or invalid access token")
    })
    @GetMapping("/leaderboard/inspectors")
    public ResponseEntity<ApiResponse<List<DimensionMetric>>> inspectorLeaderboard() {
        return ResponseUtil.ok(analyticsQueryService.inspectorActivity());
    }

    @Operation(summary = "Severity distribution across all violations",
            description = "Authorization: any authenticated user.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "One row per severity level",
                    content = @Content(mediaType = "application/json", examples = @ExampleObject(value = """
                            {
                              "success": true,
                              "message": "Request completed successfully",
                              "data": [
                                {"entityId": null, "label": "CRITICAL", "count": 47, "averageScore": null},
                                {"entityId": null, "label": "MAJOR", "count": 128, "averageScore": null}
                              ],
                              "timestamp": "2026-01-15T10:30:00Z"
                            }
                            """))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Missing or invalid access token")
    })
    @GetMapping("/severity-distribution")
    public ResponseEntity<ApiResponse<List<DimensionMetric>>> severityDistribution() {
        return ResponseUtil.ok(analyticsQueryService.severityDistribution());
    }

    @Operation(summary = "Drill-down: one manufacturer's risk score and monthly violation trend",
            description = "\"Not found\" here means the manufacturer has no computed risk profile (e.g. no violations " +
                    "recorded for it yet), not that the manufacturer id itself is unknown to the system. " +
                    "Authorization: any authenticated user.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Risk score and trend for the manufacturer",
                    content = @Content(mediaType = "application/json", examples = @ExampleObject(value = """
                            {
                              "success": true,
                              "message": "Request completed successfully",
                              "data": {
                                "manufacturerId": "d4e5f6a7-8b9c-4d0e-9f1a-2b3c4d5e6f7a",
                                "riskScore": {
                                  "entityType": "MANUFACTURER",
                                  "entityId": "d4e5f6a7-8b9c-4d0e-9f1a-2b3c4d5e6f7a",
                                  "label": "Tata Consumer Products",
                                  "riskScore": 62.40,
                                  "riskBand": "MEDIUM",
                                  "explainability": {
                                    "confidence": 0.82,
                                    "methodology": "Weighted Composite Risk Model",
                                    "inputDataPeriod": {"from": "2025-07-15", "to": "2026-01-15", "sampleSize": 22, "description": "Trailing 6 months (22 data points)"},
                                    "reasoning": ["22 violations across 6 months", "3 critical-severity violations"],
                                    "supportingMetrics": {"violationCount": 22, "criticalCount": 3},
                                    "assumptions": ["Assumes recent violation history is representative of ongoing risk"],
                                    "generatedAt": "2026-01-15T10:30:00Z"
                                  }
                                },
                                "monthlyViolationTrend": [
                                  {"month": "2025-12", "count": 4, "averageValue": null},
                                  {"month": "2026-01", "count": 6, "averageValue": null}
                                ]
                              },
                              "timestamp": "2026-01-15T10:30:00Z"
                            }
                            """))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "months is not a valid integer"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Missing or invalid access token"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "No risk profile exists for this manufacturer id",
                    content = @Content(mediaType = "application/json", examples = @ExampleObject(value = """
                            {"success": false, "message": "Manufacturer risk profile not found with id: d4e5f6a7-8b9c-4d0e-9f1a-2b3c4d5e6f7a", "timestamp": "2026-01-15T10:30:00Z"}
                            """)))
    })
    @GetMapping("/drilldown/manufacturers/{manufacturerId}")
    public ResponseEntity<ApiResponse<ManufacturerDrillDownResponse>> manufacturerDrillDown(
            @Parameter(description = "Manufacturer id", example = "d4e5f6a7-8b9c-4d0e-9f1a-2b3c4d5e6f7a") @PathVariable UUID manufacturerId,
            @Parameter(description = "How many trailing months of trend to include", example = "6") @RequestParam(defaultValue = "6") int months) {
        var riskScore = riskIndexService.manufacturerRiskIndex().stream()
                .filter(r -> manufacturerId.equals(r.entityId()))
                .findFirst()
                .orElseThrow(() -> ResourceNotFoundException.of("Manufacturer risk profile", manufacturerId));

        var trend = analyticsQueryService.monthlyViolationCountsForManufacturer(manufacturerId, months);
        return ResponseUtil.ok(new ManufacturerDrillDownResponse(manufacturerId, riskScore, trend));
    }
}
