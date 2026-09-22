package com.legalmetrology.analytics.prediction.impl;

import com.legalmetrology.analytics.explainability.DataPeriod;
import com.legalmetrology.analytics.explainability.Explainability;
import com.legalmetrology.analytics.model.MonthlyMetric;
import com.legalmetrology.analytics.prediction.Forecast;
import com.legalmetrology.analytics.prediction.LinearRegression;
import com.legalmetrology.analytics.prediction.TrendForecastService;
import com.legalmetrology.analytics.service.AnalyticsQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class TrendForecastServiceImpl implements TrendForecastService {

    private static final int HISTORY_MONTHS = 6;
    private static final String METHODOLOGY = "Ordinary Least-Squares Linear Regression";

    private final AnalyticsQueryService analyticsQueryService;

    @Override
    public Forecast forecastNextMonthViolations() {
        return forecast("violations", analyticsQueryService.monthlyViolationCounts(HISTORY_MONTHS));
    }

    @Override
    public Forecast forecastNextMonthInspectionWorkload() {
        return forecast("inspection workload", analyticsQueryService.monthlyInspectionCounts(HISTORY_MONTHS));
    }

    private Forecast forecast(String metric, List<MonthlyMetric> history) {
        YearMonth nextMonth = YearMonth.now().plusMonths(1);

        if (history.size() < 2) {
            DataPeriod period = DataPeriod.ofMonths(null, null, history.size(), HISTORY_MONTHS);
            Explainability explainability = Explainability.of(0.0, METHODOLOGY, period,
                    List.of("Fewer than 2 months of historical data are available for " + metric),
                    Map.of("monthsAvailable", history.size()),
                    List.of("A trend line needs at least 2 data points; this forecast cannot be trusted yet."));
            return new Forecast(metric, nextMonth, BigDecimal.ZERO,
                    new com.legalmetrology.analytics.prediction.ConfidenceInterval(BigDecimal.ZERO, BigDecimal.ZERO, 0.0),
                    history, null, null, explainability);
        }

        double[] x = new double[history.size()];
        double[] y = new double[history.size()];
        for (int i = 0; i < history.size(); i++) {
            x[i] = i;
            y[i] = history.get(i).count();
        }

        LinearRegression regression = LinearRegression.fit(x, y);
        double nextX = history.size();
        double predictedRaw = regression.predict(nextX);
        BigDecimal predicted = BigDecimal.valueOf(Math.max(0, predictedRaw)).setScale(1, RoundingMode.HALF_UP);
        var confidenceInterval = regression.predictionInterval(nextX);

        // A short series can produce a deceptively perfect R² — scale confidence down for thin samples.
        double sampleAdequacy = Math.min(1.0, history.size() / 4.0);
        double confidence = Math.round(regression.rSquared() * sampleAdequacy * 100) / 100.0;

        String trendDirection = regression.slope() > 0.05 ? "upward" : regression.slope() < -0.05 ? "downward" : "flat";

        DataPeriod period = DataPeriod.ofMonths(
                history.get(0).month().atDay(1), history.get(history.size() - 1).month().atEndOfMonth(),
                history.size(), HISTORY_MONTHS);

        List<String> reasoning = new ArrayList<>();
        reasoning.add("Trend is " + trendDirection + " (slope = %.2f %s/month)".formatted(regression.slope(), metric));
        reasoning.add("Regression fit quality R² = %.2f over %d months of history".formatted(regression.rSquared(), history.size()));
        reasoning.add("Historical " + metric + " counts: " + history.stream().map(m -> m.month() + "=" + m.count()).toList());

        Explainability explainability = Explainability.of(confidence, METHODOLOGY, period, reasoning,
                Map.of("slope", regression.slope(), "intercept", regression.intercept(), "rSquared", regression.rSquared(),
                        "sampleSize", history.size(), "standardErrorOfEstimate", regression.standardErrorOfEstimate()),
                List.of("Assumes the linear trend observed over the last " + history.size() + " months continues unchanged next month",
                        "Assumes residuals are approximately normally distributed with constant variance (standard OLS assumptions)",
                        "Does not account for seasonality, one-off events, or policy changes"));

        return new Forecast(metric, nextMonth, predicted, confidenceInterval, history,
                regression.equation(), Math.round(regression.rSquared() * 100) / 100.0, explainability);
    }
}
