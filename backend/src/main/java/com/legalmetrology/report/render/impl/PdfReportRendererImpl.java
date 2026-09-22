package com.legalmetrology.report.render.impl;

import com.legalmetrology.report.model.ReportData;
import com.legalmetrology.report.render.PdfReportRenderer;
import com.legalmetrology.storage.dto.StorageBucket;
import com.legalmetrology.storage.service.StorageService;
import com.legalmetrology.utils.PdfUtil;
import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfPTable;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

/**
 * Composes a {@link ReportData} into a PDF using {@code PdfUtil}'s
 * primitives — this class only knows document structure, not OpenPDF
 * plumbing (that's what {@code PdfUtil} is for) and not where the data
 * came from (that's {@code ReportDataAssembler}'s job).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PdfReportRendererImpl implements PdfReportRenderer {

    private static final DateTimeFormatter TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm 'UTC'", Locale.ENGLISH)
            .withZone(java.time.ZoneOffset.UTC);

    private final StorageService storageService;

    @Override
    public byte[] render(ReportData data) {
        Document document = PdfUtil.newDocument();
        return PdfUtil.render(document, doc -> {
            doc.add(PdfUtil.title("Legal Metrology Compliance Report"));
            doc.add(keyValue("Report Number", data.reportNumber()));
            doc.add(keyValue("Generated", TIMESTAMP_FORMAT.format(data.generatedAt())));

            addExecutiveSummary(doc, data);
            addInspectionDetails(doc, data);
            addProductDetails(doc, data);
            addComplianceSummary(doc, data);
            addDeclarations(doc, data);
            addViolations(doc, data);
            addEvidence(doc, data);
            addDecisionTrace(doc, data);
            addRecommendations(doc, data);
            addLegalReferences(doc, data);
            addTimeline(doc, data);
        }).toByteArray();
    }

    private void addExecutiveSummary(Document doc, ReportData data) throws Exception {
        doc.add(PdfUtil.heading("Executive Summary"));
        doc.add(new Paragraph(data.executiveSummary() != null ? data.executiveSummary() : "(not available)", PdfUtil.BODY_FONT));
    }

    private void addInspectionDetails(Document doc, ReportData data) throws Exception {
        doc.add(PdfUtil.heading("Inspection Details"));
        var i = data.inspection();
        doc.add(PdfUtil.keyValueTable(List.of(
                new String[]{"Inspection ID", data.inspectionId().toString()},
                new String[]{"Inspector", nullSafe(i.inspectorName())},
                new String[]{"Status", i.status().name()},
                new String[]{"Region", nullSafe(i.region())},
                new String[]{"Location", i.locationLat() != null ? i.locationLat() + ", " + i.locationLng() : "N/A"},
                new String[]{"Started At", i.startedAt() != null ? TIMESTAMP_FORMAT.format(i.startedAt()) : "N/A"},
                new String[]{"Completed At", i.completedAt() != null ? TIMESTAMP_FORMAT.format(i.completedAt()) : "N/A"}
        )));
    }

    private void addProductDetails(Document doc, ReportData data) throws Exception {
        doc.add(PdfUtil.heading("Product Details"));
        var p = data.product();
        doc.add(PdfUtil.keyValueTable(List.of(
                new String[]{"Product", nullSafe(p.productName())},
                new String[]{"Category", nullSafe(p.categoryName())},
                new String[]{"Manufacturer", nullSafe(p.manufacturerName())},
                new String[]{"Manufacturer Address", nullSafe(p.manufacturerAddress())},
                new String[]{"Barcode", nullSafe(p.barcode())}
        )));
    }

    private void addComplianceSummary(Document doc, ReportData data) throws Exception {
        doc.add(PdfUtil.heading("Compliance Summary (Compliance Score & Risk Score)"));
        var c = data.compliance();
        doc.add(PdfUtil.keyValueTable(List.of(
                new String[]{"Compliance Score", c.complianceScore() != null ? c.complianceScore() + " / 100" : "N/A"},
                new String[]{"Fraud/Risk Band", c.fraudRisk() != null ? c.fraudRisk().name() : "N/A"},
                new String[]{"Total Violations", String.valueOf(c.violationCount())},
                new String[]{"Critical Violations", String.valueOf(c.criticalCount())}
        )));
    }

    private void addDeclarations(Document doc, ReportData data) throws Exception {
        doc.add(PdfUtil.heading("Detected Declarations"));
        if (data.declarations().isEmpty()) {
            doc.add(new Paragraph("No declarations detected.", PdfUtil.BODY_FONT));
            return;
        }
        PdfPTable table = new PdfPTable(4);
        table.setWidthPercentage(100);
        addHeaderRow(table, "Declaration", "Present", "Value", "Confidence");
        for (var d : data.declarations()) {
            table.addCell(cell(d.type().name()));
            table.addCell(cell(d.present() ? "Yes" : "No"));
            table.addCell(cell(nullSafe(d.value())));
            table.addCell(cell(d.confidence() != null ? d.confidence().toString() : "N/A"));
        }
        doc.add(table);
    }

    private void addViolations(Document doc, ReportData data) throws Exception {
        doc.add(PdfUtil.heading("Violations"));
        if (data.violations().isEmpty()) {
            doc.add(new Paragraph("No violations recorded — this inspection is fully compliant.", PdfUtil.BODY_FONT));
            return;
        }
        for (var v : data.violations()) {
            doc.add(new Paragraph("[" + v.severity() + "] " + v.ruleCode() + " — " + v.ruleTitle(), PdfUtil.HEADING_FONT));
            doc.add(PdfUtil.keyValueTable(List.of(
                    new String[]{"Field", v.field() != null ? v.field().name() : "N/A"},
                    new String[]{"Description", nullSafe(v.description())},
                    new String[]{"Actual Value", nullSafe(v.actualValue())},
                    new String[]{"Expected Value", nullSafe(v.expectedValue())},
                    new String[]{"Suggested Fix", nullSafe(v.suggestedFix())},
                    new String[]{"Legal Reference", nullSafe(v.legalReference())}
            )));
        }
    }

    private void addEvidence(Document doc, ReportData data) throws Exception {
        doc.add(PdfUtil.heading("Evidence & Annotated Images"));
        if (data.evidence().isEmpty()) {
            doc.add(new Paragraph("No evidence generated for this inspection.", PdfUtil.BODY_FONT));
            return;
        }
        for (var e : data.evidence()) {
            doc.add(new Paragraph("Evidence for " + e.ruleCode() + " [" + e.severity() + "]", PdfUtil.HEADING_FONT));
            doc.add(new Paragraph(nullSafe(e.reason()), PdfUtil.BODY_FONT));
            doc.add(new Paragraph("SHA-256: " + e.sha256Hash(), PdfUtil.BODY_FONT));
            embedAnnotatedImage(doc, e);
        }
    }

    private void embedAnnotatedImage(Document doc, ReportData.EvidenceEntry evidence) {
        if (evidence.annotatedImagePath() == null) {
            return;
        }
        try {
            byte[] bytes = storageService.download(StorageBucket.EVIDENCE, evidence.annotatedImagePath());
            com.lowagie.text.Image image = com.lowagie.text.Image.getInstance(bytes);
            image.scaleToFit(400, 300);
            image.setAlignment(Element.ALIGN_LEFT);
            doc.add(image);
        } catch (Exception ex) {
            log.warn("Could not embed annotated image for evidence at {} in PDF: {}", evidence.annotatedImagePath(), ex.getMessage());
        }
    }

    private void addDecisionTrace(Document doc, ReportData data) throws Exception {
        doc.add(PdfUtil.heading("Decision Trace (Inspection Timeline Through the AI Pipeline)"));
        if (data.decisionTrace().isEmpty()) {
            doc.add(new Paragraph("No decision trace recorded.", PdfUtil.BODY_FONT));
            return;
        }
        PdfPTable table = new PdfPTable(5);
        table.setWidthPercentage(100);
        addHeaderRow(table, "Step", "Module", "Status", "Confidence", "Duration (ms)");
        for (var t : data.decisionTrace()) {
            table.addCell(cell(t.stepName().name()));
            table.addCell(cell(t.module()));
            table.addCell(cell(t.status().name()));
            table.addCell(cell(t.confidence() != null ? t.confidence().toString() : "N/A"));
            table.addCell(cell(String.valueOf(t.executionTimeMs())));
        }
        doc.add(table);
    }

    private void addRecommendations(Document doc, ReportData data) throws Exception {
        doc.add(PdfUtil.heading("AI Recommendations & Manufacturer Guidance"));
        doc.add(new Paragraph(data.recommendations() != null ? data.recommendations() : "(not available)", PdfUtil.BODY_FONT));
    }

    private void addLegalReferences(Document doc, ReportData data) throws Exception {
        doc.add(PdfUtil.heading("Legal References (Appendix)"));
        if (data.legalReferences().isEmpty()) {
            doc.add(new Paragraph("No specific legal references cited — no violations found.", PdfUtil.BODY_FONT));
            return;
        }
        for (String reference : data.legalReferences()) {
            doc.add(new Paragraph("• " + reference, PdfUtil.BODY_FONT));
        }
    }

    private void addTimeline(Document doc, ReportData data) throws Exception {
        doc.add(PdfUtil.heading("Inspection Status Timeline"));
        if (data.timeline().isEmpty()) {
            doc.add(new Paragraph("No status transitions recorded.", PdfUtil.BODY_FONT));
            return;
        }
        for (var t : data.timeline()) {
            String from = t.fromStatus() != null ? t.fromStatus().name() : "(created)";
            doc.add(new Paragraph(TIMESTAMP_FORMAT.format(t.occurredAt()) + " — " + from + " -> " + t.toStatus()
                    + (t.changedByName() != null ? " by " + t.changedByName() : "")
                    + (t.note() != null ? " (" + t.note() + ")" : ""), PdfUtil.BODY_FONT));
        }
    }

    private void addHeaderRow(PdfPTable table, String... headers) {
        for (String header : headers) {
            var cell = new com.lowagie.text.pdf.PdfPCell(new Paragraph(header, PdfUtil.HEADING_FONT));
            table.addCell(cell);
        }
    }

    private com.lowagie.text.pdf.PdfPCell cell(String text) {
        return new com.lowagie.text.pdf.PdfPCell(new Paragraph(text, PdfUtil.BODY_FONT));
    }

    private Paragraph keyValue(String label, String value) {
        return new Paragraph(label + ": " + value, PdfUtil.BODY_FONT);
    }

    private String nullSafe(String value) {
        return value != null ? value : "N/A";
    }
}
