package com.legalmetrology.analytics.prediction;

import java.util.List;

/**
 * Statistical (z-score) anomaly detection over whatever time-series/dimensional
 * data this system currently tracks. The mechanism is generic — every method
 * here is "does this value deviate from its peers by more than N standard
 * deviations" applied to a different grouping — so extending it to a new
 * dimension (e.g. a dedicated fake-label-detection count, once that data
 * exists) is adding one more method in this shape, not new machinery.
 */
public interface AnomalyDetectionService {

    /** "Sudden spike in violations" — this month's count vs. the trailing baseline. */
    List<Anomaly> detectViolationSpikes();

    /** "Region behaving differently" — a region's average compliance score vs. the cross-region baseline. */
    List<Anomaly> detectRegionalAnomalies();
}
