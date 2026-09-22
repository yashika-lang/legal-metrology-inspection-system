package com.legalmetrology.ocr.fusion;

import com.legalmetrology.ocr.entity.FusedDeclaration;

import java.util.List;
import java.util.UUID;

/**
 * Steps: "Support multiple package images... merge OCR results
 * intelligently, avoid duplicate declarations, prefer higher confidence"
 * and "Implement confidence fusion... store both... generate a fused
 * confidence score."
 * <p>
 * Consolidates every {@code label_detections} row across every image of an
 * inspection (front/back/side, OCR and Vision AI alike) into one row per
 * declaration type: the best OCR candidate, the best Vision AI candidate,
 * and a fused value/confidence combining them. Neither raw candidate is
 * ever discarded — both remain on the resulting {@link FusedDeclaration} row.
 */
public interface DeclarationFusionService {

    List<FusedDeclaration> fuseInspection(UUID inspectionId);

    List<FusedDeclaration> getFusedDeclarations(UUID inspectionId);
}
