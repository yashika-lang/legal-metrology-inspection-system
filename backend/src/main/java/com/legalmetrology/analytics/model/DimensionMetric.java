package com.legalmetrology.analytics.model;

import java.math.BigDecimal;
import java.util.UUID;

/** One category/manufacturer/region/rule/inspector's aggregate — the building block of leaderboards, treemaps, and heatmap rows. */
public record DimensionMetric(UUID entityId, String label, long count, BigDecimal averageScore) {

    public static DimensionMetric of(UUID entityId, String label, long count) {
        return new DimensionMetric(entityId, label, count, null);
    }
}
