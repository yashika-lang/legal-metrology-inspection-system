package com.legalmetrology.analytics.prediction;

/** "Predict next month's violations / inspection workload" — see {@link LinearRegression} for the method. */
public interface TrendForecastService {

    Forecast forecastNextMonthViolations();

    Forecast forecastNextMonthInspectionWorkload();
}
