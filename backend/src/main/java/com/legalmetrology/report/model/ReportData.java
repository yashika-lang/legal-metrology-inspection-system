package com.legalmetrology.report.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.legalmetrology.common.enums.DeclarationType;
import com.legalmetrology.common.enums.FraudRisk;
import com.legalmetrology.common.enums.InspectionStatus;
import com.legalmetrology.common.enums.Severity;
import com.legalmetrology.common.enums.TraceStatus;
import com.legalmetrology.common.enums.TraceStepName;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Everything a Smart Report contains, assembled once by
 * {@code ReportDataAssembler} and consumed by both {@code PdfReportRenderer}
 * and {@code DocxReportRenderer} — and returned as-is for the JSON export,
 * so all three formats are guaranteed to show the same facts. Built
 * directly from the domain repositories (not from {@code CopilotContext},
 * which is intentionally scoped to "what the LLM is allowed to see") —
 * the two narrative fields are the only parts the AI Copilot contributes,
 * and both degrade to a deterministic fallback if the LLM call fails (see
 * {@code ReportDataAssemblerImpl}), because a report must never fail to
 * generate just because an LLM provider is unavailable.
 */
public record ReportData(
        UUID inspectionId,
        String reportNumber,
        Instant generatedAt,
        InspectionDetails inspection,
        ProductDetails product,
        ComplianceSummary compliance,
        List<DeclarationEntry> declarations,
        List<ViolationEntry> violations,
        List<EvidenceEntry> evidence,
        List<TraceEntry> decisionTrace,
        List<TimelineEntry> timeline,
        List<String> legalReferences,
        String executiveSummary,
        String recommendations
) {
    public record InspectionDetails(
            String inspectorName, InspectionStatus status, String region,
            Double locationLat, Double locationLng, Instant startedAt, Instant completedAt
    ) {
    }

    public record ProductDetails(
            String productName, String categoryName, String manufacturerName,
            String manufacturerAddress, String barcode
    ) {
    }

    public record ComplianceSummary(BigDecimal complianceScore, FraudRisk fraudRisk, int violationCount, int criticalCount) {
    }

    public record DeclarationEntry(
            DeclarationType type, boolean present, String value, BigDecimal confidence, String labelSection
    ) {
    }

    public record ViolationEntry(
            UUID violationId, String ruleCode, String ruleTitle, Severity severity, DeclarationType field,
            String description, String actualValue, String expectedValue, String suggestedFix, String legalReference
    ) {
    }

    /**
     * {@code annotatedImagePath} is the raw Supabase Storage path (not a
     * signed URL) — it exists only so {@code PdfReportRenderer}/
     * {@code DocxReportRenderer} can re-download the bytes to embed, and is
     * excluded from the JSON export: a report's public contract exposes
     * {@code annotatedImageUrl}, never internal storage layout.
     */
    public record EvidenceEntry(
            UUID evidenceId, UUID violationId, String ruleCode, Severity severity, String reason,
            String sha256Hash, String originalImageUrl, String annotatedImageUrl,
            @JsonIgnore String annotatedImagePath
    ) {
    }

    public record TraceEntry(
            TraceStepName stepName, String module, TraceStatus status, BigDecimal confidence,
            long executionTimeMs, Instant startedAt, String outputSummary
    ) {
    }

    public record TimelineEntry(
            InspectionStatus fromStatus, InspectionStatus toStatus, String changedByName, String note, Instant occurredAt
    ) {
    }
}
