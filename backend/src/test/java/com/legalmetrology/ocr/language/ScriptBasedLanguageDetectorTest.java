package com.legalmetrology.ocr.language;

import com.legalmetrology.ocr.model.LanguageCode;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ScriptBasedLanguageDetectorTest {

    @Test
    void detectsEnglishFromLatinScript() {
        assertThat(ScriptBasedLanguageDetector.detect("Net Weight 500g Manufactured By ABC Foods"))
                .isEqualTo(LanguageCode.EN);
    }

    @Test
    void detectsDevanagariScriptAsHindi() {
        assertThat(ScriptBasedLanguageDetector.detect("शुद्ध वजन 500 ग्राम")).isEqualTo(LanguageCode.HI);
    }

    @Test
    void detectsTamilScript() {
        assertThat(ScriptBasedLanguageDetector.detect("நிகர எடை 500 கிராம்")).isEqualTo(LanguageCode.TA);
    }

    @Test
    void detectsGujaratiScript() {
        assertThat(ScriptBasedLanguageDetector.detect("ચોખ્ખું વજન 500 ગ્રામ")).isEqualTo(LanguageCode.GU);
    }

    @Test
    void returnsUnknownForBlankOrPurelyNumericText() {
        assertThat(ScriptBasedLanguageDetector.detect("")).isEqualTo(LanguageCode.UNKNOWN);
        assertThat(ScriptBasedLanguageDetector.detect("   ")).isEqualTo(LanguageCode.UNKNOWN);
        assertThat(ScriptBasedLanguageDetector.detect("12345 67890")).isEqualTo(LanguageCode.UNKNOWN);
    }
}
