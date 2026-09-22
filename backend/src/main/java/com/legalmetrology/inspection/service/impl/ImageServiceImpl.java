package com.legalmetrology.inspection.service.impl;

import com.legalmetrology.common.enums.ImageType;
import com.legalmetrology.exception.BadRequestException;
import com.legalmetrology.exception.ResourceNotFoundException;
import com.legalmetrology.inspection.dto.ImageResponse;
import com.legalmetrology.inspection.entity.Image;
import com.legalmetrology.inspection.entity.Inspection;
import com.legalmetrology.inspection.mapper.ImageMapper;
import com.legalmetrology.inspection.repository.ImageRepository;
import com.legalmetrology.inspection.repository.InspectionRepository;
import com.legalmetrology.inspection.service.ImageService;
import com.legalmetrology.storage.dto.StorageBucket;
import com.legalmetrology.storage.service.StorageService;
import com.legalmetrology.utils.FileUploadUtil;
import com.legalmetrology.utils.ImageUtil;
import com.legalmetrology.vision.quality.ImageQualityService;
import com.legalmetrology.common.enums.RoleName;
import com.legalmetrology.common.enums.TraceStepName;
import com.legalmetrology.security.SecurityUtils;
import com.legalmetrology.trace.service.DecisionTraceService;
import com.legalmetrology.trace.service.RecordStepCommand;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ImageServiceImpl implements ImageService {

    private static final String MODULE = "ImageServiceImpl";

    private final ImageRepository imageRepository;
    private final InspectionRepository inspectionRepository;
    private final StorageService storageService;
    private final ImageMapper imageMapper;
    private final ImageQualityService imageQualityService;
    private final DecisionTraceService decisionTraceService;
    private final SecurityUtils securityUtils;

    @Override
    @Transactional
    public ImageResponse upload(UUID inspectionId, MultipartFile file, ImageType imageType) {
        long stepStart = System.currentTimeMillis();
        FileUploadUtil.validateImage(file);

        Inspection inspection = inspectionRepository.findById(inspectionId)
                .orElseThrow(() -> ResourceNotFoundException.of("Inspection", inspectionId));

        byte[] content;
        try {
            content = file.getBytes();
        } catch (IOException ex) {
            throw new BadRequestException("Failed to read the uploaded file: " + ex.getMessage());
        }

        String perceptualHash = ImageUtil.perceptualHash(ImageUtil.read(new ByteArrayInputStream(content)));
        String path = inspectionId + "/" + (imageType != null ? imageType.name().toLowerCase() : "other")
                + "/" + FileUploadUtil.generateUniqueFileName(file);

        storageService.upload(StorageBucket.IMAGES, path, content, file.getContentType());

        Image image = Image.builder()
                .inspection(inspection)
                .storagePath(path)
                .imageType(imageType != null ? imageType : ImageType.OTHER)
                .perceptualHash(perceptualHash)
                .build();
        image = imageRepository.save(image);
        decisionTraceService.recordStep(RecordStepCommand.success(inspectionId, TraceStepName.IMAGE_UPLOADED, MODULE,
                        file.getOriginalFilename(), "Stored at " + path, null, stepStart)
                .withReferences(null, null, image.getId()));

        // Step 1 of the AI pipeline: every upload is quality-checked immediately,
        // before any OCR/Vision spend, so a bad capture is caught at the point
        // the officer can still just retake the photo.
        stepStart = System.currentTimeMillis();
        imageQualityService.analyzeAndPersist(image, content);
        decisionTraceService.recordStep(RecordStepCommand.success(inspectionId, TraceStepName.IMAGE_QUALITY, MODULE,
                        "Image " + image.getId(),
                        "Quality score " + image.getQualityScore() + ", recommended action " + image.getRecommendedAction(),
                        normalizeQualityScoreToConfidence(image.getQualityScore()), stepStart)
                .withReferences(null, null, image.getId()));

        log.info("Image uploaded for inspection {}: path={}", inspectionId, path);
        return imageMapper.toResponse(image, storageService.generateSignedUrl(StorageBucket.IMAGES, path));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ImageResponse> listByInspection(UUID inspectionId) {
        return imageRepository.findByInspectionId(inspectionId).stream()
                .map(image -> imageMapper.toResponse(image, storageService.generateSignedUrl(StorageBucket.IMAGES, image.getStoragePath())))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public ImageResponse getById(UUID imageId) {
        Image image = findImageOrThrow(imageId);
        return imageMapper.toResponse(image, storageService.generateSignedUrl(StorageBucket.IMAGES, image.getStoragePath()));
    }

    @Override
    @Transactional
    public void delete(UUID imageId) {
        Image image = findImageOrThrow(imageId);
        UUID ownerId = image.getInspection().getInspector().getId();
        if (!ownerId.equals(securityUtils.getCurrentUserId())
                && !securityUtils.currentUserHasAnyRole(RoleName.ADMIN, RoleName.SENIOR_OFFICER)) {
            throw new AccessDeniedException("Only the inspecting officer or an admin/senior officer may delete this image");
        }
        storageService.delete(StorageBucket.IMAGES, image.getStoragePath());
        imageRepository.delete(image);
    }

    /**
     * {@code images.quality_score} is a 0-100 heuristic score, but
     * {@code decision_trace_steps.confidence} is a {@code numeric(5,4)}
     * [0,1] fraction shared by every other pipeline stage — passing the
     * raw 0-100 score there overflows the column (values >= 10 exceed
     * precision 5, scale 4). Normalize to keep "confidence" meaning the
     * same thing across every trace step.
     */
    private BigDecimal normalizeQualityScoreToConfidence(BigDecimal qualityScore) {
        return qualityScore != null
                ? qualityScore.divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP)
                : null;
    }

    private Image findImageOrThrow(UUID imageId) {
        return imageRepository.findById(imageId)
                .orElseThrow(() -> ResourceNotFoundException.of("Image", imageId));
    }
}
