package com.legalmetrology.inspection.service;

import com.legalmetrology.common.enums.ImageType;
import com.legalmetrology.inspection.dto.ImageResponse;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

public interface ImageService {

    ImageResponse upload(UUID inspectionId, MultipartFile file, ImageType imageType);

    List<ImageResponse> listByInspection(UUID inspectionId);

    ImageResponse getById(UUID imageId);

    void delete(UUID imageId);
}
