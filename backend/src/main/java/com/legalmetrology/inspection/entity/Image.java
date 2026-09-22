package com.legalmetrology.inspection.entity;

import com.legalmetrology.common.entity.BaseEntity;
import com.legalmetrology.common.enums.ImageRecommendedAction;
import com.legalmetrology.common.enums.ImageType;
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
 * A single photo captured for an inspection. qualityScore/qualityWarnings/
 * recommendedAction are populated synchronously on upload by
 * {@code vision.quality.ImageQualityService} (Step 1 of the AI pipeline);
 * perceptualHash is computed on upload for future duplicate detection.
 */
@Entity
@Table(name = "images")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Image extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "inspection_id", nullable = false)
    private Inspection inspection;

    @Column(name = "storage_path", nullable = false, length = 500)
    private String storagePath;

    @Enumerated(EnumType.STRING)
    @Column(name = "image_type", nullable = false, length = 10)
    @Builder.Default
    private ImageType imageType = ImageType.OTHER;

    @Column(name = "quality_score", precision = 5, scale = 2)
    private BigDecimal qualityScore;

    /** Comma-separated {@code vision.quality.QualityWarning} names, e.g. "BLUR,LOW_CONTRAST". */
    @Column(name = "quality_warnings", columnDefinition = "text")
    private String qualityWarnings;

    @Enumerated(EnumType.STRING)
    @Column(name = "recommended_action", length = 30)
    private ImageRecommendedAction recommendedAction;

    @Column(name = "perceptual_hash", length = 32)
    private String perceptualHash;
}
