package com.legalmetrology.ocr.fusion.impl;

import com.legalmetrology.common.enums.DeclarationType;
import com.legalmetrology.common.enums.DetectionSource;
import com.legalmetrology.exception.ResourceNotFoundException;
import com.legalmetrology.inspection.entity.Image;
import com.legalmetrology.inspection.entity.Inspection;
import com.legalmetrology.inspection.repository.ImageRepository;
import com.legalmetrology.inspection.repository.InspectionRepository;
import com.legalmetrology.ocr.entity.FusedDeclaration;
import com.legalmetrology.ocr.fusion.DeclarationFusionService;
import com.legalmetrology.ocr.repository.FusedDeclarationRepository;
import com.legalmetrology.product.entity.ProductDeclaration;
import com.legalmetrology.product.repository.ProductDeclarationRepository;
import com.legalmetrology.vision.entity.LabelDetection;
import com.legalmetrology.vision.repository.LabelDetectionRepository;
import com.legalmetrology.common.enums.TraceStepName;
import com.legalmetrology.trace.service.DecisionTraceService;
import com.legalmetrology.trace.service.RecordStepCommand;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class DeclarationFusionServiceImpl implements DeclarationFusionService {

    private static final String MODULE = "DeclarationFusionServiceImpl";

    /** Agreement between OCR and Vision AI is rewarded; disagreement is penalized — see {@link #fuse}. */
    private static final double AGREEMENT_CONFIDENCE_BONUS = 0.1;
    private static final double DISAGREEMENT_CONFIDENCE_PENALTY_FACTOR = 0.7;

    private final InspectionRepository inspectionRepository;
    private final ImageRepository imageRepository;
    private final LabelDetectionRepository labelDetectionRepository;
    private final FusedDeclarationRepository fusedDeclarationRepository;
    private final ProductDeclarationRepository productDeclarationRepository;
    private final DecisionTraceService decisionTraceService;

    @Override
    @Transactional
    public List<FusedDeclaration> fuseInspection(UUID inspectionId) {
        long stepStart = System.currentTimeMillis();
        Inspection inspection = inspectionRepository.findById(inspectionId)
                .orElseThrow(() -> ResourceNotFoundException.of("Inspection", inspectionId));

        List<UUID> imageIds = imageRepository.findByInspectionId(inspectionId).stream().map(Image::getId).toList();
        List<LabelDetection> allDetections = imageIds.isEmpty() ? List.of() : labelDetectionRepository.findByImageIdIn(imageIds);

        /*
         * Barcode-first workflow: when this inspection is linked to a
         * product with verified master-data declarations, those are
         * known-certain facts, not inferred ones — they always win over
         * whatever OCR/Vision AI produced for the same declaration type,
         * without discarding the OCR/Vision reads themselves (kept
         * alongside as informational context). A declaration type the
         * product's master data doesn't cover falls straight through to
         * the normal OCR/Vision fusion below, unchanged — this is how a
         * partially-specified product (e.g. only country of origin known)
         * still lets OCR "supplement" the gaps instead of blocking it.
         */
        Map<DeclarationType, String> productDeclarations = inspection.getProduct() == null
                ? Map.of()
                : productDeclarationRepository.findByProductId(inspection.getProduct().getId()).stream()
                        .collect(Collectors.toMap(ProductDeclaration::getDeclarationType, ProductDeclaration::getValue));

        List<FusedDeclaration> fused = List.of(DeclarationType.values()).stream()
                .map(type -> fuseOne(inspection, type, allDetections, productDeclarations.get(type)))
                .toList();

        long present = fused.stream().filter(FusedDeclaration::isPresent).count();
        long fromProductDb = fused.stream().filter(FusedDeclaration::isFromProductDatabase).count();
        decisionTraceService.recordStep(RecordStepCommand.success(inspectionId, TraceStepName.DECLARATION_FUSION, MODULE,
                allDetections.size() + " raw detection(s) across " + imageIds.size() + " image(s)"
                        + (fromProductDb > 0 ? ", " + fromProductDb + " from the scanned product's known data" : ""),
                present + "/" + fused.size() + " declaration type(s) resolved present", null, stepStart));

        return fused;
    }

    @Override
    @Transactional(readOnly = true)
    public List<FusedDeclaration> getFusedDeclarations(UUID inspectionId) {
        return fusedDeclarationRepository.findByInspectionId(inspectionId);
    }

    private FusedDeclaration fuseOne(Inspection inspection, DeclarationType type, List<LabelDetection> allDetections, String productDeclarationValue) {
        List<LabelDetection> forType = allDetections.stream().filter(d -> d.getDeclarationType() == type).toList();

        LabelDetection bestOcr = best(forType, DetectionSource.OCR_TEXT);
        LabelDetection bestVision = best(forType, DetectionSource.VISION_AI);

        FusedDeclaration fused = fusedDeclarationRepository.findByInspectionIdAndDeclarationType(inspection.getId(), type)
                .orElseGet(() -> FusedDeclaration.builder().inspection(inspection).declarationType(type).build());

        applyOcrCandidate(fused, bestOcr);
        applyVisionCandidate(fused, bestVision);
        fuse(fused, bestOcr, bestVision);

        if (productDeclarationValue != null) {
            fused.setFusedValue(productDeclarationValue);
            fused.setFusedConfidence(BigDecimal.ONE);
            fused.setPresent(true);
            fused.setFromProductDatabase(true);
        } else {
            fused.setFromProductDatabase(false);
        }

        return fusedDeclarationRepository.save(fused);
    }

    /**
     * Picks the best detection for one (declarationType, source) pair across
     * every image in the inspection — critically, "best" means "the most
     * confident detection that says this declaration IS present," not just
     * "the highest raw confidence value regardless of present/absent."
     * <p>
     * Multi-image inspections (Guided 360° Capture: Front/Back/Left/Right/
     * Top/Bottom) routinely produce one detection per image per declaration
     * type — most of them correctly reporting {@code present=false} for
     * faces where the declaration genuinely isn't printed, each with its
     * own "confidence that it's genuinely absent from this image." Ranking
     * purely by raw confidence let a confidently-absent detection from the
     * wrong face (e.g. Back, 0.90 confidence it's absent there) outrank a
     * genuinely-present detection from the right face (e.g. Front, 0.85
     * confidence it's present) — reporting a declaration that's clearly
     * visible in the photo as missing. Found live: NET_QUANTITY visibly
     * printed on a label was reported missing by the Rule Engine after
     * fusion picked an absent-on-another-face detection over the real one.
     * Any present detection now always outranks any absent one; ties among
     * present (or among absent) detections still go to the higher
     * confidence.
     */
    private LabelDetection best(List<LabelDetection> detections, DetectionSource source) {
        List<LabelDetection> forSource = detections.stream().filter(d -> d.getSource() == source).toList();

        Optional<LabelDetection> bestPresent = forSource.stream()
                .filter(LabelDetection::isPresent)
                .max(Comparator.comparing(d -> Optional.ofNullable(d.getConfidence()).orElse(BigDecimal.ZERO)));
        if (bestPresent.isPresent()) {
            return bestPresent.get();
        }

        return forSource.stream()
                .max(Comparator.comparing(d -> Optional.ofNullable(d.getConfidence()).orElse(BigDecimal.ZERO)))
                .orElse(null);
    }

    private void applyOcrCandidate(FusedDeclaration fused, LabelDetection ocr) {
        fused.setOcrValue(ocr != null ? ocr.getDetectedValue() : null);
        fused.setOcrConfidence(ocr != null ? ocr.getConfidence() : null);
        fused.setOcrSourceDetectionId(ocr != null ? ocr.getId() : null);
    }

    private void applyVisionCandidate(FusedDeclaration fused, LabelDetection vision) {
        fused.setVisionValue(vision != null ? vision.getDetectedValue() : null);
        fused.setVisionConfidence(vision != null ? vision.getConfidence() : null);
        fused.setVisionSourceDetectionId(vision != null ? vision.getId() : null);
    }

    /**
     * Combines the OCR and Vision AI signals for one declaration into a
     * single fused value/confidence, without discarding either raw signal
     * (both remain on {@code fused} regardless of the outcome here).
     */
    private void fuse(FusedDeclaration fused, LabelDetection ocr, LabelDetection vision) {
        if (ocr == null && vision == null) {
            fused.setFusedValue(null);
            fused.setFusedConfidence(BigDecimal.ZERO);
            fused.setPresent(false);
            fused.setAgreement(null);
            applyFontMetrics(fused, null);
            return;
        }
        if (vision == null) {
            fused.setFusedValue(ocr.getDetectedValue());
            fused.setFusedConfidence(ocr.getConfidence());
            fused.setPresent(ocr.isPresent());
            fused.setAgreement(null);
            applyFontMetrics(fused, ocr);
            return;
        }
        if (ocr == null) {
            fused.setFusedValue(vision.getDetectedValue());
            fused.setFusedConfidence(vision.getConfidence());
            fused.setPresent(vision.isPresent());
            fused.setAgreement(null);
            applyFontMetrics(fused, vision);
            return;
        }

        boolean agree = valuesRoughlyMatch(ocr.getDetectedValue(), vision.getDetectedValue())
                && ocr.isPresent() == vision.isPresent();
        fused.setAgreement(agree);

        LabelDetection higherConfidence = compareConfidence(ocr, vision) >= 0 ? ocr : vision;
        fused.setFusedValue(higherConfidence.getDetectedValue());
        fused.setPresent(ocr.isPresent() || vision.isPresent());
        applyFontMetrics(fused, higherConfidence);

        double ocrConf = toDouble(ocr.getConfidence());
        double visionConf = toDouble(vision.getConfidence());
        double fusedConfidence = agree
                ? Math.min(1.0, (ocrConf + visionConf) / 2.0 + AGREEMENT_CONFIDENCE_BONUS)
                : Math.max(ocrConf, visionConf) * DISAGREEMENT_CONFIDENCE_PENALTY_FACTOR;

        fused.setFusedConfidence(BigDecimal.valueOf(fusedConfidence));
    }

    /** Carries the font-analysis metrics (Step 6) and bounding box from whichever detection is winning fusedValue — see FusedDeclaration's Javadoc. */
    private void applyFontMetrics(FusedDeclaration fused, LabelDetection source) {
        fused.setFontSizeEstimate(source != null ? source.getFontSizeEstimate() : null);
        fused.setReadabilityScore(source != null ? source.getReadabilityScore() : null);
        fused.setContrastScore(source != null ? source.getContrastScore() : null);
        fused.setLabelSection(source != null ? source.getLabelSection() : null);
        fused.setBoundingBox(source != null ? source.getBoundingBox() : null);
    }

    private boolean valuesRoughlyMatch(String a, String b) {
        if (a == null || b == null) {
            return a == null && b == null;
        }
        String normalizedA = a.toLowerCase().replaceAll("[^a-z0-9]", "");
        String normalizedB = b.toLowerCase().replaceAll("[^a-z0-9]", "");
        if (normalizedA.isEmpty() || normalizedB.isEmpty()) {
            return normalizedA.equals(normalizedB);
        }
        return normalizedA.equals(normalizedB) || normalizedA.contains(normalizedB) || normalizedB.contains(normalizedA);
    }

    private int compareConfidence(LabelDetection a, LabelDetection b) {
        return Double.compare(toDouble(a.getConfidence()), toDouble(b.getConfidence()));
    }

    private double toDouble(BigDecimal value) {
        return value != null ? value.doubleValue() : 0.0;
    }
}
