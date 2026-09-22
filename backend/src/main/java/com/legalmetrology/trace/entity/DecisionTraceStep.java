package com.legalmetrology.trace.entity;

import com.legalmetrology.common.entity.BaseEntity;
import com.legalmetrology.common.enums.TraceStatus;
import com.legalmetrology.common.enums.TraceStepName;
import com.legalmetrology.inspection.entity.Inspection;
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
import java.time.Instant;
import java.util.UUID;

/**
 * Part 3 — one recorded execution of one pipeline stage for one
 * inspection. Rows are append-only (never updated once written) so the
 * ordered set of rows for an inspection *is* its decision trace — a
 * complete, replayable, auditable record of exactly what happened, in
 * what order, how confidently, and how fast.
 * <p>
 * {@code referencedRuleId}/{@code referencedEvidenceId}/{@code referencedImageId}
 * are intentionally plain UUIDs with no JPA relation or DB foreign key —
 * this is a traceability log, not a normalized domain table, and a loose
 * reference must never block deleting or evolving the thing it points at.
 */
@Entity
@Table(name = "decision_trace_steps")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DecisionTraceStep extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "inspection_id", nullable = false)
    private Inspection inspection;

    @Enumerated(EnumType.STRING)
    @Column(name = "step_name", nullable = false, length = 30)
    private TraceStepName stepName;

    @Column(nullable = false, length = 100)
    private String module;

    @Column(name = "input_summary", columnDefinition = "text")
    private String inputSummary;

    @Column(name = "output_summary", columnDefinition = "text")
    private String outputSummary;

    @Column(precision = 5, scale = 4)
    private BigDecimal confidence;

    @Column(name = "execution_time_ms", nullable = false)
    @Builder.Default
    private long executionTimeMs = 0;

    @Column(name = "started_at", nullable = false)
    private Instant startedAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    private TraceStatus status;

    @Column(columnDefinition = "text")
    private String reason;

    @Column(name = "referenced_rule_id")
    private UUID referencedRuleId;

    @Column(name = "referenced_evidence_id")
    private UUID referencedEvidenceId;

    @Column(name = "referenced_image_id")
    private UUID referencedImageId;
}
