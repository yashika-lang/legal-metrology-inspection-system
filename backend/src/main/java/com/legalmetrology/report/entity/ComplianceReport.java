package com.legalmetrology.report.entity;

import com.legalmetrology.auth.entity.User;
import com.legalmetrology.common.entity.BaseEntity;
import com.legalmetrology.inspection.entity.Inspection;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * The record of one generated Smart Report — see
 * {@code report.assembler.ReportDataAssembler} for what goes into it and
 * {@code report.render} for how it becomes an actual PDF/DOCX file. JSON
 * export has no stored path: it's cheap to re-render from the same
 * assembled data on every request, so {@code ReportService} serves it
 * directly rather than persisting a third file per generation.
 */
@Entity
@Table(name = "compliance_reports")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ComplianceReport extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "inspection_id", nullable = false)
    private Inspection inspection;

    @Column(name = "report_number", nullable = false, unique = true, length = 50)
    private String reportNumber;

    @Column(name = "pdf_path", length = 500)
    private String pdfPath;

    @Column(name = "docx_path", length = 500)
    private String docxPath;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "generated_by")
    private User generatedBy;
}
