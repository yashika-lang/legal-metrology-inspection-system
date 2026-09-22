package com.legalmetrology.report.assembler;

import com.legalmetrology.report.model.ReportData;

import java.util.UUID;

/** Gathers every fact a Smart Report needs for one inspection — see {@link ReportData}'s Javadoc. */
public interface ReportDataAssembler {

    ReportData assemble(UUID inspectionId, UUID requestedByUserId);
}
