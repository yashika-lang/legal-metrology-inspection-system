package com.legalmetrology.analytics.controller;

import com.legalmetrology.analytics.insight.Insight;
import com.legalmetrology.analytics.insight.InsightEngine;
import com.legalmetrology.analytics.prediction.Anomaly;
import com.legalmetrology.analytics.prediction.AnomalyDetectionService;
import com.legalmetrology.analytics.prediction.Forecast;
import com.legalmetrology.analytics.prediction.TrendForecastService;
import com.legalmetrology.analytics.risk.EntityRiskScore;
import com.legalmetrology.analytics.risk.RiskIndexService;
import com.legalmetrology.common.response.ApiResponse;
import com.legalmetrology.utils.ResponseUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

/**
 * The Intelligence Layer sitting on top of the raw Analytics data:
 * Insight Engine → Prediction Engine (forecast + anomaly detection + risk
 * index). Every insight/forecast/anomaly/risk score carries its confidence,
 * supporting data, generated time, and explanation inline on the record
 * itself — nothing here is a bare number without justification.
 */
@Tag(name = "Analytics Intelligence", description = "Insight Engine, trend forecasting, anomaly detection, and risk indexing")
@RestController
@RequestMapping("/api/v1/analytics")
@RequiredArgsConstructor
public class InsightController {

    private final InsightEngine insightEngine;
    private final TrendForecastService trendForecastService;
    private final AnomalyDetectionService anomalyDetectionService;
    private final RiskIndexService riskIndexService;

