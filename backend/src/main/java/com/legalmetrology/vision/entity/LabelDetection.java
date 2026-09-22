package com.legalmetrology.vision.entity;

import com.legalmetrology.common.entity.BaseEntity;
import com.legalmetrology.common.enums.DeclarationType;
import com.legalmetrology.common.enums.DetectionSource;
import com.legalmetrology.common.enums.FontIssue;
import com.legalmetrology.common.enums.LabelSection;
import com.legalmetrology.inspection.entity.Image;
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
 * One declaration found (or conspicuously not found) for one image: the
 * unifying record that {@code ocr} (text-based extraction), {@code vision}
 * (Vision AI multimodal detection), and their combined font analysis all
 * write into, and that {@code rules} reads to evaluate compliance.
 * <p>
 * A single declaration type can have two rows for the same image — one from
 * OCR text parsing, one from Vision AI — since they're independent signals;
 * the rule engine and compliance scorer reconcile them (see
 * {@code rules.engine.RuleEngineService}).
 */
@Entity
@Table(name = "label_detections")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LabelDetection extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "image_id", nullable = false)
    private Image image;

    @Enumerated(EnumType.STRING)
    @Column(name = "declaration_type", nullable = false, length = 30)
    private DeclarationType declarationType;

    @Column(name = "detected_value", columnDefinition = "text")
    private String detectedValue;

    @Column(precision = 5, scale = 4)
    private BigDecimal confidence;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    private DetectionSource source;

    /** Which concrete provider produced this row — e.g. "google-vision", "tesseract". Null for VISION_AI-sourced rows (see the provider field on the fused view instead). */
    @Column(name = "ocr_provider", length = 30)
    private String ocrProvider;

    /** ISO-ish language code ("en","hi","mr","ta","gu") detected for this text span. Stored as a plain string (not an enum type) so this module has no dependency on {@code ocr}'s LanguageCode. */
    @Column(length = 10)
    private String language;

    /** JSON text {@code {"x":.., "y":.., "w":.., "h":..}} in image-relative coordinates, or null when not visually localized. */
    @Column(name = "bounding_box", columnDefinition = "text")
    private String boundingBox;

    @Column(name = "is_present", nullable = false)
    @Builder.Default
    private boolean present = false;

    @Enumerated(EnumType.STRING)
    @Column(name = "label_section", length = 10)
    private LabelSection labelSection;

    @Column(name = "font_size_estimate", precision = 6, scale = 2)
    private BigDecimal fontSizeEstimate;

    @Column(name = "readability_score", precision = 5, scale = 2)
    private BigDecimal readabilityScore;

    @Column(name = "contrast_score", precision = 5, scale = 2)
    private BigDecimal contrastScore;

    @Enumerated(EnumType.STRING)
    @Column(name = "font_issue", length = 15)
    private FontIssue fontIssue;
}
