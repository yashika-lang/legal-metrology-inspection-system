package com.legalmetrology.analytics.insight.generator;

import com.legalmetrology.analytics.explainability.DataPeriod;
import com.legalmetrology.analytics.explainability.Explainability;
import com.legalmetrology.analytics.insight.Insight;
import com.legalmetrology.analytics.insight.InsightGenerator;
import com.legalmetrology.analytics.insight.InsightType;
import com.legalmetrology.analytics.model.FieldCategoryCount;
import com.legalmetrology.analytics.service.AnalyticsQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * "Net quantity issues are most common in Food category" — for each
 * violated declaration field, finds the product category with the largest
 * share of that field's violations, and reports it when that category
 * clearly dominates (not just a narrow plurality among many categories).
 */
@Component
@RequiredArgsConstructor
public class CategoryPatternInsightGenerator implements InsightGenerator {

    private static final double DOMINANCE_THRESHOLD = 0.4; // the top category must hold >=40% of that field's violations
    private static final long MINIMUM_FIELD_SAMPLE = 3;
    private static final String METHODOLOGY = "Cross-Tabulation Share Analysis";

    private final AnalyticsQueryService analyticsQueryService;

    @Override
    public List<Insight> generate() {
        List<FieldCategoryCount> cells = analyticsQueryService.violationFieldCountsByCategory();

        Map<String, List<FieldCategoryCount>> byField = cells.stream()
                .collect(Collectors.groupingBy(FieldCategoryCount::field));

        return byField.entrySet().stream()
                .map(entry -> buildInsight(entry.getKey(), entry.getValue()))
                .filter(insight -> insight != null)
                .toList();
    }

    private Insight buildInsight(String field, List<FieldCategoryCount> cellsForField) {
        long totalForField = cellsForField.stream().mapToLong(FieldCategoryCount::count).sum();
        if (totalForField < MINIMUM_FIELD_SAMPLE) {
            return null;
        }

        FieldCategoryCount top = cellsForField.stream()
                .max((a, b) -> Long.compare(a.count(), b.count()))
                .orElse(null);
        if (top == null) {
            return null;
        }

        double share = top.count() / (double) totalForField;
        if (share < DOMINANCE_THRESHOLD) {
            return null;
        }

        int sharePercent = (int) Math.round(share * 100);
        double confidence = Math.min(0.95, 0.5 + share * 0.5);

        String message = humanizeField(field) + " issues are most common in the " + top.category() + " category ("
                + top.count() + " of " + totalForField + " such violations, " + sharePercent + "%).";

        DataPeriod period = DataPeriod.allTime(totalForField);

        Explainability explainability = Explainability.of(confidence, METHODOLOGY, period,
                List.of(top.category() + " accounts for " + top.count() + " of " + totalForField + " " + humanizeField(field) + " violations",
                        "Share of total: " + sharePercent + "%",
                        "Other categories: " + cellsForField.stream().filter(c -> !c.category().equals(top.category()))
                                .map(c -> c.category() + "=" + c.count()).toList()),
                Map.of("field", field, "topCategory", top.category(), "topCategoryCount", top.count(),
                        "totalForField", totalForField, "sharePercent", sharePercent),
                List.of("Reported only when one category holds at least " + (int) (DOMINANCE_THRESHOLD * 100) + "% of the field's total violations",
                        "Correlational — does not control for how many inspections were performed per category"));

        return Insight.of(InsightType.CATEGORY_PATTERN, field + " concentrated in " + top.category(), message, explainability);
    }

    private String humanizeField(String field) {
        return field.replace('_', ' ').toLowerCase();
    }
}
