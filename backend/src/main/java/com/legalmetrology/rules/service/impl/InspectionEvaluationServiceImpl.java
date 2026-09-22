package com.legalmetrology.rules.service.impl;

import com.legalmetrology.exception.ResourceNotFoundException;
import com.legalmetrology.inspection.entity.Inspection;
import com.legalmetrology.inspection.entity.Violation;
import com.legalmetrology.inspection.repository.InspectionRepository;
import com.legalmetrology.rules.dto.EvaluationResponse;
import com.legalmetrology.rules.engine.RuleEngineService;
import com.legalmetrology.rules.mapper.ViolationMapper;
import com.legalmetrology.rules.model.ValidationResult;
import com.legalmetrology.rules.scoring.ComplianceScoreService;
import com.legalmetrology.rules.service.InspectionEvaluationService;
import com.legalmetrology.rules.violation.ViolationGenerationService;
import com.legalmetrology.common.enums.TraceStepName;
import com.legalmetrology.trace.service.DecisionTraceService;
import com.legalmetrology.trace.service.RecordStepCommand;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class InspectionEvaluationServiceImpl implements InspectionEvaluationService {

    private static final String MODULE = "InspectionEvaluationServiceImpl";

    private final RuleEngineService ruleEngineService;
    private final ViolationGenerationService violationGenerationService;
    private final ComplianceScoreService complianceScoreService;
    private final InspectionRepository inspectionRepository;
    private final ViolationMapper violationMapper;
    private final DecisionTraceService decisionTraceService;

    @Override
    @Transactional
    public EvaluationResponse evaluateAndScore(UUID inspectionId) {
        long stepStart = System.currentTimeMillis();
        List<ValidationResult> results = ruleEngineService.evaluate(inspectionId);
        decisionTraceService.recordStep(RecordStepCommand.success(inspectionId, TraceStepName.RULE_EVALUATION, MODULE,
                "Fused declarations for inspection " + inspectionId,
                results.size() + " rule(s) evaluated, " + results.stream().filter(r -> !r.passed()).count() + " failed",
                null, stepStart));

        stepStart = System.currentTimeMillis();
        List<Violation> violations = violationGenerationService.generate(inspectionId, results);
        decisionTraceService.recordStep(RecordStepCommand.success(inspectionId, TraceStepName.VIOLATIONS, MODULE,
                results.size() + " evaluated rule(s)", violations.size() + " violation(s) persisted", null, stepStart));

        stepStart = System.currentTimeMillis();
        BigDecimal score = complianceScoreService.computeAndPersist(inspectionId);

        Inspection inspection = inspectionRepository.findById(inspectionId)
                .orElseThrow(() -> ResourceNotFoundException.of("Inspection", inspectionId));

        decisionTraceService.recordStep(RecordStepCommand.success(inspectionId, TraceStepName.COMPLIANCE_SCORE, MODULE,
                violations.size() + " violation(s)", "Compliance score = " + score, null, stepStart));
        decisionTraceService.recordStep(RecordStepCommand.success(inspectionId, TraceStepName.RISK_SCORE, MODULE,
                "Compliance score = " + score, "Fraud risk band = " + inspection.getFraudRisk(), null, stepStart));

        return new EvaluationResponse(
                inspectionId,
                score,
                inspection.getFraudRisk(),
                violations.stream().map(violationMapper::toResponse).toList(),
                results.stream().map(violationMapper::toResponse).toList()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public List<ValidationResult> explain(UUID inspectionId) {
        return ruleEngineService.evaluate(inspectionId);
    }
}
