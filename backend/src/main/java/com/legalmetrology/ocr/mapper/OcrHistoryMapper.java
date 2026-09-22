package com.legalmetrology.ocr.mapper;

import com.legalmetrology.inspection.entity.OCRResult;
import com.legalmetrology.ocr.dto.OcrHistoryEntryResponse;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface OcrHistoryMapper {

    default OcrHistoryEntryResponse toResponse(OCRResult result) {
        if (result == null) {
            return null;
        }
        return new OcrHistoryEntryResponse(
                result.getId(),
                result.getProvider() != null ? result.getProvider().name() : null,
                result.getDetectedLanguage(),
                result.getRawText(),
                result.getCorrectedText(),
                result.getConfidence(),
                result.getCorrectionConfidence(),
                result.getCreatedAt()
        );
    }
}
