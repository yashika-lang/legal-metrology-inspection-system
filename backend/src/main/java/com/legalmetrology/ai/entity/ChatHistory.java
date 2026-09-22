package com.legalmetrology.ai.entity;

import com.legalmetrology.auth.entity.User;
import com.legalmetrology.common.entity.BaseEntity;
import com.legalmetrology.inspection.entity.Inspection;
import com.legalmetrology.report.entity.ComplianceReport;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * AI Copilot conversation log — one row per turn (one row for the officer's
 * question, one for the assistant's answer). {@code provider} records which
 * LLM answered (null on USER rows); {@code report} optionally links the
 * turn to a specific generated report it was discussing.
 */
@Entity
@Table(name = "chat_history")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatHistory extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "inspection_id")
    private Inspection inspection;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "report_id")
    private ComplianceReport report;

    @Column(nullable = false, length = 10)
    private String role; // USER | ASSISTANT

    @Column(nullable = false, columnDefinition = "text")
    private String message;

    /** Which LLM produced this turn — e.g. "gemini", "claude". Null for USER rows. */
    @Column(length = 30)
    private String provider;
}
