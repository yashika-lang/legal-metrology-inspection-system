package com.legalmetrology.rules.scoring.impl;

import com.legalmetrology.common.enums.DeclarationType;
import com.legalmetrology.common.enums.FraudRisk;
import com.legalmetrology.common.settings.repository.SettingsRepository;
import com.legalmetrology.exception.ResourceNotFoundException;
import com.legalmetrology.inspection.entity.Image;
import com.legalmetrology.inspection.entity.Inspection;
import com.legalmetrology.inspection.entity.Rule;
import com.legalmetrology.inspection.entity.Violation;
import com.legalmetrology.inspection.repository.ImageRepository;
import com.legalmetrology.inspection.repository.InspectionRepository;
import com.legalmetrology.inspection.repository.ViolationRepository;
import com.legalmetrology.ocr.entity.FusedDeclaration;
import com.legalmetrology.ocr.fusion.DeclarationFusionService;
import com.legalmetrology.rules.cache.RuleCacheService;
import com.legalmetrology.rules.engine.RuleExpressionParser;
import com.legalmetrology.rules.scoring.ComplianceScoreService;
import com.legalmetrology.rules.scoring.ScoringWeights;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Formula (every weight configurable — see {@link ScoringWeights}):
 * <pre>
 * score = 100
 *       + Σ severityWeight(violation.severity)                    [negative]
 *       − imageQualityWeight     × (1 − avgImageQuality/100)
 *       − ocrConfidenceWeight    × (1 − avgOcrConfidence)
 *       − fontReadabilityWeight  × (1 − avgReadability/100)
 *       − missingFieldsWeight    × countOfMissingMandatoryFields
 * </pre>
 * clamped to [0, 100]. The severity term is the primary driver (any rule
 * failure, missing-field or otherwise, is penalized there via whatever
 * severity that rule carries); the other four terms are smaller holistic
 * adjustments for evidence quality that isn't necessarily tied to any one
 * rule.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ComplianceScoreServiceImpl implements ComplianceScoreService {

    private static final Map<String, BigDecimal> DEFAULTS = Map.ofEntries(
            Map.entry("scoring.severity.critical.weight", BigDecimal.valueOf(-25)),
            Map.entry("scoring.severity.major.weight", BigDecimal.valueOf(-10)),
            Map.entry("scoring.severity.minor.weight", BigDecimal.valueOf(-3)),
            Map.entry("scoring.imageQuality.weight", BigDecimal.valueOf(5)),
            Map.entry("scoring.ocrConfidence.weight", BigDecimal.valueOf(5)),
            Map.entry("scoring.fontReadability.weight", BigDecimal.valueOf(5)),
            Map.entry("scoring.missingFields.weight", BigDecimal.valueOf(2))
    );

    private final SettingsRepository settingsRepository;
    private final InspectionRepository inspectionRepository;
    private final ImageRepository imageRepository;
    private final ViolationRepository violationRepository;
    private final DeclarationFusionService declarationFusionService;
    private final RuleCacheService ruleCacheService;
    private final RuleExpressionParser ruleExpressionParser;

    @Override
    @Transactional
    public BigDecimal computeAndPersist(UUID inspectionId) {
        Inspection inspection = inspectionRepository.findById(inspectionId)
                .orElseThrow(() -> ResourceNotFoundException.of("Inspection", inspectionId));

        ScoringWeights weights = getCurrentWeights();

        List<Violation> violations = violationRepository.findByInspectionId(inspectionId);
        List<Image> images = imageRepository.findByInspectionId(inspectionId);
        List<FusedDeclaration> fusedDeclarations = declarationFusionService.getFusedDeclarations(inspectionId);

        BigDecimal severityPenalty = violations.stream()
                .map(v -> severityWeight(v.getSeverity().name(), weights))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal imageQualityPenalty = weights.imageQualityWeight()
                .multiply(BigDecimal.ONE.subtract(averageImageQuality(images)));
        BigDecimal ocrConfidencePenalty = weights.ocrConfidenceWeight()
                .multiply(BigDecimal.ONE.subtract(averageOcrConfidence(fusedDeclarations)));
        BigDecimal fontReadabilityPenalty = weights.fontReadabilityWeight()
                .multiply(BigDecimal.ONE.subtract(averageReadability(fusedDeclarations)));
        BigDecimal missingFieldsPenalty = weights.missingFieldsWeight()
                .multiply(BigDecimal.valueOf(countMissingMandatoryFields(fusedDeclarations)));

        BigDecimal score = BigDecimal.valueOf(100)
                .add(severityPenalty)
                .subtract(imageQualityPenalty)
                .subtract(ocrConfidencePenalty)
                .subtract(fontReadabilityPenalty)
                .subtract(missingFieldsPenalty)
                .setScale(2, RoundingMode.HALF_UP);

        score = score.max(BigDecimal.ZERO).min(BigDecimal.valueOf(100));

        inspection.setComplianceScore(score);
        inspection.setFraudRisk(deriveFraudRisk(score));
        inspectionRepository.save(inspection);

        log.info("Compliance score computed for inspection {}: {} (fraudRisk={}, violations={})",
                inspectionId, score, inspection.getFraudRisk(), violations.size());
        return score;
    }

    @Override
    public ScoringWeights getCurrentWeights() {
        return new ScoringWeights(
                weight("scoring.severity.critical.weight"),
                weight("scoring.severity.major.weight"),
                weight("scoring.severity.minor.weight"),
                weight("scoring.imageQuality.weight"),
                weight("scoring.ocrConfidence.weight"),
                weight("scoring.fontReadability.weight"),
                weight("scoring.missingFields.weight")
        );
    }

    private BigDecimal weight(String key) {
        return settingsRepository.findByKey(key)
                .map(setting -> new BigDecimal(setting.getValue().trim()))
                .orElseGet(() -> DEFAULTS.get(key));
    }

    private BigDecimal severityWeight(String severity, ScoringWeights weights) {
        return switch (severity) {
            case "CRITICAL" -> weights.criticalWeight();
            case "MAJOR" -> weights.majorWeight();
            default -> weights.minorWeight();
        };
    }

    private BigDecimal averageImageQuality(List<Image> images) {
        List<BigDecimal> scores = images.stream().map(Image::getQualityScore).filter(s -> s != null).toList();
        if (scores.isEmpty()) {
            return BigDecimal.ONE; // no data — assume acceptable rather than penalizing for a step that hasn't run
        }
        BigDecimal sum = scores.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        return sum.divide(BigDecimal.valueOf(scores.size()), 4, RoundingMode.HALF_UP)
                .divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);
    }

    private BigDecimal averageOcrConfidence(List<FusedDeclaration> fusedDeclarations) {
        List<BigDecimal> confidences = fusedDeclarations.stream()
                .filter(FusedDeclaration::isPresent)
                .map(FusedDeclaration::getFusedConfidence)
                .filter(c -> c != null)
                .toList();
        if (confidences.isEmpty()) {
            return BigDecimal.ONE;
        }
        BigDecimal sum = confidences.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        return sum.divide(BigDecimal.valueOf(confidences.size()), 4, RoundingMode.HALF_UP);
    }

    private BigDecimal averageReadability(List<FusedDeclaration> fusedDeclarations) {
        List<BigDecimal> scores = fusedDeclarations.stream()
                .filter(FusedDeclaration::isPresent)
                .map(FusedDeclaration::getReadabilityScore)
                .filter(s -> s != null)
                .toList();
        if (scores.isEmpty()) {
            return BigDecimal.ONE;
        }
        BigDecimal sum = scores.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        return sum.divide(BigDecimal.valueOf(scores.size()), 4, RoundingMode.HALF_UP)
                .divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);
    }

    /** Distinct declaration types targeted by an active, mandatory rule whose fused declaration isn't present. */
    private long countMissingMandatoryFields(List<FusedDeclaration> fusedDeclarations) {
        Map<DeclarationType, Boolean> presenceByType = fusedDeclarations.stream()
                .collect(Collectors.toMap(FusedDeclaration::getDeclarationType, FusedDeclaration::isPresent, (a, b) -> a || b));

        Set<DeclarationType> mandatoryFields = ruleCacheService.getActiveRules().stream()
                .filter(Rule::isMandatory)
                .map(this::tryGetField)
                .filter(f -> f != null)
                .collect(Collectors.toSet());

        return mandatoryFields.stream()
                .filter(field -> !presenceByType.getOrDefault(field, false))
                .count();
    }

    private DeclarationType tryGetField(Rule rule) {
        try {
            return ruleExpressionParser.getField(rule);
        } catch (Exception ex) {
            return null; // CUSTOM rules may not target a single field — excluded from this count, not an error
        }
    }

    private FraudRisk deriveFraudRisk(BigDecimal score) {
        double value = score.doubleValue();
        if (value < 40) return FraudRisk.HIGH;
        if (value < 70) return FraudRisk.MEDIUM;
        return FraudRisk.LOW;
    }
}
