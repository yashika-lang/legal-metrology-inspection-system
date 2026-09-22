package com.legalmetrology.inspection.entity;

import com.legalmetrology.common.entity.BaseEntity;
import com.legalmetrology.common.enums.OcrProvider;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * One OCR run against one image. Every run inserts a new row (never
 * updates an existing one) so this table doubles as the OCR audit
 * history — {@code ocr.service.OcrService} always appends, and
 * {@code OCRResultRepository.findByImageIdOrderByCreatedAtDesc} exposes
 * the run-to-run history for comparison/debugging.
 */
@Entity
@Table(name = "ocr_results")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OCRResult extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "image_id", nullable = false)
    private Image image;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OcrProvider provider;

    @Column(name = "raw_text", columnDefinition = "text")
    private String rawText;

    @Column(name = "corrected_text", columnDefinition = "text")
    private String correctedText;

    /** Raw OCR confidence (average across extracted words), independent of how much the AI correction step changed the text. */
    @Column(precision = 5, scale = 4)
    private BigDecimal confidence;

    /** The correction step's own confidence in its rewrite — see {@code ocr.correction.OcrCorrectionService}. */
    @Column(name = "correction_confidence", precision = 5, scale = 4)
    private BigDecimal correctionConfidence;

    /** Paragraph/line/word hierarchy with coordinates, serialized as JSON text — see {@code ocr.model.OcrExtractionResult}. */
    @Column(name = "structured_hierarchy", columnDefinition = "text")
    private String structuredHierarchy;

    /** ISO-ish language code ("en","hi","mr","ta","gu") the provider detected for this image, when it could tell. */
    @Column(name = "detected_language", length = 10)
    private String detectedLanguage;
}
