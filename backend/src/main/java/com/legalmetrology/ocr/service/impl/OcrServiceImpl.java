package com.legalmetrology.ocr.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.legalmetrology.common.enums.DetectionSource;
import com.legalmetrology.common.enums.OcrProvider;
import com.legalmetrology.exception.ResourceNotFoundException;
import com.legalmetrology.inspection.entity.Image;
import com.legalmetrology.inspection.entity.OCRResult;
import com.legalmetrology.inspection.repository.ImageRepository;
import com.legalmetrology.inspection.repository.OCRResultRepository;
import com.legalmetrology.ocr.classify.ClassifiedField;
import com.legalmetrology.ocr.classify.DeclarationClassifier;
import com.legalmetrology.ocr.correction.CorrectionResult;
import com.legalmetrology.ocr.correction.OcrCorrectionService;
import com.legalmetrology.ocr.dto.OcrRunResponse;
import com.legalmetrology.ocr.model.OcrExtractionResult;
import com.legalmetrology.ocr.provider.OcrProviderFactory;
import com.legalmetrology.ocr.service.OcrService;
import com.legalmetrology.storage.dto.StorageBucket;
import com.legalmetrology.storage.service.StorageService;
import com.legalmetrology.utils.FileUploadUtil;
import com.legalmetrology.utils.ImageUtil;
import com.legalmetrology.vision.BoundingBoxJson;
import com.legalmetrology.vision.entity.LabelDetection;
import com.legalmetrology.vision.font.FontAnalysisService;
import com.legalmetrology.vision.mapper.LabelDetectionMapper;
import com.legalmetrology.vision.repository.LabelDetectionRepository;
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
public class OcrServiceImpl implements OcrService {

    private static final String MODULE = "OcrServiceImpl";

    private final ImageRepository imageRepository;
    private final StorageService storageService;
    private final OcrProviderFactory ocrProviderFactory;
    private final OcrCorrectionService ocrCorrectionService;
    private final DeclarationClassifier declarationClassifier;
    private final OCRResultRepository ocrResultRepository;
    private final LabelDetectionRepository labelDetectionRepository;
    private final FontAnalysisService fontAnalysisService;
    private final LabelDetectionMapper labelDetectionMapper;
    private final ObjectMapper objectMapper;
    private final DecisionTraceService decisionTraceService;

    @Override
    @Transactional
    public OcrRunResponse run(UUID imageId) {
        long stepStart = System.currentTimeMillis();
        Image image = imageRepository.findById(imageId)
                .orElseThrow(() -> ResourceNotFoundException.of("Image", imageId));

        byte[] imageBytes = storageService.download(StorageBucket.IMAGES, image.getStoragePath());
        String mimeType = FileUploadUtil.inferMimeTypeFromPath(image.getStoragePath());

        OcrExtractionResult extraction = ocrProviderFactory.extractWithFallback(imageBytes, mimeType);
        log.info("OCR ({}) extracted image {}: {} paragraphs, language={}, confidence={}",
                extraction.providerKey(), imageId, extraction.paragraphs().size(),
                extraction.detectedLanguage(), extraction.overallConfidence());

        CorrectionResult correction = ocrCorrectionService.correct(extraction.fullText());

        OCRResult ocrResult = persistAuditRow(image, extraction, correction);
        UUID inspectionId = image.getInspection().getId();
        decisionTraceService.recordStep(RecordStepCommand.success(inspectionId, TraceStepName.OCR, MODULE,
                        "Image " + imageId, extraction.paragraphs().size() + " paragraph(s) via " + extraction.providerKey(),
                        BigDecimal.valueOf(extraction.overallConfidence()), stepStart)
                .withReferences(null, null, imageId));
        decisionTraceService.recordStep(RecordStepCommand.success(inspectionId, TraceStepName.OCR_CORRECTION, MODULE,
                        extraction.fullText(), correction.changesSummary(),
                        BigDecimal.valueOf(correction.confidence()), stepStart)
                .withReferences(null, null, imageId));

        List<LabelDetection> persistedFields = classifyAndPersistFields(image, extraction);

        BufferedImage sourceImage = ImageUtil.read(new ByteArrayInputStream(imageBytes));
        persistedFields.forEach(detection -> fontAnalysisService.analyzeAndPersist(detection, sourceImage));

        return new OcrRunResponse(
                ocrResult.getId(),
                imageId,
                extraction.providerKey(),
                extraction.detectedLanguage().name(),
                extraction.fullText(),
                correction.correctedText(),
                BigDecimal.valueOf(extraction.overallConfidence()),
                BigDecimal.valueOf(correction.confidence()),
                correction.changesSummary(),
                persistedFields.stream().map(labelDetectionMapper::toResponse).toList(),
                ocrResult.getCreatedAt()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public List<OCRResult> getHistory(UUID imageId) {
        return ocrResultRepository.findByImageIdOrderByCreatedAtDesc(imageId);
    }

    private OCRResult persistAuditRow(Image image, OcrExtractionResult extraction, CorrectionResult correction) {
        OCRResult ocrResult = OCRResult.builder()
                .image(image)
                .provider(toOcrProviderEnum(extraction.providerKey()))
                .rawText(extraction.fullText())
                .correctedText(correction.correctedText())
                .confidence(BigDecimal.valueOf(extraction.overallConfidence()))
                .correctionConfidence(BigDecimal.valueOf(correction.confidence()))
                .detectedLanguage(extraction.detectedLanguage().googleVisionCode())
                .structuredHierarchy(serializeHierarchy(extraction))
                .build();
        // Always inserted, never updated — this table is the OCR audit history (see OCRResult's Javadoc).
        return ocrResultRepository.save(ocrResult);
    }

    private List<LabelDetection> classifyAndPersistFields(Image image, OcrExtractionResult extraction) {
        // Detections are the *current* OCR-derived understanding of this image, not a run
        // history (OCRResult already is that) — each run replaces the prior OCR_TEXT rows.
        labelDetectionRepository.deleteByImageIdAndSource(image.getId(), DetectionSource.OCR_TEXT);

        List<ClassifiedField> classifiedFields = declarationClassifier.classify(extraction);

        return classifiedFields.stream()
                .map(field -> labelDetectionRepository.save(LabelDetection.builder()
                        .image(image)
                        .declarationType(field.declarationType())
                        .detectedValue(field.normalizedValue())
                        .confidence(BigDecimal.valueOf(field.confidence()))
                        .source(DetectionSource.OCR_TEXT)
                        .ocrProvider(extraction.providerKey())
                        .language(field.language() != null ? field.language().googleVisionCode() : null)
                        .boundingBox(BoundingBoxJson.toJson(field.boundingBox()))
                        .present(true)
                        .build()))
                .toList();
    }

    private String serializeHierarchy(OcrExtractionResult extraction) {
        try {
            return objectMapper.writeValueAsString(extraction.paragraphs());
        } catch (Exception ex) {
            log.warn("Failed to serialize OCR hierarchy for storage: {}", ex.getMessage());
            return null;
        }
    }

    private OcrProvider toOcrProviderEnum(String providerKey) {
        return switch (providerKey) {
            case "google-vision" -> OcrProvider.GOOGLE_VISION;
            case "tesseract" -> OcrProvider.TESSERACT;
            default -> throw new IllegalStateException("Unknown OCR provider key: " + providerKey);
        };
    }

}
