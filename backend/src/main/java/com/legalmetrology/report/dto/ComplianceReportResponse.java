package com.legalmetrology.report.dto;

import java.time.Instant;
import java.util.UUID;

public record ComplianceReportResponse(
        UUID id,
        UUID inspectionId,
        String reportNumber,
        String pdfUrl,
        String docxUrl,
        String generatedByName,
        Instant generatedAt
) {
}
