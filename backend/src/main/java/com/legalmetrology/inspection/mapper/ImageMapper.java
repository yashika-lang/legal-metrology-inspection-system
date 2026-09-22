package com.legalmetrology.inspection.mapper;

import com.legalmetrology.inspection.dto.ImageResponse;
import com.legalmetrology.inspection.entity.Image;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface ImageMapper {

    /** signedUrl is resolved by the service layer (via StorageService) since it requires an outbound call. */
    default ImageResponse toResponse(Image image, String signedUrl) {
        if (image == null) {
            return null;
        }
        List<String> warnings = image.getQualityWarnings() == null || image.getQualityWarnings().isBlank()
                ? List.of()
                : List.of(image.getQualityWarnings().split(","));
        return new ImageResponse(
                image.getId(),
                image.getInspection().getId(),
                image.getStoragePath(),
                signedUrl,
                image.getImageType(),
                image.getQualityScore(),
                warnings,
                image.getRecommendedAction(),
                image.getCreatedAt()
        );
    }
}
