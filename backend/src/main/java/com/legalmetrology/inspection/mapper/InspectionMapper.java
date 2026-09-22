package com.legalmetrology.inspection.mapper;

import com.legalmetrology.inspection.dto.InspectionResponse;
import com.legalmetrology.inspection.dto.InspectionStatusHistoryResponse;
import com.legalmetrology.inspection.entity.Inspection;
import com.legalmetrology.inspection.entity.InspectionStatusHistory;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface InspectionMapper {

    default InspectionResponse toResponse(Inspection inspection) {
        return toResponse(inspection, null);
    }

    /**
     * @param fallbackProductName Used only when this inspection has no linked
     *                             {@code product} — the OCR/Vision-fused
     *                             {@code PRODUCT_NAME} declaration's value, if the
     *                             AI Pipeline has run and found one on the label.
     *                             Real extracted data, not a guess: still {@code null}
     *                             when the pipeline hasn't run or found no name.
     */
    default InspectionResponse toResponse(Inspection inspection, String fallbackProductName) {
        if (inspection == null) {
            return null;
        }
        return new InspectionResponse(
                inspection.getId(),
                inspection.getInspector().getId(),
                inspection.getInspector().getFullName(),
                inspection.getProduct() != null ? inspection.getProduct().getId() : null,
                inspection.getProduct() != null ? inspection.getProduct().getName() : fallbackProductName,
                inspection.getStatus(),
                inspection.getLocationLat(),
                inspection.getLocationLng(),
                inspection.getRegion(),
                inspection.getComplianceScore(),
                inspection.getFraudRisk(),
                inspection.getStartedAt(),
                inspection.getCompletedAt()
        );
    }

    default InspectionStatusHistoryResponse toResponse(InspectionStatusHistory history) {
        if (history == null) {
            return null;
        }
        return new InspectionStatusHistoryResponse(
                history.getId(),
                history.getFromStatus(),
                history.getToStatus(),
                history.getChangedBy() != null ? history.getChangedBy().getFullName() : null,
                history.getNote(),
                history.getCreatedAt()
        );
    }
}
