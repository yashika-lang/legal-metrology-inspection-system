package com.legalmetrology.analytics.prediction;

import com.legalmetrology.analytics.explainability.Explainability;
import com.legalmetrology.analytics.model.MonthlyMetric;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;

/**
 * Never a bare number — every forecast this system produces exposes the
 * exact regression equation and R² it came from, so "the model predicted
 * 42" is always followed by "here's the line, here's the data, here's how
 * well it fits."
 */
public record Forecast(
        String metric,
        YearMonth targetPeriod,
        BigDecimal predictedValue,
        ConfidenceInterval confidenceInterval,
        List<MonthlyMetric> historicalDataUsed,
        String regressionEquation,
        Double rSquared,
        Explainability explainability
) {
}
