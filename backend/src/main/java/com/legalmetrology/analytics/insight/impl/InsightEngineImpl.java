package com.legalmetrology.analytics.insight.impl;

import com.legalmetrology.analytics.insight.Insight;
import com.legalmetrology.analytics.insight.InsightEngine;
import com.legalmetrology.analytics.insight.InsightGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class InsightEngineImpl implements InsightEngine {

    private final List<InsightGenerator> generators;

    @Override
    public List<Insight> generateAll() {
        List<Insight> insights = generators.stream()
                .flatMap(generator -> safeGenerate(generator).stream())
                .sorted(Comparator.comparingDouble((Insight insight) -> insight.explainability().confidence()).reversed())
                .toList();

        log.info("Insight engine produced {} insights from {} generators", insights.size(), generators.size());
        return insights;
    }

    private List<Insight> safeGenerate(InsightGenerator generator) {
        try {
            return generator.generate();
        } catch (Exception ex) {
            log.error("Insight generator {} failed: {}", generator.getClass().getSimpleName(), ex.getMessage());
            return List.of();
        }
    }
}
