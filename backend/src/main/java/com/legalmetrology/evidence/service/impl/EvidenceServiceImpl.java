package com.legalmetrology.evidence.service.impl;

import com.legalmetrology.common.enums.TraceStepName;
import com.legalmetrology.evidence.annotate.AnnotatedImageService;
import com.legalmetrology.evidence.annotate.AnnotationBox;
import com.legalmetrology.evidence.entity.Evidence;
import com.legalmetrology.evidence.hash.EvidenceHasher;
import com.legalmetrology.evidence.repository.EvidenceRepository;
import com.legalmetrology.evidence.service.EvidenceService;
import com.legalmetrology.exception.BadRequestException;
import com.legalmetrology.exception.ResourceNotFoundException;
import com.legalmetrology.inspection.entity.Image;
import com.legalmetrology.inspection.entity.Inspection;
import com.legalmetrology.inspection.entity.Violation;
import com.legalmetrology.inspection.repository.InspectionRepository;
import com.legalmetrology.inspection.repository.ViolationRepository;
import com.legalmetrology.ocr.entity.FusedDeclaration;
import com.legalmetrology.ocr.repository.FusedDeclarationRepository;
import com.legalmetrology.storage.dto.StorageBucket;
import com.legalmetrology.storage.service.StorageService;
import com.legalmetrology.trace.service.DecisionTraceService;
import com.legalmetrology.trace.service.RecordStepCommand;
import com.legalmetrology.utils.ImageUtil;
import com.legalmetrology.vision.BoundingBoxJson;
import com.legalmetrology.vision.entity.LabelDetection;
import com.legalmetrology.vision.provider.VisionBoundingBox;
import com.legalmetrology.vision.repository.LabelDetectionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Implements the Explainable Evidence Framework: for each violation, walks
 * back through {@code FusedDeclaration} to the winning OCR/Vision detection,
 * locates the image it came from, crops and annotates that region, hashes
 * the result, and persists one {@link Evidence} row. See {@link Evidence}'s
 * Javadoc for the full field-by-field rationale.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EvidenceServiceImpl implements EvidenceService {

    private static final String MODULE = "EvidenceServiceImpl";

    private final InspectionRepository inspectionRepository;
    private final ViolationRepository violationRepository;
    private final FusedDeclarationRepository fusedDeclarationRepository;
    private final LabelDetectionRepository labelDetectionRepository;
    private final StorageService storageService;
    private final AnnotatedImageService annotatedImageService;
    private final EvidenceHasher evidenceHasher;
    private final EvidenceRepository evidenceRepository;
    private final DecisionTraceService decisionTraceService;

    @Override
    @Transactional
    public List<Evidence> generateForInspection(UUID inspectionId) {
        long stepStart = System.currentTimeMillis();
        List<Violation> violations = violationRepository.findByInspectionId(inspectionId);

        List<Evidence> generated = violations.stream()
                .filter(violation -> evidenceRepository.findByViolationId(violation.getId())
                        .map(existing -> !existing.isImmutable())
                        .orElse(true))
                .map(this::generate)
                .toList();

        decisionTraceService.recordStep(RecordStepCommand.success(inspectionId, TraceStepName.EVIDENCE_GENERATED, MODULE,
                violations.size() + " violation(s)", generated.size() + " evidence record(s) generated/refreshed", null, stepStart));

        return generated;
    }

    @Override
    @Transactional
    public Evidence generateForViolation(UUID violationId) {
        Violation violation = violationRepository.findById(violationId)
                .orElseThrow(() -> ResourceNotFoundException.of("Violation", violationId));
        return generate(violation);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Evidence> getByInspection(UUID inspectionId) {
        if (!inspectionRepository.existsById(inspectionId)) {
            throw ResourceNotFoundException.of("Inspection", inspectionId);
        }
        return evidenceRepository.findByInspectionId(inspectionId);
    }

    @Override
    @Transactional(readOnly = true)
    public Evidence getById(UUID evidenceId) {
        return evidenceRepository.findById(evidenceId)
                .orElseThrow(() -> ResourceNotFoundException.of("Evidence", evidenceId));
    }

    @Override
    @Transactional
    public void finalizeEvidence(UUID inspectionId) {
        List<Evidence> evidence = evidenceRepository.findByInspectionId(inspectionId);
        evidence.forEach(e -> e.setImmutable(true));
        evidenceRepository.saveAll(evidence);
        log.info("Finalized {} evidence record(s) as immutable for inspection {}", evidence.size(), inspectionId);
    }

    private Evidence generate(Violation violation) {
        Evidence existing = evidenceRepository.findByViolationId(violation.getId()).orElse(null);
        if (existing != null && existing.isImmutable()) {
            throw new BadRequestException(
                    "Evidence for violation " + violation.getId() + " is immutable — the inspection has been finalized");
        }

        Inspection inspection = violation.getInspection();
        DeclarationSource source = resolveDeclarationSource(inspection.getId(), violation);

        Evidence evidence = existing != null ? existing : Evidence.builder()
                .inspection(inspection)
                .violation(violation)
                .build();

        evidence.setImage(source.image());
        evidence.setBoundingBox(source.boundingBoxJson() != null ? source.boundingBoxJson() : violation.getBoundingBox());
        evidence.setOcrText(source.ocrText());
        evidence.setNormalizedValue(source.normalizedValue());
        evidence.setOcrConfidence(source.ocrConfidence());
        evidence.setVisionConfidence(source.visionConfidence());
        evidence.setFusedConfidence(source.fusedConfidence() != null ? source.fusedConfidence() : violation.getConfidence());
        evidence.setExpectedValue(violation.getExpectedValue());
        evidence.setActualValue(violation.getActualValue());
        evidence.setReason(violation.getDescription());
        evidence.setSuggestedFix(violation.getSuggestedFix());
        evidence.setLegalRuleReference(violation.getLegalReference());
        evidence.setSeverity(violation.getSeverity());

        byte[] annotatedBytes = null;
        if (source.image() != null) {
            byte[] originalBytes = storageService.download(StorageBucket.IMAGES, source.image().getStoragePath());
            java.awt.image.BufferedImage sourceImage = ImageUtil.read(new ByteArrayInputStream(originalBytes));
            VisionBoundingBox box = BoundingBoxJson.fromJson(evidence.getBoundingBox());

            AnnotationBox annotationBox = new AnnotationBox(box, describeField(violation), violation.getSeverity(), evidence.getFusedConfidence());
            annotatedBytes = annotatedImageService.annotate(sourceImage, box != null ? List.of(annotationBox) : List.of());
            byte[] croppedBytes = box != null ? annotatedImageService.crop(sourceImage, box) : originalBytes;

            String basePath = inspection.getId() + "/" + violation.getId();
            storageService.upload(StorageBucket.EVIDENCE, basePath + "/original.png", croppedBytes, "image/png");
            storageService.upload(StorageBucket.EVIDENCE, basePath + "/annotated.png", annotatedBytes, "image/png");
            evidence.setOriginalImagePath(basePath + "/original.png");
            evidence.setAnnotatedImagePath(basePath + "/annotated.png");
        }

        evidence.setSha256Hash(evidenceHasher.hash(evidence, annotatedBytes));
        return evidenceRepository.save(evidence);
    }

    private String describeField(Violation violation) {
        return violation.getField() != null ? violation.getField().name() : violation.getRule().getRuleCode();
    }

    /** Everything about the winning OCR/Vision candidate behind a violation's declaration, if one exists. */
    private record DeclarationSource(Image image, String boundingBoxJson, String ocrText, String normalizedValue,
                                      BigDecimal ocrConfidence, BigDecimal visionConfidence, BigDecimal fusedConfidence) {
        static DeclarationSource empty() {
            return new DeclarationSource(null, null, null, null, null, null, null);
        }
    }

    private DeclarationSource resolveDeclarationSource(UUID inspectionId, Violation violation) {
        if (violation.getField() == null) {
            return DeclarationSource.empty();
        }

        Optional<FusedDeclaration> fusedOpt = fusedDeclarationRepository.findByInspectionIdAndDeclarationType(inspectionId, violation.getField());
        if (fusedOpt.isEmpty()) {
            return DeclarationSource.empty();
        }
        FusedDeclaration fused = fusedOpt.get();

        UUID winningDetectionId = winningDetectionId(fused);
        Image image = winningDetectionId != null
                ? labelDetectionRepository.findById(winningDetectionId).map(LabelDetection::getImage).orElse(null)
                : null;

        return new DeclarationSource(image, fused.getBoundingBox(), fused.getOcrValue(), fused.getFusedValue(),
                fused.getOcrConfidence(), fused.getVisionConfidence(), fused.getFusedConfidence());
    }

    /** Mirrors {@code DeclarationFusionServiceImpl.fuse}: the higher-confidence side is the one whose bounding box/image fused forward. */
    private UUID winningDetectionId(FusedDeclaration fused) {
        double ocrConf = fused.getOcrConfidence() != null ? fused.getOcrConfidence().doubleValue() : -1;
        double visionConf = fused.getVisionConfidence() != null ? fused.getVisionConfidence().doubleValue() : -1;
        if (ocrConf < 0 && visionConf < 0) {
            return null;
        }
        return visionConf >= ocrConf ? fused.getVisionSourceDetectionId() : fused.getOcrSourceDetectionId();
    }
}
