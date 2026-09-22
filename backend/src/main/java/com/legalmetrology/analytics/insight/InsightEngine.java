package com.legalmetrology.analytics.insight;

import java.util.List;

/** Runs every registered {@link InsightGenerator} and returns the combined, ranked result. */
public interface InsightEngine {

    List<Insight> generateAll();
}
