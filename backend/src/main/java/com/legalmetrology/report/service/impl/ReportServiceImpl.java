package com.legalmetrology.report.service.impl;

import com.legalmetrology.auth.entity.User;
import com.legalmetrology.auth.repository.UserRepository;
import com.legalmetrology.exception.ResourceNotFoundException;
import com.legalmetrology.inspection.entity.Inspection;
import com.legalmetrology.inspection.repository.InspectionRepository;
import com.legalmetrology.report.assembler.ReportDataAssembler;
import com.legalmetrology.report.dto.ComplianceReportResponse;
import com.legalmetrology.report.entity.ComplianceReport;
import com.legalmetrology.report.mapper.ReportMapper;
import com.legalmetrology.report.model.ReportData;
import com.legalmetrology.report.render.DocxReportRenderer;
import com.legalmetrology.report.render.PdfReportRenderer;
import com.legalmetrology.report.repository.ComplianceReportRepository;
import com.legalmetrology.report.service.ReportService;
import com.legalmetrology.storage.dto.StorageBucket;
import com.legalmetrology.storage.service.StorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReportServiceImpl implements ReportService {

    private static final DateTimeFormatter REPORT_NUMBER_DATE = DateTimeFormatter.ofPattern("yyyyMMdd").withZone(ZoneOffset.UTC);

    private final ComplianceReportRepository complianceReportRepository;
    private final ReportMapper reportMapper;
    private final InspectionRepository inspectionRepository;
    private final UserRepository userRepository;
    private final ReportDataAssembler reportDataAssembler;
    private final PdfReportRenderer pdfReportRenderer;
    private final DocxReportRenderer docxReportRenderer;
    private final StorageService storageService;

    @Override
    @Transactional(readOnly = true)
    public ComplianceReportResponse getById(UUID id) {
        return toResponse(complianceReportRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("ComplianceReport", id)));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ComplianceReportResponse> listByInspection(UUID inspectionId) {
        return complianceReportRepository.findByInspectionId(inspectionId).stream().map(this::toResponse).toList();
    }

    @Override
    @Transactional
    public ComplianceReportResponse generate(UUID inspectionId, UUID generatedByUserId) {
        Inspection inspection = inspectionRepository.findById(inspectionId)
                .orElseThrow(() -> ResourceNotFoundException.of("Inspection", inspectionId));
        User generatedBy = userRepository.findById(generatedByUserId)
                .orElseThrow(() -> ResourceNotFoundException.of("User", generatedByUserId));

        String reportNumber = generateReportNumber();
        ReportData data = reportDataAssembler.assemble(inspectionId, generatedByUserId);
        data = withReportNumber(data, reportNumber);

        byte[] pdfBytes = pdfReportRenderer.render(data);
        byte[] docxBytes = docxReportRenderer.render(data);

        String pdfPath = "reports/" + inspectionId + "/" + reportNumber + ".pdf";
        String docxPath = "reports/" + inspectionId + "/" + reportNumber + ".docx";
        storageService.upload(StorageBucket.REPORTS, pdfPath, pdfBytes, "application/pdf");
        storageService.upload(StorageBucket.REPORTS, docxPath,
                docxBytes, "application/vnd.openxmlformats-officedocument.wordprocessingml.document");

        ComplianceReport report = ComplianceReport.builder()
                .inspection(inspection)
                .reportNumber(reportNumber)
                .pdfPath(pdfPath)
                .docxPath(docxPath)
                .generatedBy(generatedBy)
                .build();
        report = complianceReportRepository.save(report);

        log.info("Generated compliance report {} for inspection {}", reportNumber, inspectionId);
        return toResponse(report);
    }

    @Override
    @Transactional
    public ReportData getReportData(UUID inspectionId, UUID requestedByUserId) {
        return withReportNumber(reportDataAssembler.assemble(inspectionId, requestedByUserId), "(preview — not persisted)");
    }

    private ReportData withReportNumber(ReportData data, String reportNumber) {
        return new ReportData(data.inspectionId(), reportNumber, data.generatedAt(), data.inspection(), data.product(),
                data.compliance(), data.declarations(), data.violations(), data.evidence(), data.decisionTrace(),
                data.timeline(), data.legalReferences(), data.executiveSummary(), data.recommendations());
    }

    /** {@code CR-<UTC yyyyMMdd>-<8 random hex chars>} — collision-checked against the unique DB constraint rather than trusted blindly. */
    private String generateReportNumber() {
        String prefix = "CR-" + REPORT_NUMBER_DATE.format(Instant.now()) + "-";
        String candidate;
        do {
            candidate = prefix + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        } while (complianceReportRepository.existsByReportNumber(candidate));
        return candidate;
    }

    private ComplianceReportResponse toResponse(ComplianceReport report) {
        String pdfUrl = report.getPdfPath() != null ? storageService.generateSignedUrl(StorageBucket.REPORTS, report.getPdfPath()) : null;
        String docxUrl = report.getDocxPath() != null ? storageService.generateSignedUrl(StorageBucket.REPORTS, report.getDocxPath()) : null;
        return reportMapper.toResponse(report, pdfUrl, docxUrl);
    }
}
