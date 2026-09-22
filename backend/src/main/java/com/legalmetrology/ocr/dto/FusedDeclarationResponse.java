package com.legalmetrology.ocr.dto;

import com.legalmetrology.common.enums.DeclarationType;

import java.math.BigDecimal;
import java.util.UUID;

public record FusedDeclarationResponse(
        UUID id,
        UUID inspectionId,
        DeclarationType declarationType,
        String fusedValue,
        BigDecimal fusedConfidence,
        boolean present,
        String ocrValue,
        BigDecimal ocrConfidence,
        String visionValue,
        BigDecimal visionConfidence,
        Boolean agreement,
        boolean fromProductDatabase
) {
}
