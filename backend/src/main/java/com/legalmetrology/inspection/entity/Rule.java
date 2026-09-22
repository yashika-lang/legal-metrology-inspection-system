package com.legalmetrology.inspection.entity;

import com.legalmetrology.common.entity.BaseEntity;
import com.legalmetrology.common.enums.Severity;
import com.legalmetrology.common.enums.ValidationType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

/**
 * Master data for the fully database-driven rule engine (Step 5). A rule is
 * never mutated in place once it may have been used by an evaluation —
 * {@code rules.service.RuleService} enforces an immutable version chain:
 * "updating" a rule inserts a new row with the same {@code ruleCode} and
 * {@code version + 1}, deactivating the previous version. This is what lets
 * a historical {@code Violation}'s FK to a specific {@code Rule} row stay
 * accurate forever, even after the rule text/expression later changes.
 */
@Entity
@Table(name = "rules")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Rule extends BaseEntity {

    @Column(name = "rule_code", nullable = false, length = 50)
    private String ruleCode;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(columnDefinition = "text")
    private String description;

    @Column(name = "legal_reference", length = 255)
    private String legalReference;

    @Column(length = 100)
    private String category;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    @Builder.Default
    private Severity severity = Severity.MINOR;

    /** Which generic validator strategy evaluates this rule — see {@code rules.engine.validator}. */
    @Enumerated(EnumType.STRING)
    @Column(name = "validation_type", length = 20)
    private ValidationType validationType;

    /** JSON parameter blob interpreted by the strategy named in {@link #validationType}, e.g. {@code {"field":"MRP","pattern":"..."}}. */
    @Column(name = "validation_expression", columnDefinition = "text")
    private String validationExpression;

    /** Whether this is a legally mandatory declaration (vs. a quality/best-practice check) — informs the compliance score's missing-fields term. */
    @Column(nullable = false)
    @Builder.Default
    private boolean mandatory = true;

    /** Template guidance shown to the officer/manufacturer when this rule fails. */
    @Column(columnDefinition = "text")
    private String suggestion;

    /** The statutory penalty clause for violating this declaration requirement, distinct from {@link #legalReference} (which cites the declaration requirement itself). */
    @Column(name = "penalty_reference", columnDefinition = "text")
    private String penaltyReference;

    @Column(nullable = false)
    @Builder.Default
    private int version = 1;

    @Column(name = "effective_date", nullable = false)
    @Builder.Default
    private LocalDate effectiveDate = LocalDate.now();

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private boolean active = true;
}
