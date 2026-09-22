package com.legalmetrology.report.render;

import com.legalmetrology.report.model.ReportData;

/** Renders assembled {@link ReportData} as a DOCX (Apache POI XWPF), the same structure and content as the PDF. */
public interface DocxReportRenderer {

    byte[] render(ReportData data);
}
