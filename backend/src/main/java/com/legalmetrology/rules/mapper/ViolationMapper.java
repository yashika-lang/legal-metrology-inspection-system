package com.legalmetrology.rules.mapper;

import com.legalmetrology.inspection.entity.Violation;
import com.legalmetrology.rules.dto.ValidationResultResponse;
import com.legalmetrology.rules.dto.ViolationResponse;
import com.legalmetrology.rules.model.ValidationResult;
import com.legalmetrology.vision.BoundingBoxJson;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ViolationMapper {

    default ViolationResponse toResponse(Violation violation) {
        if (violation == null) {
            return null;
        }
        return new ViolationResponse(
                violation.getId(),
                violation.getInspection().getId(),
                violation.getRule().getId(),
                violation.getRule().getRuleCode(),
                violation.getRule().getTitle(),
                violation.getSeverity(),
                violation.getField(),
                violation.getDescription(),
                violation.getActualValue(),
                violation.getExpectedValue(),
                violation.getConfidence(),
                BoundingBoxJson.fromJson(violation.getBoundingBox()),
                violation.getSuggestedFix(),
                violation.getLegalReference(),
                violation.getStatus(),
                violation.getCreatedAt()
        );
    }

    default ValidationResultResponse toResponse(ValidationResult result) {
        if (result == null) {
            return null;
        }
        return new ValidationResultResponse(
                result.rule().getId(),
                result.rule().getRuleCode(),
                result.rule().getTitle(),
                result.rule().getSeverity(),
                result.passed(),
                result.field(),
                result.actualValue(),
                result.expectedValue(),
                result.confidence(),
                result.boundingBox(),
                result.explanation()
        );
    }
}
