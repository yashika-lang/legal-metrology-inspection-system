package com.legalmetrology.ocr.mapper;

import com.legalmetrology.ocr.dto.FusedDeclarationResponse;
import com.legalmetrology.ocr.entity.FusedDeclaration;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface FusedDeclarationMapper {

    default FusedDeclarationResponse toResponse(FusedDeclaration fused) {
        if (fused == null) {
            return null;
        }
        return new FusedDeclarationResponse(
                fused.getId(),
                fused.getInspection().getId(),
                fused.getDeclarationType(),
                fused.getFusedValue(),
                fused.getFusedConfidence(),
                fused.isPresent(),
                fused.getOcrValue(),
                fused.getOcrConfidence(),
                fused.getVisionValue(),
                fused.getVisionConfidence(),
                fused.getAgreement(),
                fused.isFromProductDatabase()
        );
    }
}
