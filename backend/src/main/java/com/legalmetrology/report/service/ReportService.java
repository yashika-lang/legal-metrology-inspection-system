package com.legalmetrology.report.service;

import com.legalmetrology.report.dto.ComplianceReportResponse;
import com.legalmetrology.report.model.ReportData;

import java.util.List;
import java.util.UUID;

public interface ReportService {

    ComplianceReportResponse getById(UUID id);

    List<ComplianceReportResponse> listByInspection(UUID inspectionId);

    /** Assembles the inspection's data, renders PDF + DOCX, uploads both, and persists a new {@code ComplianceReport} row. */
    ComplianceReportResponse generate(UUID inspectionId, UUID generatedByUserId);

    /** The same assembled data the PDF/DOCX are rendered from, served directly — no file is stored for the JSON export. */
    ReportData getReportData(UUID inspectionId, UUID requestedByUserId);
}
