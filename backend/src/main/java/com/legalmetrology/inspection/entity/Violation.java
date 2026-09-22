package com.legalmetrology.inspection.entity;

import com.legalmetrology.common.entity.BaseEntity;
import com.legalmetrology.common.enums.DeclarationType;
import com.legalmetrology.common.enums.Severity;
import com.legalmetrology.common.enums.ViolationStatus;
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
 * One failed rule evaluation for one inspection, produced by
 * {@code rules.violation.ViolationGenerationService}. The FK to {@code rule}
 * points at the exact rule *version* that was evaluated — see {@link Rule}'s
 * Javadoc on why that row is never mutated after the fact.
 */
@Entity
@Table(name = "violations")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Violation extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "inspection_id", nullable = false)
    private Inspection inspection;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "rule_id", nullable = false)
    private Rule rule;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Severity severity;

    /** Which declaration this violation is about — the {@code field} the rule's validation_expression targeted. */
    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    private DeclarationType field;

    @Column(columnDefinition = "text")
    private String description;

    @Column(name = "actual_value", columnDefinition = "text")
    private String actualValue;

    @Column(name = "expected_value", columnDefinition = "text")
    private String expectedValue;

    @Column(name = "ai_explanation", columnDefinition = "text")
    private String aiExplanation;

    @Column(precision = 5, scale = 4)
    private BigDecimal confidence;

    /** JSON text {x,y,w,h} in image-relative coordinates, copied from the underlying detection when one exists. */
    @Column(name = "bounding_box", columnDefinition = "text")
    private String boundingBox;

    @Column(name = "suggested_fix", columnDefinition = "text")
    private String suggestedFix;

    /** Denormalized snapshot of {@code rule.legalReference} at evaluation time — see {@link Rule}'s Javadoc. */
    @Column(name = "legal_reference", columnDefinition = "text")
    private String legalReference;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    @Builder.Default
    private ViolationStatus status = ViolationStatus.OPEN;
}
