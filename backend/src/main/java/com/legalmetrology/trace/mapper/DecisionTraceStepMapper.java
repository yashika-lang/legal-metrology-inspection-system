package com.legalmetrology.trace.mapper;

import com.legalmetrology.trace.dto.DecisionTraceStepResponse;
import com.legalmetrology.trace.entity.DecisionTraceStep;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface DecisionTraceStepMapper {

    default DecisionTraceStepResponse toResponse(DecisionTraceStep step) {
        if (step == null) {
            return null;
        }
        return new DecisionTraceStepResponse(
                step.getId(),
                step.getInspection().getId(),
                step.getStepName(),
                step.getModule(),
                step.getInputSummary(),
                step.getOutputSummary(),
                step.getConfidence(),
                step.getExecutionTimeMs(),
                step.getStartedAt(),
                step.getStatus(),
                step.getReason(),
                step.getReferencedRuleId(),
                step.getReferencedEvidenceId(),
                step.getReferencedImageId(),
                step.getCreatedAt()
        );
    }
}
