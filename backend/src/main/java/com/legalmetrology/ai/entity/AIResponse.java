package com.legalmetrology.ai.entity;

import com.legalmetrology.common.entity.BaseEntity;
import com.legalmetrology.common.enums.AiRefType;
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

import java.util.UUID;

/**
 * Full audit trail of every LLM call (prompt, response, model, token/latency
 * cost) — required for cost tracking and reproducibility once the {@code ai}
 * module (OCR correction, vision explanations, compliance assistant, etc.)
 * is implemented. Data model only in this phase.
 */
@Entity
@Table(name = "ai_responses")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AIResponse extends BaseEntity {

    @Enumerated(EnumType.STRING)
    @Column(name = "ref_type", nullable = false, length = 20)
    private AiRefType refType;

    @Column(name = "ref_id", nullable = false)
    private UUID refId;

    @Column(nullable = false, columnDefinition = "text")
    private String prompt;

    @Column(nullable = false, columnDefinition = "text")
    private String response;

    @Column(nullable = false, length = 100)
    private String model;

    @Column(name = "token_count")
    private Integer tokenCount;

    @Column(name = "latency_ms")
    private Integer latencyMs;
}
