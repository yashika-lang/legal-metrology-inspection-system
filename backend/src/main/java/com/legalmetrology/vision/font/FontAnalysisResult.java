package com.legalmetrology.vision.font;

import com.legalmetrology.common.enums.FontIssue;

public record FontAnalysisResult(
        double fontSizeEstimate,
        double readabilityScore,
        double contrastScore,
        FontIssue issue
) {
}
