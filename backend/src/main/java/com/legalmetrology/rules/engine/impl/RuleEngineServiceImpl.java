package com.legalmetrology.rules.engine.impl;

import com.legalmetrology.common.enums.DeclarationType;
import com.legalmetrology.inspection.entity.Rule;
import com.legalmetrology.ocr.entity.FusedDeclaration;
import com.legalmetrology.ocr.fusion.DeclarationFusionService;
import com.legalmetrology.rules.cache.RuleCacheService;
import com.legalmetrology.vision.BoundingBoxJson;
import com.legalmetrology.rules.engine.RuleEngineService;
import com.legalmetrology.rules.engine.ValidationContext;
import com.legalmetrology.rules.engine.ValidatorFactory;
import com.legalmetrology.rules.model.DeclarationSnapshot;
import com.legalmetrology.rules.model.ValidationResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * The only class in {@code rules} that knows {@link FusedDeclaration}
 * exists — every other class in this module works exclusively with
 * {@link DeclarationSnapshot}, which carries no information about which
 * OCR/Vision provider produced the underlying data.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RuleEngineServiceImpl implements RuleEngineService {

    private final DeclarationFusionService declarationFusionService;
    private final RuleCacheService ruleCacheService;
    private final ValidatorFactory validatorFactory;

    @Override
    @Transactional(readOnly = true)
    public List<ValidationResult> evaluate(UUID inspectionId) {
        ValidationContext context = buildContext(inspectionId);
        List<Rule> activeRules = ruleCacheService.getActiveRules();

        List<ValidationResult> results = activeRules.stream()
                .map(rule -> evaluateOne(rule, context))
                .toList();

        long failed = results.stream().filter(r -> !r.passed()).count();
        log.info("Rule engine evaluated inspection {}: {} rules, {} failed", inspectionId, results.size(), failed);
        return results;
    }

    private ValidationResult evaluateOne(Rule rule, ValidationContext context) {
        try {
            return validatorFactory.resolve(rule.getValidationType()).validate(rule, context);
        } catch (Exception ex) {
            log.error("Rule {} failed to evaluate due to a configuration error: {}", rule.getRuleCode(), ex.getMessage());
            return ValidationResult.fail(rule, null, null, null, 0.0, null,
                    "This rule could not be evaluated due to a configuration error: " + ex.getMessage());
        }
    }

    private ValidationContext buildContext(UUID inspectionId) {
        List<FusedDeclaration> fusedDeclarations = declarationFusionService.getFusedDeclarations(inspectionId);

        Map<DeclarationType, DeclarationSnapshot> snapshots = fusedDeclarations.stream()
                .collect(Collectors.toMap(FusedDeclaration::getDeclarationType, this::toSnapshot));

        return new ValidationContext(snapshots);
    }

    private DeclarationSnapshot toSnapshot(FusedDeclaration fused) {
        return new DeclarationSnapshot(
                fused.getDeclarationType(),
                fused.isPresent(),
                fused.getFusedValue(),
                fused.getFusedConfidence() != null ? fused.getFusedConfidence().doubleValue() : 0.0,
                BoundingBoxJson.fromJson(fused.getBoundingBox()),
                fused.getFontSizeEstimate(),
                fused.getReadabilityScore(),
                fused.getContrastScore(),
                fused.getLabelSection()
        );
    }
}
