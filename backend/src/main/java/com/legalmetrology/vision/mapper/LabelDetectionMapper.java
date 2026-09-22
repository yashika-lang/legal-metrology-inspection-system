package com.legalmetrology.vision.mapper;

import com.legalmetrology.vision.BoundingBoxJson;
import com.legalmetrology.vision.dto.LabelDetectionResponse;
import com.legalmetrology.vision.entity.LabelDetection;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface LabelDetectionMapper {

    default LabelDetectionResponse toResponse(LabelDetection detection) {
        if (detection == null) {
            return null;
        }
        return new LabelDetectionResponse(
                detection.getId(),
                detection.getImage().getId(),
                detection.getDeclarationType(),
                detection.getDetectedValue(),
                detection.getConfidence(),
                detection.getSource(),
                BoundingBoxJson.fromJson(detection.getBoundingBox()),
                detection.isPresent(),
                detection.getLabelSection(),
                detection.getFontSizeEstimate(),
                detection.getReadabilityScore(),
                detection.getContrastScore(),
                detection.getFontIssue()
        );
    }
}
