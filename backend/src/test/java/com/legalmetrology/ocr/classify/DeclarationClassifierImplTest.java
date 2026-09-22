package com.legalmetrology.ocr.classify;

import com.legalmetrology.common.enums.DeclarationType;
import com.legalmetrology.ocr.classify.impl.DeclarationClassifierImpl;
import com.legalmetrology.ocr.model.LanguageCode;
import com.legalmetrology.ocr.model.OcrExtractionResult;
import com.legalmetrology.ocr.model.OcrLine;
import com.legalmetrology.ocr.model.OcrParagraph;
import com.legalmetrology.ocr.normalize.impl.OcrNormalizationServiceImpl;
import com.legalmetrology.vision.provider.VisionBoundingBox;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class DeclarationClassifierImplTest {

    private final DeclarationClassifierImpl classifier =
            new DeclarationClassifierImpl(new OcrNormalizationServiceImpl());

    @Test
    void classifiesNetQuantityFromALabelledLine() {
        List<ClassifiedField> fields = classify("Net Wt. 500g");

        assertThat(fields).anySatisfy(field -> {
            assertThat(field.declarationType()).isEqualTo(DeclarationType.NET_QUANTITY);
            assertThat(field.normalizedValue()).isEqualTo("500g");
        });
    }

    @Test
    void classifiesMrpAndNormalizesTheCurrency() {
        List<ClassifiedField> fields = classify("MRP: Rs.120 (incl. of all taxes)");

        assertThat(fields).anySatisfy(field -> {
            assertThat(field.declarationType()).isEqualTo(DeclarationType.MRP);
            assertThat(field.normalizedValue()).isEqualTo("₹120");
        });
    }

    @Test
    void classifiesPackerFromKeyword() {
        List<ClassifiedField> fields = classify("Packed By: ABC Foods Pvt Ltd, Pune");

        assertThat(fields).anySatisfy(field -> assertThat(field.declarationType()).isEqualTo(DeclarationType.PACKER));
    }

    @Test
    void classifiesEmailWithoutNeedingAKeyword() {
        List<ClassifiedField> fields = classify("For queries write to care@company.com");

        assertThat(fields).anySatisfy(field -> {
            assertThat(field.declarationType()).isEqualTo(DeclarationType.EMAIL);
            assertThat(field.normalizedValue()).isEqualTo("care@company.com");
        });
    }

    @Test
    void classifiesMfgDateAsBothMonthAndYear() {
        List<ClassifiedField> fields = classify("Mfg Date: 12/2025");

        assertThat(fields).anySatisfy(field -> {
            assertThat(field.declarationType()).isEqualTo(DeclarationType.MFG_MONTH);
            assertThat(field.normalizedValue()).isEqualTo("2025-12");
        });
        assertThat(fields).anySatisfy(field -> {
            assertThat(field.declarationType()).isEqualTo(DeclarationType.MFG_YEAR);
            assertThat(field.normalizedValue()).isEqualTo("2025-12");
        });
    }

    @Test
    void classifiesLicenseNumberFromFssaiKeyword() {
        List<ClassifiedField> fields = classify("FSSAI Lic No 12345678901234");

        assertThat(fields).anySatisfy(field -> assertThat(field.declarationType()).isEqualTo(DeclarationType.LICENSE_NUMBER));
    }

    @Test
    void aLineMatchingNoKeywordProducesNoFields() {
        List<ClassifiedField> fields = classify("Ingredients: Wheat Flour, Sugar, Edible Vegetable Oil");

        assertThat(fields).isEmpty();
    }

    private List<ClassifiedField> classify(String lineText) {
        VisionBoundingBox box = new VisionBoundingBox(0.1, 0.1, 0.5, 0.05);
        OcrLine line = new OcrLine(lineText, box, 0.95, List.of());
        OcrParagraph paragraph = new OcrParagraph(lineText, box, 0.95, LanguageCode.EN, List.of(line));
        OcrExtractionResult extraction = new OcrExtractionResult(
                "google-vision", LanguageCode.EN, lineText, List.of(paragraph), 0.95, Instant.now());
        return classifier.classify(extraction);
    }
}
