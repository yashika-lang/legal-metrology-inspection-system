package com.legalmetrology.vision.service.impl;

import com.legalmetrology.common.enums.DetectionSource;
import com.legalmetrology.exception.ResourceNotFoundException;
import com.legalmetrology.inspection.entity.Image;
import com.legalmetrology.inspection.repository.ImageRepository;
import com.legalmetrology.storage.dto.StorageBucket;
import com.legalmetrology.storage.service.StorageService;
import com.legalmetrology.utils.FileUploadUtil;
import com.legalmetrology.utils.ImageUtil;
import com.legalmetrology.vision.BoundingBoxJson;
import com.legalmetrology.vision.entity.LabelDetection;
import com.legalmetrology.vision.font.FontAnalysisService;
import com.legalmetrology.vision.provider.DetectedDeclaration;
import com.legalmetrology.vision.provider.VisionAiProvider;
import com.legalmetrology.vision.provider.VisionAiProviderFactory;
import com.legalmetrology.vision.provider.VisionAnalysisResult;
import com.legalmetrology.vision.repository.LabelDetectionRepository;
import com.legalmetrology.vision.service.VisionAiService;
import com.legalmetrology.common.enums.TraceStepName;
import com.legalmetrology.trace.service.DecisionTraceService;
import com.legalmetrology.trace.service.RecordStepCommand;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class VisionAiServiceImpl implements VisionAiService {

    private static final String MODULE = "VisionAiServiceImpl";

    private final VisionAiProviderFactory providerFactory;
    private final LabelDetectionRepository labelDetectionRepository;
    private final FontAnalysisService fontAnalysisService;
    private final ImageRepository imageRepository;
    private final StorageService storageService;
    private final DecisionTraceService decisionTraceService;

    @Override
    @Transactional
    public List<LabelDetection> analyzeAndPersist(Image image, byte[] imageBytes, String mimeType) {
        long stepStart = System.currentTimeMillis();
        VisionAiProvider provider = providerFactory.resolve();
        VisionAnalysisResult analysis = provider.analyzeLabel(imageBytes, mimeType);

        log.info("Vision AI ({}) analyzed image {}: {} declarations, section={}, latency={}ms",
                analysis.model(), image.getId(), analysis.declarations().size(), analysis.labelSection(), analysis.latencyMs());

        // Detections reflect the current understanding of the image, not a run history —
        // re-analysis (e.g. after a retake) replaces the prior VISION_AI rows rather than
        // accumulating duplicates alongside them.
        labelDetectionRepository.deleteByImageIdAndSource(image.getId(), DetectionSource.VISION_AI);

        BufferedImage sourceImage = ImageUtil.read(new ByteArrayInputStream(imageBytes));

        List<LabelDetection> persisted = analysis.declarations().stream()
                .map(declaration -> persistDetection(image, declaration, analysis))
                .toList();

        persisted.forEach(detection -> fontAnalysisService.analyzeAndPersist(detection, sourceImage));

        decisionTraceService.recordStep(RecordStepCommand.success(image.getInspection().getId(), TraceStepName.VISION_DETECTION, MODULE,
                        "Image " + image.getId(),
                        persisted.size() + " declaration(s) detected via " + analysis.model(), null, stepStart)
                .withReferences(null, null, image.getId()));

        return persisted;
    }

    @Override
    @Transactional
    public List<LabelDetection> analyzeAndPersist(UUID imageId) {
        Image image = imageRepository.findById(imageId)
                .orElseThrow(() -> ResourceNotFoundException.of("Image", imageId));
        byte[] bytes = storageService.download(StorageBucket.IMAGES, image.getStoragePath());
        return analyzeAndPersist(image, bytes, FileUploadUtil.inferMimeTypeFromPath(image.getStoragePath()));
    }

    @Override
    @Transactional(readOnly = true)
    public List<LabelDetection> getDetections(UUID imageId) {
        return labelDetectionRepository.findByImageIdAndSource(imageId, DetectionSource.VISION_AI);
    }

    private LabelDetection persistDetection(Image image, DetectedDeclaration declaration, VisionAnalysisResult analysis) {
        LabelDetection detection = LabelDetection.builder()
                .image(image)
                .declarationType(declaration.type())
                .detectedValue(declaration.value())
                .confidence(BigDecimal.valueOf(declaration.confidence()))
                .source(DetectionSource.VISION_AI)
                .boundingBox(BoundingBoxJson.toJson(declaration.boundingBox()))
                .present(declaration.present())
                .labelSection(analysis.labelSection())
                .build();
        return labelDetectionRepository.save(detection);
    }
}
