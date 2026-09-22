package com.legalmetrology.analytics.explainability;

import java.time.LocalDate;

/**
 * The historical window a prediction/anomaly/insight/risk score was
 * computed from. {@code from}/{@code to} are left null when the underlying
 * query doesn't cheaply know exact date bounds (e.g. an all-time
 * aggregate) — {@code description} always carries a human-readable
 * statement of the window either way, so this field is never silently
 * empty.
 */
public record DataPeriod(LocalDate from, LocalDate to, long sampleSize, String description) {

    public static DataPeriod allTime(long sampleSize) {
        return new DataPeriod(null, LocalDate.now(), sampleSize, "All available inspection history (" + sampleSize + " records)");
    }

    public static DataPeriod ofMonths(LocalDate from, LocalDate to, long sampleSize, int months) {
        return new DataPeriod(from, to, sampleSize, "Trailing " + months + " months (" + sampleSize + " data points)");
    }

    public static DataPeriod ofDays(LocalDate from, LocalDate to, long sampleSize, int days) {
        return new DataPeriod(from, to, sampleSize, "Trailing " + days + " days (" + sampleSize + " records)");
    }
}
