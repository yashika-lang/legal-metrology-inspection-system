package com.legalmetrology.trace.service.impl;

import com.legalmetrology.exception.ResourceNotFoundException;
import com.legalmetrology.inspection.entity.Inspection;
import com.legalmetrology.inspection.repository.InspectionRepository;
import com.legalmetrology.trace.entity.DecisionTraceStep;
import com.legalmetrology.trace.repository.DecisionTraceStepRepository;
import com.legalmetrology.trace.service.DecisionTraceService;
import com.legalmetrology.trace.service.RecordStepCommand;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class DecisionTraceServiceImpl implements DecisionTraceService {

    private final DecisionTraceStepRepository decisionTraceStepRepository;
    private final InspectionRepository inspectionRepository;

    @Override
    public DecisionTraceStep recordStep(RecordStepCommand command) {
        Inspection inspection = inspectionRepository.getReferenceById(command.inspectionId());

        DecisionTraceStep step = DecisionTraceStep.builder()
                .inspection(inspection)
                .stepName(command.stepName())
                .module(command.module())
                .inputSummary(command.inputSummary())
                .outputSummary(command.outputSummary())
                .confidence(command.confidence())
                .executionTimeMs(command.executionTimeMs())
                .startedAt(command.startedAt())
                .status(command.status())
                .reason(command.reason())
                .referencedRuleId(command.referencedRuleId())
                .referencedEvidenceId(command.referencedEvidenceId())
                .referencedImageId(command.referencedImageId())
                .build();

        return decisionTraceStepRepository.save(step);
    }

    @Override
    @Transactional(readOnly = true)
    public List<DecisionTraceStep> getTrace(UUID inspectionId) {
        if (!inspectionRepository.existsById(inspectionId)) {
            throw ResourceNotFoundException.of("Inspection", inspectionId);
        }
        return decisionTraceStepRepository.findByInspectionIdOrderByCreatedAtAsc(inspectionId);
    }
}
