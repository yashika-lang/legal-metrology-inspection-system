package com.legalmetrology.rules.violation.impl;

import com.legalmetrology.exception.ResourceNotFoundException;
import com.legalmetrology.inspection.entity.Inspection;
import com.legalmetrology.inspection.entity.Violation;
import com.legalmetrology.inspection.repository.InspectionRepository;
import com.legalmetrology.inspection.repository.ViolationRepository;
import com.legalmetrology.rules.model.ValidationResult;
import com.legalmetrology.rules.violation.ViolationGenerationService;
import com.legalmetrology.vision.BoundingBoxJson;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ViolationGenerationServiceImpl implements ViolationGenerationService {

    private final InspectionRepository inspectionRepository;
    private final ViolationRepository violationRepository;

    @Override
    @Transactional
    public List<Violation> generate(UUID inspectionId, List<ValidationResult> evaluationResults) {
        Inspection inspection = inspectionRepository.findById(inspectionId)
                .orElseThrow(() -> ResourceNotFoundException.of("Inspection", inspectionId));

        // Violations are the current rule-engine outcome, not an audit log — re-evaluation
        // (e.g. after a retake or added image) replaces them rather than accumulating duplicates.
        violationRepository.deleteByInspectionId(inspectionId);

        List<Violation> violations = evaluationResults.stream()
                .filter(result -> !result.passed())
                .map(result -> buildViolation(inspection, result))
                .map(violationRepository::save)
                .toList();

        log.info("Generated {} violations for inspection {}", violations.size(), inspectionId);
        return violations;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Violation> getViolations(UUID inspectionId) {
        return violationRepository.findByInspectionId(inspectionId);
    }

    private Violation buildViolation(Inspection inspection, ValidationResult result) {
        return Violation.builder()
                .inspection(inspection)
                .rule(result.rule())
                .severity(result.rule().getSeverity())
                .field(result.field())
                .description(result.explanation())
                .actualValue(result.actualValue())
                .expectedValue(result.expectedValue())
                .confidence(BigDecimal.valueOf(result.confidence()))
                .boundingBox(BoundingBoxJson.toJson(result.boundingBox()))
                .suggestedFix(result.rule().getSuggestion())
                .legalReference(result.rule().getLegalReference())
                .build();
    }
}
