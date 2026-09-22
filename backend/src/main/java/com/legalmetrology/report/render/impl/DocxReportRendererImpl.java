package com.legalmetrology.report.render.impl;

import com.legalmetrology.exception.BadRequestException;
import com.legalmetrology.report.model.ReportData;
import com.legalmetrology.report.render.DocxReportRenderer;
import com.legalmetrology.storage.dto.StorageBucket;
import com.legalmetrology.storage.service.StorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.util.Units;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

/** DOCX counterpart of {@code PdfReportRendererImpl} — same sections, same data, Apache POI XWPF instead of OpenPDF. */
@Slf4j
@Component
@RequiredArgsConstructor
public class DocxReportRendererImpl implements DocxReportRenderer {

    private static final DateTimeFormatter TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm 'UTC'", Locale.ENGLISH)
            .withZone(java.time.ZoneOffset.UTC);

    private final StorageService storageService;

    @Override
    public byte[] render(ReportData data) {
        try (XWPFDocument document = new XWPFDocument()) {
            title(document, "Legal Metrology Compliance Report");
            paragraph(document, "Report Number: " + data.reportNumber());
            paragraph(document, "Generated: " + TIMESTAMP_FORMAT.format(data.generatedAt()));

            heading(document, "Executive Summary");
            paragraph(document, nullSafe(data.executiveSummary()));

            heading(document, "Inspection Details");
            var i = data.inspection();
            keyValueTable(document, List.of(
                    new String[]{"Inspector", nullSafe(i.inspectorName())},
                    new String[]{"Status", i.status().name()},
                    new String[]{"Region", nullSafe(i.region())},
                    new String[]{"Started At", i.startedAt() != null ? TIMESTAMP_FORMAT.format(i.startedAt()) : "N/A"},
                    new String[]{"Completed At", i.completedAt() != null ? TIMESTAMP_FORMAT.format(i.completedAt()) : "N/A"}
            ));

            heading(document, "Product Details");
            var p = data.product();
            keyValueTable(document, List.of(
                    new String[]{"Product", nullSafe(p.productName())},
                    new String[]{"Category", nullSafe(p.categoryName())},
                    new String[]{"Manufacturer", nullSafe(p.manufacturerName())},
                    new String[]{"Manufacturer Address", nullSafe(p.manufacturerAddress())},
                    new String[]{"Barcode", nullSafe(p.barcode())}
            ));

            heading(document, "Compliance Summary (Compliance Score & Risk Score)");
            var c = data.compliance();
            keyValueTable(document, List.of(
                    new String[]{"Compliance Score", c.complianceScore() != null ? c.complianceScore() + " / 100" : "N/A"},
                    new String[]{"Fraud/Risk Band", c.fraudRisk() != null ? c.fraudRisk().name() : "N/A"},
                    new String[]{"Total Violations", String.valueOf(c.violationCount())},
                    new String[]{"Critical Violations", String.valueOf(c.criticalCount())}
            ));

            heading(document, "Detected Declarations");
            if (data.declarations().isEmpty()) {
                paragraph(document, "No declarations detected.");
            } else {
                table(document, List.of("Declaration", "Present", "Value", "Confidence"),
                        data.declarations().stream()
                                .map(d -> new String[]{d.type().name(), d.present() ? "Yes" : "No", nullSafe(d.value()),
                                        d.confidence() != null ? d.confidence().toString() : "N/A"})
                                .toList());
            }

            heading(document, "Violations");
            if (data.violations().isEmpty()) {
                paragraph(document, "No violations recorded — this inspection is fully compliant.");
            } else {
                for (var v : data.violations()) {
                    boldParagraph(document, "[" + v.severity() + "] " + v.ruleCode() + " — " + v.ruleTitle());
                    keyValueTable(document, List.of(
                            new String[]{"Field", v.field() != null ? v.field().name() : "N/A"},
                            new String[]{"Description", nullSafe(v.description())},
                            new String[]{"Actual Value", nullSafe(v.actualValue())},
                            new String[]{"Expected Value", nullSafe(v.expectedValue())},
                            new String[]{"Suggested Fix", nullSafe(v.suggestedFix())},
                            new String[]{"Legal Reference", nullSafe(v.legalReference())}
                    ));
                }
            }

            heading(document, "Evidence & Annotated Images");
            if (data.evidence().isEmpty()) {
                paragraph(document, "No evidence generated for this inspection.");
            } else {
                for (var e : data.evidence()) {
                    boldParagraph(document, "Evidence for " + e.ruleCode() + " [" + e.severity() + "]");
                    paragraph(document, nullSafe(e.reason()));
                    paragraph(document, "SHA-256: " + e.sha256Hash());
                    embedAnnotatedImage(document, e);
                }
            }

            heading(document, "Decision Trace (Inspection Timeline Through the AI Pipeline)");
            if (data.decisionTrace().isEmpty()) {
                paragraph(document, "No decision trace recorded.");
            } else {
                table(document, List.of("Step", "Module", "Status", "Confidence", "Duration (ms)"),
                        data.decisionTrace().stream()
                                .map(t -> new String[]{t.stepName().name(), t.module(), t.status().name(),
                                        t.confidence() != null ? t.confidence().toString() : "N/A", String.valueOf(t.executionTimeMs())})
                                .toList());
            }

            heading(document, "AI Recommendations & Manufacturer Guidance");
            paragraph(document, nullSafe(data.recommendations()));

            heading(document, "Legal References (Appendix)");
            if (data.legalReferences().isEmpty()) {
                paragraph(document, "No specific legal references cited — no violations found.");
            } else {
                data.legalReferences().forEach(ref -> paragraph(document, "• " + ref));
            }

            heading(document, "Inspection Status Timeline");
            if (data.timeline().isEmpty()) {
                paragraph(document, "No status transitions recorded.");
            } else {
                for (var t : data.timeline()) {
                    String from = t.fromStatus() != null ? t.fromStatus().name() : "(created)";
                    paragraph(document, TIMESTAMP_FORMAT.format(t.occurredAt()) + " — " + from + " -> " + t.toStatus()
                            + (t.changedByName() != null ? " by " + t.changedByName() : "")
                            + (t.note() != null ? " (" + t.note() + ")" : ""));
                }
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            document.write(out);
            return out.toByteArray();
        } catch (Exception ex) {
            throw new BadRequestException("Failed to generate DOCX report: " + ex.getMessage());
        }
    }

    private void embedAnnotatedImage(XWPFDocument document, ReportData.EvidenceEntry evidence) {
        if (evidence.annotatedImagePath() == null) {
            return;
        }
        try {
            byte[] bytes = storageService.download(StorageBucket.EVIDENCE, evidence.annotatedImagePath());
            XWPFParagraph paragraph = document.createParagraph();
            XWPFRun run = paragraph.createRun();
            run.addPicture(new ByteArrayInputStream(bytes), org.apache.poi.xwpf.usermodel.Document.PICTURE_TYPE_PNG,
                    "annotated.png", Units.toEMU(400), Units.toEMU(300));
        } catch (Exception ex) {
            log.warn("Could not embed annotated image for evidence at {} in DOCX: {}", evidence.annotatedImagePath(), ex.getMessage());
        }
    }

    private void title(XWPFDocument document, String text) {
        XWPFParagraph paragraph = document.createParagraph();
        paragraph.setAlignment(org.apache.poi.xwpf.usermodel.ParagraphAlignment.CENTER);
        XWPFRun run = paragraph.createRun();
        run.setText(text);
        run.setBold(true);
        run.setFontSize(18);
    }

    private void heading(XWPFDocument document, String text) {
        XWPFParagraph paragraph = document.createParagraph();
        paragraph.setSpacingBefore(200);
        XWPFRun run = paragraph.createRun();
        run.setText(text);
        run.setBold(true);
        run.setFontSize(13);
    }

    private void boldParagraph(XWPFDocument document, String text) {
        XWPFParagraph paragraph = document.createParagraph();
        XWPFRun run = paragraph.createRun();
        run.setText(text);
        run.setBold(true);
    }

    private void paragraph(XWPFDocument document, String text) {
        XWPFParagraph paragraph = document.createParagraph();
        XWPFRun run = paragraph.createRun();
        run.setText(text);
        run.setFontSize(10);
    }

    private void keyValueTable(XWPFDocument document, List<String[]> rows) {
        XWPFTable table = document.createTable(rows.size(), 2);
        for (int r = 0; r < rows.size(); r++) {
            table.getRow(r).getCell(0).setText(rows.get(r)[0]);
            table.getRow(r).getCell(1).setText(rows.get(r).length > 1 ? rows.get(r)[1] : "");
        }
    }

    private void table(XWPFDocument document, List<String> headers, List<String[]> rows) {
        XWPFTable table = document.createTable(rows.size() + 1, headers.size());
        for (int c = 0; c < headers.size(); c++) {
            table.getRow(0).getCell(c).setText(headers.get(c));
        }
        for (int r = 0; r < rows.size(); r++) {
            String[] row = rows.get(r);
            for (int c = 0; c < row.length; c++) {
                table.getRow(r + 1).getCell(c).setText(row[c]);
            }
        }
    }

    private String nullSafe(String value) {
        return value != null ? value : "N/A";
    }
}
