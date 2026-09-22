package com.legalmetrology.ocr.entity;

import com.legalmetrology.common.entity.BaseEntity;
import com.legalmetrology.common.enums.DeclarationType;
import com.legalmetrology.common.enums.LabelSection;
import com.legalmetrology.inspection.entity.Inspection;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * The current best-understood value for one declaration type across an
 * entire inspection: every OCR run across every image (front/back/side)
 * consolidated down to the highest-confidence OCR value, then fused against
 * the Vision AI signal for the same declaration. Recomputed (upserted, not
 * appended) each time {@code DeclarationFusionService} runs — this is a
 * derived summary, not an audit log; the raw per-image, per-source
 * detections it was computed from remain untouched in {@code label_detections}.
 */
@Entity
@Table(name = "fused_declarations", uniqueConstraints = @UniqueConstraint(columnNames = {"inspection_id", "declaration_type"}))
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FusedDeclaration extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "inspection_id", nullable = false)
    private Inspection inspection;

    @Enumerated(EnumType.STRING)
    @Column(name = "declaration_type", nullable = false, length = 30)
    private DeclarationType declarationType;

    @Column(name = "fused_value", columnDefinition = "text")
    private String fusedValue;

    @Column(name = "fused_confidence", precision = 5, scale = 4)
    private BigDecimal fusedConfidence;

    @Column(name = "is_present", nullable = false)
    @Builder.Default
    private boolean present = false;

    @Column(name = "ocr_value", columnDefinition = "text")
    private String ocrValue;

    @Column(name = "ocr_confidence", precision = 5, scale = 4)
    private BigDecimal ocrConfidence;

    @Column(name = "ocr_source_detection_id")
    private UUID ocrSourceDetectionId;

    @Column(name = "vision_value", columnDefinition = "text")
    private String visionValue;

    @Column(name = "vision_confidence", precision = 5, scale = 4)
    private BigDecimal visionConfidence;

    @Column(name = "vision_source_detection_id")
    private UUID visionSourceDetectionId;

    /** Whether the OCR and Vision AI values roughly agree — false means both are kept but the fused confidence was penalized. */
    @Column(name = "agreement")
    private Boolean agreement;

    // Font-analysis metrics (Step 6), carried forward from whichever detection contributed
    // fusedValue — the single input rules.model.DeclarationSnapshot reads for
    // FONT_SIZE/READABILITY/PLACEMENT rule types.

    @Column(name = "font_size_estimate", precision = 6, scale = 2)
    private BigDecimal fontSizeEstimate;

    @Column(name = "readability_score", precision = 5, scale = 2)
    private BigDecimal readabilityScore;

    @Column(name = "contrast_score", precision = 5, scale = 2)
    private BigDecimal contrastScore;

    @Enumerated(EnumType.STRING)
    @Column(name = "label_section", length = 10)
    private LabelSection labelSection;

    /** JSON text {x,y,w,h}, carried forward from whichever detection contributed fusedValue — lets a violation for this declaration be highlighted on the image. */
    @Column(name = "bounding_box", columnDefinition = "text")
    private String boundingBox;

    /**
     * True when {@code fusedValue} came from a scanned product's verified
     * {@link com.legalmetrology.product.entity.ProductDeclaration} master
     * data rather than an OCR/Vision AI read of this inspection's own
     * photos — the barcode-first workflow's "we already know this for
     * certain" case. {@code ocrValue}/{@code visionValue} are left
     * populated as-is if either provider also ran, purely as
     * informational context; they never override a known product value.
     */
    @Column(name = "from_product_database", nullable = false)
    @Builder.Default
    private boolean fromProductDatabase = false;
}
