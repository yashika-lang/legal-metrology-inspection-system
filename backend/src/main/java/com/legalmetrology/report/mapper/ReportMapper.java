package com.legalmetrology.report.mapper;

import com.legalmetrology.report.dto.ComplianceReportResponse;
import com.legalmetrology.report.entity.ComplianceReport;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ReportMapper {

    /** Signed URLs are resolved by the service layer (via StorageService) since that requires an outbound call. */
    default ComplianceReportResponse toResponse(ComplianceReport report, String pdfUrl, String docxUrl) {
        if (report == null) {
            return null;
        }
        return new ComplianceReportResponse(
                report.getId(),
                report.getInspection().getId(),
                report.getReportNumber(),
                pdfUrl,
                docxUrl,
                report.getGeneratedBy() != null ? report.getGeneratedBy().getFullName() : null,
                report.getCreatedAt()
        );
    }
}
