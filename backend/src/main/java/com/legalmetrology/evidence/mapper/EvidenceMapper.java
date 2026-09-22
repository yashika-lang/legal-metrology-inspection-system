package com.legalmetrology.evidence.mapper;

import com.legalmetrology.evidence.dto.EvidenceResponse;
import com.legalmetrology.evidence.entity.Evidence;
import com.legalmetrology.vision.BoundingBoxJson;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface EvidenceMapper {

    /** Signed URLs are resolved by the service layer (via StorageService) since that requires an outbound call. */
    default EvidenceResponse toResponse(Evidence evidence, String originalImageUrl, String annotatedImageUrl) {
        if (evidence == null) {
            return null;
        }
        return new EvidenceResponse(
                evidence.getId(),
                evidence.getSha256Hash(),
                evidence.getInspection().getId(),
                evidence.getImage() != null ? evidence.getImage().getId() : null,
                evidence.getViolation().getId(),
                originalImageUrl,
                annotatedImageUrl,
                BoundingBoxJson.fromJson(evidence.getBoundingBox()),
                evidence.getOcrText(),
                evidence.getNormalizedValue(),
                evidence.getVisionConfidence(),
                evidence.getOcrConfidence(),
                evidence.getFusedConfidence(),
                evidence.getExpectedValue(),
                evidence.getActualValue(),
                evidence.getReason(),
                evidence.getSuggestedFix(),
                evidence.getLegalRuleReference(),
                evidence.getSeverity(),
                evidence.isImmutable(),
                evidence.getCreatedAt()
        );
    }
}
