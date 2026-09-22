package com.legalmetrology.analytics.model;

import java.math.BigDecimal;
import java.time.YearMonth;

/** One time-bucketed data point — the building block of every time-series/trend/forecast in this module. */
public record MonthlyMetric(YearMonth month, long count, BigDecimal averageValue) {

    public static MonthlyMetric countOnly(YearMonth month, long count) {
        return new MonthlyMetric(month, count, null);
    }
}
