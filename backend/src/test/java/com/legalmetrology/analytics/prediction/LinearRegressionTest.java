package com.legalmetrology.analytics.prediction;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class LinearRegressionTest {

    @Test
    void fitsAPerfectLineWithRSquaredOfOne() {
        double[] x = {0, 1, 2, 3, 4};
        double[] y = {10, 12, 14, 16, 18}; // y = 2x + 10, no noise

        LinearRegression regression = LinearRegression.fit(x, y);

        assertThat(regression.slope()).isCloseTo(2.0, org.assertj.core.data.Offset.offset(1e-9));
        assertThat(regression.intercept()).isCloseTo(10.0, org.assertj.core.data.Offset.offset(1e-9));
        assertThat(regression.rSquared()).isCloseTo(1.0, org.assertj.core.data.Offset.offset(1e-9));
        assertThat(regression.predict(5)).isCloseTo(20.0, org.assertj.core.data.Offset.offset(1e-9));
    }

    @Test
    void aPerfectFitProducesAnEssentiallyZeroWidthPredictionInterval() {
        double[] x = {0, 1, 2, 3, 4, 5};
        double[] y = {10, 12, 14, 16, 18, 20};

        LinearRegression regression = LinearRegression.fit(x, y);
        ConfidenceInterval interval = regression.predictionInterval(6);

        assertThat(interval.lowerBound()).isEqualByComparingTo(interval.upperBound());
    }

    @Test
    void noisyDataProducesALowerRSquaredAndAWiderPredictionInterval() {
        double[] x = {0, 1, 2, 3, 4, 5};
        double[] noisyY = {10, 15, 11, 19, 14, 24}; // same overall upward trend, but scattered

        LinearRegression noisy = LinearRegression.fit(x, noisyY);
        LinearRegression clean = LinearRegression.fit(x, new double[]{10, 12, 14, 16, 18, 20});

        assertThat(noisy.rSquared()).isLessThan(clean.rSquared());

        ConfidenceInterval noisyInterval = noisy.predictionInterval(6);
        ConfidenceInterval cleanInterval = clean.predictionInterval(6);
        double noisyWidth = noisyInterval.upperBound().subtract(noisyInterval.lowerBound()).doubleValue();
        double cleanWidth = cleanInterval.upperBound().subtract(cleanInterval.lowerBound()).doubleValue();
        assertThat(noisyWidth).isGreaterThan(cleanWidth);
    }

    @Test
    void aFlatSeriesHasApproximatelyZeroSlope() {
        double[] x = {0, 1, 2, 3};
        double[] y = {5, 5, 5, 5};

        LinearRegression regression = LinearRegression.fit(x, y);

        assertThat(regression.slope()).isCloseTo(0.0, org.assertj.core.data.Offset.offset(1e-9));
        assertThat(regression.predict(10)).isCloseTo(5.0, org.assertj.core.data.Offset.offset(1e-6));
    }

    @Test
    void theEquationStringMatchesTheFittedCoefficients() {
        LinearRegression regression = LinearRegression.fit(new double[]{0, 1, 2}, new double[]{1, 3, 5});

        assertThat(regression.equation()).isEqualTo("y = 2.000x + 1.000");
    }

    @Test
    void aSingleDataPointCannotFitALineAndReturnsAFlatEstimate() {
        LinearRegression regression = LinearRegression.fit(new double[]{0}, new double[]{7});

        assertThat(regression.slope()).isZero();
        assertThat(regression.predict(5)).isEqualTo(7.0);
    }
}