    @Operation(summary = "Generate actionable insights (trends, repeat offenders, category patterns, rule-version impact)",
            description = "Computed live on every call (not cached/pre-aggregated) — every message is template-based and " +
                    "traceable to explainability.supportingMetrics, never free-text. May return an empty list if there " +
                    "is not enough underlying data to generate any insight. Authorization: any authenticated user.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Generated insights",
                    content = @Content(mediaType = "application/json", examples = @ExampleObject(value = """
                            {
                              "success": true,
                              "message": "Request completed successfully",
                              "data": [
                                {
                                  "id": "7a8b9c0d-1e2f-4a3b-8c4d-5e6f7a8b9c0d",
                                  "type": "TREND",
                                  "title": "Violations trending upward in New Delhi",
                                  "message": "Violation count rose 24% over the trailing 3 months in New Delhi, driven mainly by NET_QUANTITY declarations.",
                                  "explainability": {
                                    "confidence": 0.78,
                                    "methodology": "Month-over-month percentage change",
                                    "inputDataPeriod": {"from": "2025-10-15", "to": "2026-01-15", "sampleSize": 3, "description": "Trailing 3 months (3 data points)"},
                                    "reasoning": ["41 violations in October", "53 in November", "61 in December"],
                                    "supportingMetrics": {"octoberCount": 41, "novemberCount": 53, "decemberCount": 61},
                                    "assumptions": ["Assumes reporting volume is stable across the period"],
                                    "generatedAt": "2026-01-15T10:30:00Z"
                                  }
                                }
                              ],
                              "timestamp": "2026-01-15T10:30:00Z"
                            }
                            """))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Missing or invalid access token")
    })
    @GetMapping("/insights")
    public ResponseEntity<ApiResponse<List<Insight>>> insights() {
        return ResponseUtil.ok(insightEngine.generateAll());
    }

    @Operation(summary = "Forecast next month's violation count and inspection workload",
            description = "Uses ordinary least-squares linear regression over historical monthly data; the response " +
                    "exposes the exact regression equation and R² alongside the predicted value, never a bare number. " +
                    "Authorization: any authenticated user.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Two forecasts: violations, then inspection workload",
                    content = @Content(mediaType = "application/json", examples = @ExampleObject(value = """
                            {
                              "success": true,
                              "message": "Request completed successfully",
                              "data": [
                                {
                                  "metric": "MONTHLY_VIOLATIONS",
                                  "targetPeriod": "2026-02",
                                  "predictedValue": 67.20,
                                  "confidenceInterval": {"lowerBound": 58.10, "upperBound": 76.30, "confidenceLevel": 0.95},
                                  "historicalDataUsed": [
                                    {"month": "2025-12", "count": 61, "averageValue": null},
                                    {"month": "2026-01", "count": 64, "averageValue": null}
                                  ],
                                  "regressionEquation": "y = 3.10x + 45.20",
                                  "rSquared": 0.87,
                                  "explainability": {
                                    "confidence": 0.87,
                                    "methodology": "Ordinary Least-Squares Linear Regression",
                                    "inputDataPeriod": {"from": "2025-08-15", "to": "2026-01-15", "sampleSize": 6, "description": "Trailing 6 months (6 data points)"},
                                    "reasoning": ["Consistent upward trend over 6 months", "R\\u00b2 of 0.87 indicates a strong linear fit"],
                                    "supportingMetrics": {"slope": 3.10, "intercept": 45.20},
                                    "assumptions": ["Assumes residuals are approximately normally distributed"],
                                    "generatedAt": "2026-01-15T10:30:00Z"
                                  }
                                }
                              ],
                              "timestamp": "2026-01-15T10:30:00Z"
                            }
                            """))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Missing or invalid access token")
    })
    @GetMapping("/predictions/forecast")
    public ResponseEntity<ApiResponse<List<Forecast>>> forecasts() {
        return ResponseUtil.ok(List.of(
                trendForecastService.forecastNextMonthViolations(),
                trendForecastService.forecastNextMonthInspectionWorkload()
        ));
    }

    @Operation(summary = "Detect statistical anomalies — violation spikes and regions behaving differently",
            description = "Z-score based detection against each dimension's historical baseline; returns an empty list " +
                    "when nothing crosses the configured threshold. Authorization: any authenticated user.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Detected anomalies (may be empty)",
                    content = @Content(mediaType = "application/json", examples = @ExampleObject(value = """
                            {
                              "success": true,
                              "message": "Request completed successfully",
                              "data": [
                                {
                                  "dimension": "REGION",
                                  "dimensionValue": "Chennai",
                                  "expectedValue": 18.40,
                                  "actualValue": 39.00,
                                  "zScore": 3.12,
                                  "threshold": 2.5,
                                  "reason": "Violation count in Chennai is 3.12 standard deviations above its historical average",
                                  "explainability": {
                                    "confidence": 0.91,
                                    "methodology": "Z-Score Anomaly Detection",
                                    "inputDataPeriod": {"from": "2025-07-15", "to": "2026-01-15", "sampleSize": 6, "description": "Trailing 6 months (6 data points)"},
                                    "reasoning": ["Historical mean 18.4, standard deviation 6.6", "Current value 39.0 exceeds threshold of 2.5 standard deviations"],
                                    "supportingMetrics": {"mean": 18.40, "stdDev": 6.60},
                                    "assumptions": ["Assumes the historical baseline follows an approximately normal distribution"],
                                    "generatedAt": "2026-01-15T10:30:00Z"
                                  }
                                }
                              ],
                              "timestamp": "2026-01-15T10:30:00Z"
                            }
                            """))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Missing or invalid access token")
    })
    @GetMapping("/predictions/anomalies")
    public ResponseEntity<ApiResponse<List<Anomaly>>> anomalies() {
        List<Anomaly> anomalies = new ArrayList<>();
        anomalies.addAll(anomalyDetectionService.detectViolationSpikes());
        anomalies.addAll(anomalyDetectionService.detectRegionalAnomalies());
        return ResponseUtil.ok(anomalies);
    }

    @Operation(summary = "Manufacturer risk index — most risky manufacturers",
            description = "Ranked descending by riskScore using the Weighted Composite Risk Model. Authorization: any authenticated user.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Manufacturer risk scores, highest risk first",
                    content = @Content(mediaType = "application/json", examples = @ExampleObject(value = """
                            {
                              "success": true,
                              "message": "Request completed successfully",
                              "data": [
                                {
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
                                }
                              ],
                              "timestamp": "2026-01-15T10:30:00Z"
                            }
                            """))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Missing or invalid access token")
    })
    @GetMapping("/risk/manufacturers")
    public ResponseEntity<ApiResponse<List<EntityRiskScore>>> manufacturerRisk() {
        return ResponseUtil.ok(riskIndexService.manufacturerRiskIndex());
    }

    @Operation(summary = "Product category risk index — most risky categories",
            description = "Ranked descending by riskScore using the Weighted Composite Risk Model. Authorization: any authenticated user.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Category risk scores, highest risk first",
                    content = @Content(mediaType = "application/json", examples = @ExampleObject(value = """
                            {
                              "success": true,
                              "message": "Request completed successfully",
                              "data": [
                                {
                                  "entityType": "CATEGORY",
                                  "entityId": null,
                                  "label": "Food",
                                  "riskScore": 71.10,
                                  "riskBand": "HIGH",
                                  "explainability": {
                                    "confidence": 0.85,
                                    "methodology": "Weighted Composite Risk Model",
                                    "inputDataPeriod": {"from": "2025-07-15", "to": "2026-01-15", "sampleSize": 128, "description": "Trailing 6 months (128 data points)"},
                                    "reasoning": ["128 violations across 6 months", "This category accounts for the largest share of critical violations"],
                                    "supportingMetrics": {"violationCount": 128, "criticalCount": 21},
                                    "assumptions": ["Assumes recent violation history is representative of ongoing risk"],
                                    "generatedAt": "2026-01-15T10:30:00Z"
                                  }
                                }
                              ],
                              "timestamp": "2026-01-15T10:30:00Z"
                            }
                            """))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Missing or invalid access token")
    })
    @GetMapping("/risk/categories")
    public ResponseEntity<ApiResponse<List<EntityRiskScore>>> categoryRisk() {
        return ResponseUtil.ok(riskIndexService.categoryRiskIndex());
    }

    @Operation(summary = "Region risk index",
            description = "Ranked descending by riskScore using the Weighted Composite Risk Model. Authorization: any authenticated user.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Region risk scores, highest risk first",
                    content = @Content(mediaType = "application/json", examples = @ExampleObject(value = """
                            {
                              "success": true,
                              "message": "Request completed successfully",
                              "data": [
                                {
                                  "entityType": "REGION",
                                  "entityId": null,
                                  "label": "New Delhi",
                                  "riskScore": 58.90,
                                  "riskBand": "MEDIUM",
                                  "explainability": {
                                    "confidence": 0.80,
                                    "methodology": "Weighted Composite Risk Model",
                                    "inputDataPeriod": {"from": "2025-07-15", "to": "2026-01-15", "sampleSize": 84, "description": "Trailing 6 months (84 data points)"},
                                    "reasoning": ["84 violations across 6 months"],
                                    "supportingMetrics": {"violationCount": 84},
                                    "assumptions": ["Assumes recent violation history is representative of ongoing risk"],
                                    "generatedAt": "2026-01-15T10:30:00Z"
                                  }
                                }
                              ],
                              "timestamp": "2026-01-15T10:30:00Z"
                            }
                            """))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Missing or invalid access token")
    })
    @GetMapping("/risk/regions")
    public ResponseEntity<ApiResponse<List<EntityRiskScore>>> regionRisk() {
        return ResponseUtil.ok(riskIndexService.regionRiskIndex());
    }

    @Operation(summary = "Inspector risk index",
            description = "Note: despite the name, this ranks inspectors by workload/anomaly signals for oversight " +
                    "purposes (e.g. unusually low violation-detection rate), not by wrongdoing. " +
                    "Authorization: any authenticated user.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Inspector risk scores, highest risk first",
                    content = @Content(mediaType = "application/json", examples = @ExampleObject(value = """
                            {
                              "success": true,
                              "message": "Request completed successfully",
                              "data": [
                                {
                                  "entityType": "INSPECTOR",
                                  "entityId": "b3f1c2d4-5e6f-4a7b-8c9d-0e1f2a3b4c5d",
                                  "label": "Priya Sharma",
                                  "riskScore": 34.20,
                                  "riskBand": "LOW",
                                  "explainability": {
                                    "confidence": 0.75,
                                    "methodology": "Weighted Composite Risk Model",
                                    "inputDataPeriod": {"from": "2025-07-15", "to": "2026-01-15", "sampleSize": 47, "description": "Trailing 6 months (47 data points)"},
                                    "reasoning": ["47 inspections conducted", "Violation detection rate in line with peer average"],
                                    "supportingMetrics": {"inspectionCount": 47, "violationDetectionRate": 0.26},
                                    "assumptions": ["Assumes recent activity is representative of ongoing performance"],
                                    "generatedAt": "2026-01-15T10:30:00Z"
                                  }
                                }
                              ],
                              "timestamp": "2026-01-15T10:30:00Z"
                            }
                            """))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Missing or invalid access token")
    })
    @GetMapping("/risk/inspectors")
    public ResponseEntity<ApiResponse<List<EntityRiskScore>>> inspectorRisk() {
        return ResponseUtil.ok(riskIndexService.inspectorRiskIndex());
    }
}
