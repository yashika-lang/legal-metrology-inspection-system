package com.legalmetrology.ocr.provider;

import com.legalmetrology.exception.BadRequestException;
import com.legalmetrology.ocr.config.OcrProperties;
import com.legalmetrology.ocr.language.ScriptBasedLanguageDetector;
import com.legalmetrology.ocr.model.LanguageCode;
import com.legalmetrology.ocr.model.OcrExtractionResult;
import com.legalmetrology.ocr.model.OcrLine;
import com.legalmetrology.ocr.model.OcrParagraph;
import com.legalmetrology.ocr.model.OcrWord;
import com.legalmetrology.utils.ImageUtil;
import com.legalmetrology.vision.provider.VisionBoundingBox;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.sourceforge.tess4j.ITessAPI;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;
import net.sourceforge.tess4j.Word;
import org.springframework.stereotype.Component;

import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * {@link OcrProvider} backed by a locally installed Tesseract engine (via
 * Tess4J/JNA) — the offline fallback used when Google Vision is unavailable
 * or over quota, so field inspections keep working without connectivity
 * (see docs/ARCHITECTURE.md §14). Requires the {@code tesseract} native
 * library and the {@code eng+hin+mar+tam+guj} trained-data files to be
 * present at {@code ai.ocr.tesseract.data-path} on the host; if they're
 * missing, {@link #extractText} fails fast with a clear error rather than
 * silently returning nothing.
 * <p>
 * Unlike Google Vision, Tesseract has no reliable per-word language
 * auto-detection, so language is inferred after the fact from the Unicode
 * script of the recognized text via {@link ScriptBasedLanguageDetector}.
 * Paragraph/line grouping is reconstructed from Tesseract's own
 * block/line/word iterator levels rather than Google's native hierarchy.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TesseractOcrProvider implements OcrProvider {

    private final OcrProperties properties;

    @Override
    public String providerKey() {
        return "tesseract";
    }

    @Override
    public OcrExtractionResult extractText(byte[] imageBytes, String mimeType) {
        BufferedImage image = ImageUtil.read(new ByteArrayInputStream(imageBytes));
        int imageWidth = image.getWidth();
        int imageHeight = image.getHeight();

        Tesseract tesseract = buildEngine();

        try {
            List<Word> paragraphWords = tesseract.getWords(image, ITessAPI.TessPageIteratorLevel.RIL_PARA);
            List<Word> lineWords = tesseract.getWords(image, ITessAPI.TessPageIteratorLevel.RIL_TEXTLINE);
            List<Word> words = tesseract.getWords(image, ITessAPI.TessPageIteratorLevel.RIL_WORD);

            List<OcrParagraph> paragraphs = assembleHierarchy(paragraphWords, lineWords, words, imageWidth, imageHeight);
            String fullText = String.join("\n\n", paragraphs.stream().map(OcrParagraph::text).toList());

            double overallConfidence = words.isEmpty() ? 0.0
                    : words.stream().mapToDouble(w -> w.getConfidence() / 100.0).average().orElse(0.0);

            LanguageCode detectedLanguage = ScriptBasedLanguageDetector.detect(fullText);

            return new OcrExtractionResult(providerKey(), detectedLanguage, fullText, paragraphs, overallConfidence, Instant.now());
        } catch (RuntimeException ex) {
            log.error("Tesseract OCR failed", ex);
            throw new BadRequestException("Tesseract OCR failed: " + rootCauseMessage(ex));
        }
    }

    private Tesseract buildEngine() {
        Tesseract tesseract = new Tesseract();
        tesseract.setDatapath(properties.tesseract().dataPath());
        tesseract.setLanguage(properties.tesseract() != null && properties.tesseract().language() != null
                ? properties.tesseract().language()
                : LanguageCode.tesseractCombinedLanguageString());
        return tesseract;
    }

    /**
     * Reconstructs the paragraph→line→word tree from three independent
     * flat lists (Tesseract reports one entry per element at each
     * requested iterator level, with no parent/child linkage) by assigning
     * each line to the paragraph whose box contains its center, and each
     * word to the line whose box contains its center.
     */
    private List<OcrParagraph> assembleHierarchy(List<Word> paragraphWords, List<Word> lineWords,
                                                   List<Word> words, int imageWidth, int imageHeight) {
        List<OcrParagraph> paragraphs = new ArrayList<>();

        for (Word paragraphWord : paragraphWords) {
            Rectangle paragraphRect = paragraphWord.getBoundingBox();

            List<Word> linesInParagraph = lineWords.stream()
                    .filter(line -> paragraphRect.contains(centerOf(line.getBoundingBox())))
                    .sorted(Comparator.comparingInt(w -> w.getBoundingBox().y))
                    .toList();

            List<OcrLine> lines = new ArrayList<>();
            for (Word lineWord : linesInParagraph) {
                Rectangle lineRect = lineWord.getBoundingBox();
                List<Word> wordsInLine = words.stream()
                        .filter(w -> lineRect.contains(centerOf(w.getBoundingBox())))
                        .sorted(Comparator.comparingInt(w -> w.getBoundingBox().x))
                        .toList();

                List<OcrWord> ocrWords = wordsInLine.stream()
                        .map(w -> new OcrWord(w.getText().trim(), toRelativeBox(w.getBoundingBox(), imageWidth, imageHeight), w.getConfidence() / 100.0))
                        .toList();

                double lineConfidence = ocrWords.isEmpty() ? lineWord.getConfidence() / 100.0
                        : ocrWords.stream().mapToDouble(OcrWord::confidence).average().orElse(0.0);

                lines.add(new OcrLine(lineWord.getText().trim(), toRelativeBox(lineRect, imageWidth, imageHeight), lineConfidence, ocrWords));
            }

            double paragraphConfidence = lines.isEmpty() ? paragraphWord.getConfidence() / 100.0
                    : lines.stream().mapToDouble(OcrLine::confidence).average().orElse(0.0);

            String paragraphText = paragraphWord.getText().trim();
            LanguageCode language = ScriptBasedLanguageDetector.detect(paragraphText);

            paragraphs.add(new OcrParagraph(paragraphText, toRelativeBox(paragraphRect, imageWidth, imageHeight),
                    paragraphConfidence, language, lines));
        }

        return paragraphs;
    }

    private java.awt.Point centerOf(Rectangle rectangle) {
        return new java.awt.Point(rectangle.x + rectangle.width / 2, rectangle.y + rectangle.height / 2);
    }

    private VisionBoundingBox toRelativeBox(Rectangle rectangle, int imageWidth, int imageHeight) {
        if (imageWidth == 0 || imageHeight == 0) {
            return new VisionBoundingBox(0, 0, 0, 0);
        }
        return new VisionBoundingBox(
                rectangle.x / (double) imageWidth,
                rectangle.y / (double) imageHeight,
                rectangle.width / (double) imageWidth,
                rectangle.height / (double) imageHeight
        );
    }

    private String rootCauseMessage(Throwable throwable) {
        Throwable cause = throwable;
        while (cause.getCause() != null) {
            cause = cause.getCause();
        }
        return cause instanceof TesseractException ? cause.getMessage() : throwable.getMessage();
    }
}
