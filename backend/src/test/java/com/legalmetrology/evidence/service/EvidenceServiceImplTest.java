package com.legalmetrology.evidence.service;

import com.legalmetrology.common.enums.DeclarationType;
import com.legalmetrology.common.enums.Severity;
import com.legalmetrology.evidence.annotate.AnnotatedImageService;
import com.legalmetrology.evidence.entity.Evidence;
import com.legalmetrology.evidence.hash.EvidenceHasher;
import com.legalmetrology.evidence.repository.EvidenceRepository;
import com.legalmetrology.evidence.service.impl.EvidenceServiceImpl;
import com.legalmetrology.exception.BadRequestException;
import com.legalmetrology.inspection.entity.Image;
import com.legalmetrology.inspection.entity.Inspection;
import com.legalmetrology.inspection.entity.Rule;
import com.legalmetrology.inspection.entity.Violation;
import com.legalmetrology.inspection.repository.InspectionRepository;
import com.legalmetrology.inspection.repository.ViolationRepository;
import com.legalmetrology.ocr.entity.FusedDeclaration;
import com.legalmetrology.ocr.repository.FusedDeclarationRepository;
import com.legalmetrology.storage.dto.StorageBucket;
import com.legalmetrology.storage.service.StorageService;
import com.legalmetrology.trace.service.DecisionTraceService;
import com.legalmetrology.vision.entity.LabelDetection;
import com.legalmetrology.vision.repository.LabelDetectionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EvidenceServiceImplTest {

    @Mock
    private InspectionRepository inspectionRepository;
    @Mock
    private ViolationRepository violationRepository;
    @Mock
    private FusedDeclarationRepository fusedDeclarationRepository;
    @Mock
    private LabelDetectionRepository labelDetectionRepository;
    @Mock
    private StorageService storageService;
    @Mock
    private AnnotatedImageService annotatedImageService;
    @Mock
    private EvidenceHasher evidenceHasher;
    @Mock
    private EvidenceRepository evidenceRepository;
    @Mock
    private DecisionTraceService decisionTraceService;

    private EvidenceServiceImpl evidenceService;
    private byte[] realPngBytes;

    @BeforeEach
    void setUp() throws Exception {
        evidenceService = new EvidenceServiceImpl(inspectionRepository, violationRepository, fusedDeclarationRepository,
                labelDetectionRepository, storageService, annotatedImageService, evidenceHasher, evidenceRepository,
                decisionTraceService);

        BufferedImage image = new BufferedImage(10, 10, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(image, "png", out);
        realPngBytes = out.toByteArray();

        lenient().when(evidenceHasher.hash(any(), any())).thenReturn("deadbeef");
        lenient().when(evidenceRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        lenient().when(annotatedImageService.annotate(any(), any())).thenReturn(new byte[]{1});
        lenient().when(annotatedImageService.crop(any(), any())).thenReturn(new byte[]{2});
        lenient().when(storageService.download(any(), any())).thenReturn(realPngBytes);
    }

    @Test
    void generateForViolation_throwsWhenExistingEvidenceIsImmutable() {
        Violation violation = violation(UUID.randomUUID(), DeclarationType.MRP);
        when(violationRepository.findById(violation.getId())).thenReturn(Optional.of(violation));

        Evidence immutable = Evidence.builder().immutable(true).build();
        when(evidenceRepository.findByViolationId(violation.getId())).thenReturn(Optional.of(immutable));

        assertThatThrownBy(() -> evidenceService.generateForViolation(violation.getId()))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("immutable");

        verifyNoInteractions(storageService, annotatedImageService);
    }

    @Test
    void generateForViolation_choosesTheHigherConfidenceVisionDetectionWhenVisionWins() {
        Violation violation = violation(UUID.randomUUID(), DeclarationType.MRP);
        when(violationRepository.findById(violation.getId())).thenReturn(Optional.of(violation));
        when(evidenceRepository.findByViolationId(violation.getId())).thenReturn(Optional.empty());

        UUID ocrDetectionId = UUID.randomUUID();
        UUID visionDetectionId = UUID.randomUUID();
        Image ocrImage = imageWithPath("ocr-image.png");
        Image visionImage = imageWithPath("vision-image.png");

        FusedDeclaration fused = FusedDeclaration.builder()
                .ocrConfidence(BigDecimal.valueOf(0.60))
                .visionConfidence(BigDecimal.valueOf(0.95))
                .ocrSourceDetectionId(ocrDetectionId)
                .visionSourceDetectionId(visionDetectionId)
                .boundingBox("{\"x\":0.1,\"y\":0.1,\"w\":0.2,\"h\":0.2}")
                .build();
        when(fusedDeclarationRepository.findByInspectionIdAndDeclarationType(violation.getInspection().getId(), DeclarationType.MRP))
                .thenReturn(Optional.of(fused));

        when(labelDetectionRepository.findById(visionDetectionId))
                .thenReturn(Optional.of(LabelDetection.builder().image(visionImage).build()));
        lenient().when(labelDetectionRepository.findById(ocrDetectionId))
                .thenReturn(Optional.of(LabelDetection.builder().image(ocrImage).build()));

        Evidence result = evidenceService.generateForViolation(violation.getId());

        assertThat(result.getImage()).isEqualTo(visionImage);
        verify(storageService).download(StorageBucket.IMAGES, "vision-image.png");
    }

    @Test
    void generateForViolation_choosesOcrDetectionWhenOcrConfidenceIsHigher() {
        Violation violation = violation(UUID.randomUUID(), DeclarationType.MRP);
        when(violationRepository.findById(violation.getId())).thenReturn(Optional.of(violation));
        when(evidenceRepository.findByViolationId(violation.getId())).thenReturn(Optional.empty());

        UUID ocrDetectionId = UUID.randomUUID();
        UUID visionDetectionId = UUID.randomUUID();
        Image ocrImage = imageWithPath("ocr-image.png");

        FusedDeclaration fused = FusedDeclaration.builder()
                .ocrConfidence(BigDecimal.valueOf(0.91))
                .visionConfidence(BigDecimal.valueOf(0.40))
                .ocrSourceDetectionId(ocrDetectionId)
                .visionSourceDetectionId(visionDetectionId)
                .boundingBox("{\"x\":0.1,\"y\":0.1,\"w\":0.2,\"h\":0.2}")
                .build();
        when(fusedDeclarationRepository.findByInspectionIdAndDeclarationType(violation.getInspection().getId(), DeclarationType.MRP))
                .thenReturn(Optional.of(fused));
        when(labelDetectionRepository.findById(ocrDetectionId))
                .thenReturn(Optional.of(LabelDetection.builder().image(ocrImage).build()));

        Evidence result = evidenceService.generateForViolation(violation.getId());

        assertThat(result.getImage()).isEqualTo(ocrImage);
        verify(storageService).download(StorageBucket.IMAGES, "ocr-image.png");
    }

    @Test
    void generateForViolation_withNoDeclarationFieldSkipsImageResolutionEntirely() {
        Violation violation = violation(UUID.randomUUID(), null);
        when(violationRepository.findById(violation.getId())).thenReturn(Optional.of(violation));
        when(evidenceRepository.findByViolationId(violation.getId())).thenReturn(Optional.empty());

        Evidence result = evidenceService.generateForViolation(violation.getId());

        assertThat(result.getImage()).isNull();
        assertThat(result.getOriginalImagePath()).isNull();
        assertThat(result.getAnnotatedImagePath()).isNull();
        verifyNoInteractions(fusedDeclarationRepository, storageService, annotatedImageService);
    }

    @Test
    void generateForViolation_withNoFusedDeclarationFoundHasNoImageButStillSucceeds() {
        Violation violation = violation(UUID.randomUUID(), DeclarationType.MRP);
        when(violationRepository.findById(violation.getId())).thenReturn(Optional.of(violation));
        when(evidenceRepository.findByViolationId(violation.getId())).thenReturn(Optional.empty());
        when(fusedDeclarationRepository.findByInspectionIdAndDeclarationType(violation.getInspection().getId(), DeclarationType.MRP))
                .thenReturn(Optional.empty());

        Evidence result = evidenceService.generateForViolation(violation.getId());

        assertThat(result.getImage()).isNull();
        assertThat(result.getReason()).isEqualTo(violation.getDescription());
        verifyNoInteractions(storageService, annotatedImageService);
    }

    private Violation violation(UUID id, DeclarationType field) {
        Inspection inspection = Inspection.builder().build();
        setId(inspection, UUID.randomUUID());

        Rule rule = Rule.builder().ruleCode("LM-MRP-001").build();
        setId(rule, UUID.randomUUID());

        Violation violation = Violation.builder()
                .inspection(inspection)
                .rule(rule)
                .severity(Severity.MAJOR)
                .field(field)
                .description("MRP is missing required wording")
                .actualValue("Rs 100.00")
                .expectedValue("Rs 100.00 (incl. of all taxes)")
                .confidence(BigDecimal.valueOf(0.9))
                .suggestedFix("Add inclusive-of-taxes wording")
                .legalReference("Rule 6")
                .build();
        setId(violation, id);
        return violation;
    }

    private Image imageWithPath(String path) {
        Image image = Image.builder().storagePath(path).build();
        setId(image, UUID.randomUUID());
        return image;
    }

    private void setId(com.legalmetrology.common.entity.BaseEntity entity, UUID id) {
        entity.setId(id);
    }
}
