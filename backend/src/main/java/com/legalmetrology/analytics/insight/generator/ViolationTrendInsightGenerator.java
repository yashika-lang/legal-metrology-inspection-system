package com.legalmetrology.analytics.insight.generator;

import com.legalmetrology.analytics.explainability.DataPeriod;
import com.legalmetrology.analytics.explainability.Explainability;
import com.legalmetrology.analytics.insight.Insight;
import com.legalmetrology.analytics.insight.InsightGenerator;
import com.legalmetrology.analytics.insight.InsightType;
import com.legalmetrology.analytics.model.MonthlyMetric;
import com.legalmetrology.analytics.model.RuleSummary;
import com.legalmetrology.analytics.service.AnalyticsQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * "MRP violations increased by 18% this month" — compares this month's
 * violation count against last month's, per rule and overall. A rule needs
 * a minimum sample in the prior month before a swing is reported, so a
 * change from 1 to 3 violations doesn't get reported as "+200%".
 */
@Component
@RequiredArgsConstructor
public class ViolationTrendInsightGenerator implements InsightGenerator {

    private static final double SIGNIFICANT_CHANGE_THRESHOLD = 0.15; // 15%
    private static final long MINIMUM_PRIOR_MONTH_SAMPLE = 3;
    private static final String METHODOLOGY = "Month-over-Month Delta Comparison";
    private static final int HISTORY_MONTHS = 3;

    private final AnalyticsQueryService analyticsQueryService;

    @Override
    public List<Insight> generate() {
        List<Insight> insights = new ArrayList<>();

        overallTrend().ifPresent(insights::add);

        for (RuleSummary rule : analyticsQueryService.activeRuleSummaries()) {
            perRuleTrend(rule).ifPresent(insights::add);
        }

        return insights;
    }

    private Optional<Insight> overallTrend() {
        return trendInsight(analyticsQueryService.monthlyViolationCounts(HISTORY_MONTHS), "Overall violations", null);
    }

    private Optional<Insight> perRuleTrend(RuleSummary rule) {
        return trendInsight(analyticsQueryService.monthlyViolationCountsForRule(rule.ruleCode(), HISTORY_MONTHS),
                rule.title() + " (" + rule.ruleCode() + ")", rule.ruleCode());
    }

    private Optional<Insight> trendInsight(List<MonthlyMetric> months, String label, String ruleCode) {
        Map<YearMonth, Long> byMonth = months.stream()
                .collect(java.util.stream.Collectors.toMap(MonthlyMetric::month, MonthlyMetric::count));

        YearMonth thisMonth = YearMonth.now();
        YearMonth lastMonth = thisMonth.minusMonths(1);
        long current = byMonth.getOrDefault(thisMonth, 0L);
        long previous = byMonth.getOrDefault(lastMonth, 0L);

        if (previous < MINIMUM_PRIOR_MONTH_SAMPLE) {
            return Optional.empty();
        }

        double changeRatio = (current - previous) / (double) previous;
        if (Math.abs(changeRatio) < SIGNIFICANT_CHANGE_THRESHOLD) {
            return Optional.empty();
        }

        String direction = changeRatio > 0 ? "increased" : "decreased";
        int changePercent = (int) Math.round(Math.abs(changeRatio) * 100);
        double confidence = Math.min(0.95, 0.5 + Math.min(previous, 20) / 40.0); // more prior-month data → more confidence

        String message = "%s %s by %d%% this month (%d vs %d last month).".formatted(label, direction, changePercent, current, previous);

        DataPeriod period = DataPeriod.ofMonths(lastMonth.atDay(1), thisMonth.atEndOfMonth(), months.size(), HISTORY_MONTHS);

        Explainability explainability = Explainability.of(confidence, METHODOLOGY, period,
                List.of(label + ": " + current + " violations this month vs " + previous + " last month",
                        "Change: " + direction + " by " + changePercent + "%"),
                Map.of("ruleCode", ruleCode == null ? "ALL" : ruleCode, "currentMonthCount", current,
                        "previousMonthCount", previous, "changePercent", changePercent),
                List.of("A rule needs at least " + MINIMUM_PRIOR_MONTH_SAMPLE + " violations in the prior month before a swing is reported",
                        "Compares only two adjacent months — does not smooth out single-month noise beyond the minimum-sample gate"));

        return Optional.of(Insight.of(InsightType.TREND, label + " trend", message, explainability));
    }
}
