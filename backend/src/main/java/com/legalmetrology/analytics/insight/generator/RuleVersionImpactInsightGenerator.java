package com.legalmetrology.analytics.insight.generator;

import com.legalmetrology.analytics.explainability.DataPeriod;
import com.legalmetrology.analytics.explainability.Explainability;
import com.legalmetrology.analytics.insight.Insight;
import com.legalmetrology.analytics.insight.InsightGenerator;
import com.legalmetrology.analytics.insight.InsightType;
import com.legalmetrology.analytics.model.RuleSummary;
import com.legalmetrology.analytics.service.AnalyticsQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * "Packaging quality improved after Rule Version 3" — for every active rule
 * that has been revised at least once, compares the overall average
 * compliance score in the 30 days before vs. after its current version's
 * effective date. Deliberately compares *overall* compliance in the
 * window, not just violations of that one rule — a rule change (e.g. a
 * stricter font-size threshold) can shift behavior on the label as a
 * whole, and overall score is what officers and manufacturers actually see.
 */
@Component
@RequiredArgsConstructor
public class RuleVersionImpactInsightGenerator implements InsightGenerator {

    private static final int WINDOW_DAYS = 30;
    private static final BigDecimal NOTABLE_CHANGE_THRESHOLD = BigDecimal.valueOf(5); // compliance-score points
    private static final String METHODOLOGY = "Before/After Window Comparison";

    private final AnalyticsQueryService analyticsQueryService;

    @Override
    public List<Insight> generate() {
        List<Insight> insights = new ArrayList<>();
        for (RuleSummary rule : analyticsQueryService.activeRuleSummariesWithMultipleVersions()) {
            buildInsight(rule).ifPresent(insights::add);
        }
        return insights;
    }

    private Optional<Insight> buildInsight(RuleSummary rule) {
        Instant effectiveInstant = rule.effectiveDate().atStartOfDay(ZoneOffset.UTC).toInstant();
        Instant windowStart = effectiveInstant.minus(Duration.ofDays(WINDOW_DAYS));
        Instant windowEnd = effectiveInstant.plus(Duration.ofDays(WINDOW_DAYS));

        BigDecimal before = analyticsQueryService.averageComplianceScoreInWindow(windowStart, effectiveInstant);
        BigDecimal after = analyticsQueryService.averageComplianceScoreInWindow(effectiveInstant, windowEnd);

        if (before == null || after == null) {
            return Optional.empty();
        }

        BigDecimal delta = after.subtract(before);
        if (delta.abs().compareTo(NOTABLE_CHANGE_THRESHOLD) < 0) {
            return Optional.empty();
        }

        String direction = delta.signum() > 0 ? "improved" : "declined";
        double confidence = Math.min(0.9, 0.5 + delta.abs().doubleValue() / 50.0);

        String message = "Packaging quality %s after %s (rule %s) reached version %d on %s — average compliance score moved from %.1f to %.1f."
                .formatted(direction, rule.title(), rule.ruleCode(), rule.version(), rule.effectiveDate(), before, after);

        DataPeriod period = new DataPeriod(
                LocalDate.ofInstant(windowStart, ZoneOffset.UTC), LocalDate.ofInstant(windowEnd, ZoneOffset.UTC), 0,
                WINDOW_DAYS + " days before and after " + rule.effectiveDate());

        Explainability explainability = Explainability.of(confidence, METHODOLOGY, period,
                List.of("Average compliance score before: " + before, "Average compliance score after: " + after,
                        "Change: " + (delta.signum() > 0 ? "+" : "") + delta + " points"),
                Map.of("ruleCode", rule.ruleCode(), "version", rule.version(), "effectiveDate", rule.effectiveDate().toString(),
                        "avgScoreBefore", before, "avgScoreAfter", after, "delta", delta),
                List.of("Correlational, not a controlled before/after experiment — other changes in the same window could contribute",
                        "Compares overall inspection compliance, not only inspections that triggered this specific rule",
                        "Reported only when the change is at least " + NOTABLE_CHANGE_THRESHOLD + " compliance-score points"));

        return Optional.of(Insight.of(InsightType.RULE_VERSION_IMPACT, rule.ruleCode() + " v" + rule.version() + " impact", message, explainability));
    }
}
