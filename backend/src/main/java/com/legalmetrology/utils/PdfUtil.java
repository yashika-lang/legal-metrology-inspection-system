package com.legalmetrology.utils;

import com.legalmetrology.exception.BadRequestException;
import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;

import java.io.ByteArrayOutputStream;
import java.util.List;

/**
 * Thin, reusable wrapper around OpenPDF's document API. This does not know
 * anything about inspections or compliance reports — {@code report}
 * module's future PdfReportGenerator composes documents out of these
 * primitives once report-generation business logic is implemented.
 */
public final class PdfUtil {

    public static final Font TITLE_FONT = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18);
    public static final Font HEADING_FONT = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 13);
    public static final Font BODY_FONT = FontFactory.getFont(FontFactory.HELVETICA, 10);

    private PdfUtil() {
    }

    public static Document newDocument() {
        return new Document(PageSize.A4, 36, 36, 54, 54);
    }

    public static ByteArrayOutputStream render(Document document, DocumentWriter writer) {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        try {
            PdfWriter.getInstance(document, outputStream);
            document.open();
            writer.write(document);
        } catch (Exception ex) {
            throw new BadRequestException("Failed to generate PDF document: " + ex.getMessage());
        } finally {
            if (document.isOpen()) {
                document.close();
            }
        }
        return outputStream;
    }

    public static Paragraph title(String text) {
        Paragraph paragraph = new Paragraph(text, TITLE_FONT);
        paragraph.setAlignment(Element.ALIGN_CENTER);
        paragraph.setSpacingAfter(16f);
        return paragraph;
    }

    public static Paragraph heading(String text) {
        Paragraph paragraph = new Paragraph(text, HEADING_FONT);
        paragraph.setSpacingBefore(12f);
        paragraph.setSpacingAfter(6f);
        return paragraph;
    }

    public static PdfPTable keyValueTable(List<String[]> rows) {
        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(100);
        for (String[] row : rows) {
            table.addCell(labelCell(row[0]));
            table.addCell(valueCell(row.length > 1 ? row[1] : ""));
        }
        return table;
    }

    private static PdfPCell labelCell(String text) {
        PdfPCell cell = new PdfPCell(new Paragraph(text, HEADING_FONT));
        cell.setBorder(PdfPCell.NO_BORDER);
        cell.setPaddingBottom(4f);
        return cell;
    }

    private static PdfPCell valueCell(String text) {
        PdfPCell cell = new PdfPCell(new Paragraph(text, BODY_FONT));
        cell.setBorder(PdfPCell.NO_BORDER);
        cell.setPaddingBottom(4f);
        return cell;
    }

    @FunctionalInterface
    public interface DocumentWriter {
        void write(Document document) throws Exception;
    }
}
