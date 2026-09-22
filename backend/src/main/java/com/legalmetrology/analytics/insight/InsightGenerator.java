package com.legalmetrology.analytics.insight;

import java.util.List;

/** Strategy interface — one pattern detector. Adding a new kind of insight means implementing this, nothing else changes (Open/Closed). */
public interface InsightGenerator {

    List<Insight> generate();
}
