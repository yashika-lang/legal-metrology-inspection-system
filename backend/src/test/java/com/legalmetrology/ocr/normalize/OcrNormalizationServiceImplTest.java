package com.legalmetrology.ocr.normalize;

import com.legalmetrology.common.enums.DeclarationType;
import com.legalmetrology.ocr.normalize.impl.OcrNormalizationServiceImpl;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class OcrNormalizationServiceImplTest {

    private final OcrNormalizationServiceImpl normalizer = new OcrNormalizationServiceImpl();

    @Test
    void currencyVariantsAllNormalizeToTheSameCanonicalForm() {
        assertThat(normalizer.normalizeCurrency("Rs.120")).isEqualTo("₹120");
        assertThat(normalizer.normalizeCurrency("₹120")).isEqualTo("₹120");
        assertThat(normalizer.normalizeCurrency("INR 120")).isEqualTo("₹120");
        assertThat(normalizer.normalizeCurrency("Rs 120.50")).isEqualTo("₹120.50");
        assertThat(normalizer.normalizeCurrency("MRP: Rs. 1,299")).isEqualTo("₹1299");
    }

    @Test
    void unitVariantsNormalizeToCanonicalAbbreviations() {
        assertThat(normalizer.normalizeUnit("500 gm")).isEqualTo("500g");
        assertThat(normalizer.normalizeUnit("500gms")).isEqualTo("500g");
        assertThat(normalizer.normalizeUnit("0.5kg")).isEqualTo("0.5kg");
        assertThat(normalizer.normalizeUnit("1 litre")).isEqualTo("1l");
        assertThat(normalizer.normalizeUnit("250 ml")).isEqualTo("250ml");
    }

    @Test
    void monthYearVariantsNormalizeToIsoStyleYearMonth() {
        assertThat(normalizer.normalizeMonthYear("12/2025")).isEqualTo("2025-12");
        assertThat(normalizer.normalizeMonthYear("Dec 2025")).isEqualTo("2025-12");
        assertThat(normalizer.normalizeMonthYear("December-25")).isEqualTo("2025-12");
    }

    @Test
    void phoneNumbersAreNormalizedToE164StyleForTenDigitIndianMobiles() {
        assertThat(normalizer.normalizePhone("9876543210")).isEqualTo("+919876543210");
        assertThat(normalizer.normalizePhone("+91 98765 43210")).isEqualTo("+919876543210");
        assertThat(normalizer.normalizePhone("91-9876543210")).isEqualTo("+919876543210");
    }

    @Test
    void emailIsExtractedAndLowercased() {
        assertThat(normalizer.normalizeEmail("Contact us at CARE@Company.CO.IN for help"))
                .isEqualTo("care@company.co.in");
    }

    @Test
    void pinCodeIsExtractedAsASixDigitToken() {
        assertThat(normalizer.normalizePinCode("Mumbai, Maharashtra - 400001")).isEqualTo("400001");
        assertThat(normalizer.normalizePinCode("No pin code here")).isNull();
    }

    @Test
    void dispatchesToTheRightNormalizerByDeclarationType() {
        assertThat(normalizer.normalizeForDeclarationType(DeclarationType.MRP, "Rs.120")).isEqualTo("₹120");
        assertThat(normalizer.normalizeForDeclarationType(DeclarationType.NET_QUANTITY, "500 gm")).isEqualTo("500g");
        assertThat(normalizer.normalizeForDeclarationType(DeclarationType.EMAIL, "CARE@Company.com")).isEqualTo("care@company.com");
    }
}
