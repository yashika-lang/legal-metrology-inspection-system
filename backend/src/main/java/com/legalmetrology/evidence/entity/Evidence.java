package com.legalmetrology.evidence.entity;

import com.legalmetrology.common.entity.BaseEntity;
import com.legalmetrology.common.enums.Severity;
import com.legalmetrology.inspection.entity.Image;
import com.legalmetrology.inspection.entity.Inspection;
import com.legalmetrology.inspection.entity.Violation;
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
 * Part 1 — one legally-traceable evidence record for one {@link Violation}.
 * Carries every signal that fed the violation (OCR text, both raw
 * confidences, the fused confidence, expected vs. actual value, the rule
 * citation) plus pointers to the cropped and annotated images, so it can be
 * reproduced standalone in a report, on the dashboard, or in an audit
 * years later without re-deriving anything from the pipeline.
 * <p>
 * {@code sha256Hash} is computed once, at generation time, over the
 * evidence's own content (see {@code EvidenceHasher}) — a future digital
 * signature or tamper check is "does the stored hash still match the
 * stored content," which needs no architecture change to add later.
 * <p>
 * Once {@code isImmutable} is set (when the parent inspection is marked
 * {@code COMPLETED} — see {@code InspectionServiceImpl.updateStatus}),
 * the service layer refuses further regeneration for this violation; the
 * row itself has no DB-level write lock, since "immutable" here is a
 * business rule enforced above the persistence layer, not a physical one.
 */
@Entity
@Table(name = "evidence")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Evidence extends BaseEntity {

    @Column(name = "sha256_hash", nullable = false, length = 64)
    private String sha256Hash;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "inspection_id", nullable = false)
    private Inspection inspection;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "image_id")
    private Image image;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "violation_id", nullable = false)
    private Violation violation;

    @Column(name = "original_image_path", length = 500)
    private String originalImagePath;

    @Column(name = "annotated_image_path", length = 500)
    private String annotatedImagePath;

    /** JSON text {x,y,w,h}, image-relative — the exact region the snippet/annotation was cropped from. */
    @Column(name = "bounding_box", columnDefinition = "text")
    private String boundingBox;

    @Column(name = "ocr_text", columnDefinition = "text")
    private String ocrText;

    @Column(name = "normalized_value", columnDefinition = "text")
    private String normalizedValue;

    @Column(name = "vision_confidence", precision = 5, scale = 4)
    private BigDecimal visionConfidence;

    @Column(name = "ocr_confidence", precision = 5, scale = 4)
    private BigDecimal ocrConfidence;

    @Column(name = "fused_confidence", precision = 5, scale = 4)
    private BigDecimal fusedConfidence;

    @Column(name = "expected_value", columnDefinition = "text")
    private String expectedValue;

    @Column(name = "actual_value", columnDefinition = "text")
    private String actualValue;

    @Column(columnDefinition = "text")
    private String reason;

    @Column(name = "suggested_fix", columnDefinition = "text")
    private String suggestedFix;

    @Column(name = "legal_rule_reference", columnDefinition = "text")
    private String legalRuleReference;

    @Enumerated(EnumType.STRING)
    @Column(length = 10)
    private Severity severity;

    /** Free-form JSON bag for anything evidence-type-specific that doesn't warrant its own column (e.g. which declaration snippet type this is). */
    @Column(columnDefinition = "text")
    private String metadata;

    @Column(name = "is_immutable", nullable = false)
    @Builder.Default
    private boolean immutable = false;

    /** Reserved for a future digital signature over {@code sha256Hash}; unused until that capability is added. */
    @Column(columnDefinition = "text")
    private String signature;
}
