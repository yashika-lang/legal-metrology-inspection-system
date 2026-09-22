package com.legalmetrology.ocr.fusion;

import com.legalmetrology.common.enums.DeclarationType;
import com.legalmetrology.common.enums.DetectionSource;
import com.legalmetrology.inspection.entity.Image;
import com.legalmetrology.inspection.entity.Inspection;
import com.legalmetrology.inspection.repository.ImageRepository;
import com.legalmetrology.inspection.repository.InspectionRepository;
import com.legalmetrology.ocr.entity.FusedDeclaration;
import com.legalmetrology.ocr.fusion.impl.DeclarationFusionServiceImpl;
import com.legalmetrology.ocr.repository.FusedDeclarationRepository;
import com.legalmetrology.product.entity.Product;
import com.legalmetrology.product.entity.ProductDeclaration;
import com.legalmetrology.product.repository.ProductDeclarationRepository;
import com.legalmetrology.trace.service.DecisionTraceService;
import com.legalmetrology.vision.entity.LabelDetection;
import com.legalmetrology.vision.repository.LabelDetectionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

/**
 * Covers the barcode-first override added to fusion: a scanned product's
 * verified {@link ProductDeclaration} rows must win over whatever OCR/Vision
 * AI produced for the same declaration type, while any declaration type the
 * product doesn't cover must still fuse from OCR/Vision exactly as before
 * (regression coverage for the pre-existing present-over-absent logic this
 * override sits on top of).
 */
@ExtendWith(MockitoExtension.class)
class DeclarationFusionServiceImplTest {

    @Mock
    private InspectionRepository inspectionRepository;
    @Mock
    private ImageRepository imageRepository;
    @Mock
    private LabelDetectionRepository labelDetectionRepository;
    @Mock
    private FusedDeclarationRepository fusedDeclarationRepository;
    @Mock
    private ProductDeclarationRepository productDeclarationRepository;
    @Mock
    private DecisionTraceService decisionTraceService;

    private DeclarationFusionServiceImpl fusionService;
    private Inspection inspection;

    @BeforeEach
    void setUp() {
        fusionService = new DeclarationFusionServiceImpl(inspectionRepository, imageRepository, labelDetectionRepository,
                fusedDeclarationRepository, productDeclarationRepository, decisionTraceService);

        inspection = Inspection.builder().build();
        inspection.setId(UUID.randomUUID());

        when(inspectionRepository.findById(inspection.getId())).thenReturn(Optional.of(inspection));
        when(imageRepository.findByInspectionId(inspection.getId())).thenReturn(List.of());
        lenient().when(labelDetectionRepository.findByImageIdIn(any())).thenReturn(List.of());
        lenient().when(fusedDeclarationRepository.findByInspectionIdAndDeclarationType(any(), any())).thenReturn(Optional.empty());
        when(fusedDeclarationRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void aProductDeclarationOverridesWhateverOcrVisionFoundForTheSameField() {
        Product product = Product.builder().build();
        product.setId(UUID.randomUUID());
        inspection.setProduct(product);

        when(productDeclarationRepository.findByProductId(product.getId())).thenReturn(List.of(
                ProductDeclaration.builder().product(product).declarationType(DeclarationType.MRP).value("₹415").build()));

        Image image = Image.builder().build();
        image.setId(UUID.randomUUID());
        when(imageRepository.findByInspectionId(inspection.getId())).thenReturn(List.of(image));

        LabelDetection lowConfidenceOcrMrp = LabelDetection.builder()
                .image(image).declarationType(DeclarationType.MRP).source(DetectionSource.OCR_TEXT)
                .detectedValue("₹99").confidence(BigDecimal.valueOf(0.4)).present(true).build();
        when(labelDetectionRepository.findByImageIdIn(List.of(image.getId()))).thenReturn(List.of(lowConfidenceOcrMrp));

        List<FusedDeclaration> result = fusionService.fuseInspection(inspection.getId());

        FusedDeclaration mrp = result.stream().filter(f -> f.getDeclarationType() == DeclarationType.MRP).findFirst().orElseThrow();
        assertThat(mrp.getFusedValue()).isEqualTo("₹415");
        assertThat(mrp.getFusedConfidence()).isEqualByComparingTo(BigDecimal.ONE);
        assertThat(mrp.isPresent()).isTrue();
        assertThat(mrp.isFromProductDatabase()).isTrue();
        // The OCR read is kept as informational context, not discarded, even though it lost.
        assertThat(mrp.getOcrValue()).isEqualTo("₹99");
    }

    @Test
    void aDeclarationTypeNotCoveredByTheProductStillFusesNormallyFromOcrVision() {
        Product product = Product.builder().build();
        product.setId(UUID.randomUUID());
        inspection.setProduct(product);

        // The product only knows MRP — NET_QUANTITY must fall through to normal OCR/Vision fusion.
        when(productDeclarationRepository.findByProductId(product.getId())).thenReturn(List.of(
                ProductDeclaration.builder().product(product).declarationType(DeclarationType.MRP).value("₹415").build()));

        Image image = Image.builder().build();
        image.setId(UUID.randomUUID());
        when(imageRepository.findByInspectionId(inspection.getId())).thenReturn(List.of(image));

        LabelDetection netQtyOcr = LabelDetection.builder()
                .image(image).declarationType(DeclarationType.NET_QUANTITY).source(DetectionSource.OCR_TEXT)
                .detectedValue("450 g").confidence(BigDecimal.valueOf(0.9)).present(true).build();
        when(labelDetectionRepository.findByImageIdIn(List.of(image.getId()))).thenReturn(List.of(netQtyOcr));

        List<FusedDeclaration> result = fusionService.fuseInspection(inspection.getId());

        FusedDeclaration netQty = result.stream().filter(f -> f.getDeclarationType() == DeclarationType.NET_QUANTITY).findFirst().orElseThrow();
        assertThat(netQty.getFusedValue()).isEqualTo("450 g");
        assertThat(netQty.isFromProductDatabase()).isFalse();
    }

    @Test
    void withNoLinkedProductFusionBehavesExactlyAsBefore() {
        assertThat(inspection.getProduct()).isNull();

        Image image = Image.builder().build();
        image.setId(UUID.randomUUID());
        when(imageRepository.findByInspectionId(inspection.getId())).thenReturn(List.of(image));

        LabelDetection mrpOcr = LabelDetection.builder()
                .image(image).declarationType(DeclarationType.MRP).source(DetectionSource.OCR_TEXT)
                .detectedValue("₹99").confidence(BigDecimal.valueOf(0.8)).present(true).build();
        when(labelDetectionRepository.findByImageIdIn(List.of(image.getId()))).thenReturn(List.of(mrpOcr));

        List<FusedDeclaration> result = fusionService.fuseInspection(inspection.getId());

        FusedDeclaration mrp = result.stream().filter(f -> f.getDeclarationType() == DeclarationType.MRP).findFirst().orElseThrow();
        assertThat(mrp.getFusedValue()).isEqualTo("₹99");
        assertThat(mrp.isFromProductDatabase()).isFalse();
    }
}
