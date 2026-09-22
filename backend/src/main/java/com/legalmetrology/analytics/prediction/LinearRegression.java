package com.legalmetrology.analytics.prediction;

/**
 * Ordinary least-squares fit of a straight line through (x, y) points —
 * the honest, explainable forecasting method this module uses. Not a
 * machine-learning model; a documented statistical technique whose
 * quality is reported alongside every forecast via {@link #rSquared()},
 * and whose predictions come with a proper prediction interval
 * ({@link #predictionInterval}), not just a point guess.
 */
public record LinearRegression(double slope, double intercept, double rSquared,
                                double standardErrorOfEstimate, double meanX, double sumSquaredDeviationsX, int sampleSize) {

    /**
     * Two-tailed 95% critical t-values by degrees of freedom (n-2), for the
     * small sample sizes this module's monthly time series actually have.
     * Falls back to the normal-distribution approximation (1.96) once df
     * is large enough that the t- and normal distributions have converged.
     */
    private static final double[] T_CRITICAL_95_BY_DF = {
            12.706, 4.303, 3.182, 2.776, 2.571, 2.447, 2.365, 2.306, 2.262, 2.228
    };

    public static LinearRegression fit(double[] xValues, double[] yValues) {
        int n = xValues.length;
        if (n < 2) {
            return new LinearRegression(0, n == 1 ? yValues[0] : 0, 0, 0, n == 1 ? xValues[0] : 0, 0, n);
        }

        double sumX = 0, sumY = 0, sumXY = 0, sumXX = 0;
        for (int i = 0; i < n; i++) {
            sumX += xValues[i];
            sumY += yValues[i];
            sumXY += xValues[i] * yValues[i];
            sumXX += xValues[i] * xValues[i];
        }
        double meanX = sumX / n;
        double meanY = sumY / n;

        double sumSquaredDeviationsX = sumXX - n * meanX * meanX;
        double slope = sumSquaredDeviationsX == 0 ? 0 : (sumXY - n * meanX * meanY) / sumSquaredDeviationsX;
        double intercept = meanY - slope * meanX;

        double ssTotal = 0, ssResidual = 0;
        for (int i = 0; i < n; i++) {
            double predicted = slope * xValues[i] + intercept;
            ssTotal += Math.pow(yValues[i] - meanY, 2);
            ssResidual += Math.pow(yValues[i] - predicted, 2);
        }
        double rSquared = ssTotal == 0 ? 1.0 : Math.max(0, 1 - (ssResidual / ssTotal));
        double standardError = n > 2 ? Math.sqrt(ssResidual / (n - 2)) : 0;

        return new LinearRegression(slope, intercept, rSquared, standardError, meanX, sumSquaredDeviationsX, n);
    }

    public double predict(double x) {
        return slope * x + intercept;
    }

    /**
     * 95% prediction interval for a new point at {@code x0} — wider than a
     * confidence interval for the mean, because it accounts for both the
     * line's uncertainty and the scatter of individual points around it.
     * Assumes residuals are approximately normally distributed with
     * constant variance (ordinary least-squares' standard assumptions).
     */
    public ConfidenceInterval predictionInterval(double x0) {
        double pointEstimate = predict(x0);
        if (sampleSize <= 2 || standardErrorOfEstimate == 0) {
            // Not enough degrees of freedom for a meaningful interval — collapse to the point estimate.
            java.math.BigDecimal point = java.math.BigDecimal.valueOf(Math.max(0, pointEstimate)).setScale(1, java.math.RoundingMode.HALF_UP);
            return new ConfidenceInterval(point, point, 0.95);
        }

        int degreesOfFreedom = sampleSize - 2;
        double tCritical = degreesOfFreedom <= T_CRITICAL_95_BY_DF.length
                ? T_CRITICAL_95_BY_DF[degreesOfFreedom - 1]
                : 1.96;

        double leverageTerm = 1 + (1.0 / sampleSize) + (sumSquaredDeviationsX == 0 ? 0 : Math.pow(x0 - meanX, 2) / sumSquaredDeviationsX);
        double marginOfError = tCritical * standardErrorOfEstimate * Math.sqrt(leverageTerm);

        java.math.BigDecimal lower = java.math.BigDecimal.valueOf(Math.max(0, pointEstimate - marginOfError)).setScale(1, java.math.RoundingMode.HALF_UP);
        java.math.BigDecimal upper = java.math.BigDecimal.valueOf(Math.max(0, pointEstimate + marginOfError)).setScale(1, java.math.RoundingMode.HALF_UP);
        return new ConfidenceInterval(lower, upper, 0.95);
    }

    public String equation() {
        return "y = %.3fx + %.3f".formatted(slope, intercept);
    }
}
