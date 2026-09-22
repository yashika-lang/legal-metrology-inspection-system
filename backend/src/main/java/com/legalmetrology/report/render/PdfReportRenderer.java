package com.legalmetrology.report.render;

import com.legalmetrology.report.model.ReportData;

/** Renders assembled {@link ReportData} as a PDF, via {@code PdfUtil}'s OpenPDF primitives. */
public interface PdfReportRenderer {

    byte[] render(ReportData data);
}
