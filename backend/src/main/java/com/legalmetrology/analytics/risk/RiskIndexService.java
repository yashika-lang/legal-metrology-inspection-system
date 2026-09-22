package com.legalmetrology.analytics.risk;

import java.util.List;

/**
 * Step: "Every manufacturer, every category, every state, every inspector
 * should receive a dynamic risk score." One formula, four dimensions —
 * see the implementation for the weighted composite and
 * docs on how its weights are configured (via {@code settings}, same
 * pattern as {@code rules.scoring.ComplianceScoreService}).
 */
public interface RiskIndexService {

    List<EntityRiskScore> manufacturerRiskIndex();

    List<EntityRiskScore> categoryRiskIndex();

    List<EntityRiskScore> regionRiskIndex();

    List<EntityRiskScore> inspectorRiskIndex();
}
