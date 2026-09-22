package com.legalmetrology.analytics.model;

import java.time.LocalDate;

/** Rule master-data fields analytics needs (code/title/version/effective date) — never the rule's validation logic itself. */
public record RuleSummary(String ruleCode, String title, int version, LocalDate effectiveDate) {
}
