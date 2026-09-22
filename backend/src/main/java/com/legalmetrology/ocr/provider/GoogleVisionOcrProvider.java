package com.legalmetrology.ocr.provider;

import com.fasterxml.jackson.databind.JsonNode;
import com.legalmetrology.exception.BadRequestException;
import com.legalmetrology.exception.ServiceNotConfiguredException;
import com.legalmetrology.ocr.config.GoogleCloudCredentialsProvider;
import com.legalmetrology.ocr.config.OcrProperties;
import com.legalmetrology.ocr.language.ScriptBasedLanguageDetector;
import com.legalmetrology.ocr.model.LanguageCode;
import com.legalmetrology.ocr.model.OcrExtractionResult;
import com.legalmetrology.ocr.model.OcrLine;
import com.legalmetrology.ocr.model.OcrParagraph;
import com.legalmetrology.ocr.model.OcrWord;
import com.legalmetrology.utils.ImageUtil;
import com.legalmetrology.vision.provider.VisionBoundingBox;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * {@link OcrProvider} backed by Google Cloud Vision's {@code DOCUMENT_TEXT_DETECTION}
 * feature — the primary OCR provider (see docs/ARCHITECTURE.md §7/§14).
 * Chosen over plain {@code TEXT_DETECTION} specifically because it returns
 * the paragraph→word hierarchy this module needs to preserve, not just a
 * flat text blob.
 * <p>
 * Vision returns word/paragraph boxes as absolute pixel {@code vertices},
 * not normalized coordinates, so this provider decodes the image once up
 * front purely to read its width/height and convert every box into the
 * same 0-1 relative {@link VisionBoundingBox} format Vision AI detections
 * use — one coordinate system across the whole system regardless of which
 * component produced a box.
 */
@Slf4j
@Component
public class GoogleVisionOcrProvider implements OcrProvider {

    private static final List<String> LANGUAGE_HINTS = List.of("en", "hi", "mr", "ta", "gu");

    private final WebClient googleVisionWebClient;
    private final OcrProperties properties;
    private final GoogleCloudCredentialsProvider credentialsProvider;

    public GoogleVisionOcrProvider(@Qualifier("googleVisionWebClient") WebClient googleVisionWebClient,
                                    OcrProperties properties,
                                    GoogleCloudCredentialsProvider credentialsProvider) {
        this.googleVisionWebClient = googleVisionWebClient;
        this.properties = properties;
        this.credentialsProvider = credentialsProvider;
    }

    @Override
    public String providerKey() {
        return "google-vision";
    }

    @Override
    public OcrExtractionResult extractText(byte[] imageBytes, String mimeType) {
        BufferedImage image = ImageUtil.read(new ByteArrayInputStream(imageBytes));
        int imageWidth = image.getWidth();
        int imageHeight = image.getHeight();

        Map<String, Object> requestBody = Map.of(
                "requests", List.of(Map.of(
                        "image", Map.of("content", Base64.getEncoder().encodeToString(imageBytes)),
                        "features", List.of(Map.of("type", "DOCUMENT_TEXT_DETECTION")),
                        "imageContext", Map.of("languageHints", LANGUAGE_HINTS)
                ))
        );

        JsonNode response = authenticatedRequest()
                .bodyValue(requestBody)
                .retrieve()
                .onStatus(status -> status.isError(), clientResponse -> clientResponse.bodyToMono(String.class)
                        .defaultIfEmpty("<empty response body>")
                        .flatMap(body -> Mono.error(new BadRequestException(
                                "Google Vision OCR request failed (" + clientResponse.statusCode() + "): " + body))))
                .bodyToMono(JsonNode.class)
                .block();

        return parse(response, imageWidth, imageHeight);
    }

    /**
     * Prefers a service-account bearer token (set up via {@code GOOGLE_APPLICATION_CREDENTIALS}
     * — see SETUP.md) over the simpler {@code GOOGLE_VISION_API_KEY} query
     * param when both are available, since that's the auth path Google
     * itself recommends for production use. Throws a clear, actionable
     * error rather than sending an unauthenticated request when neither is
     * configured.
     */
    private WebClient.RequestBodySpec authenticatedRequest() {
        Optional<String> accessToken = credentialsProvider.getAccessToken();
        if (accessToken.isPresent()) {
            WebClient.RequestBodySpec request = googleVisionWebClient.post()
                    .uri("/images:annotate")
                    .header("Authorization", "Bearer " + accessToken.get());
            String projectId = properties.googleVision().projectId();
            if (projectId != null && !projectId.isBlank()) {
                request = request.header("X-Goog-User-Project", projectId);
            }
            return request;
        }

        String apiKey = properties.googleVision().apiKey();
        if (apiKey == null || apiKey.isBlank()) {
            throw ServiceNotConfiguredException.apiKeyMissing("Google Vision");
        }
        return googleVisionWebClient.post()
                .uri(uriBuilder -> uriBuilder.path("/images:annotate").queryParam("key", apiKey).build());
    }

    private OcrExtractionResult parse(JsonNode response, int imageWidth, int imageHeight) {
        if (response == null) {
            throw new BadRequestException("Google Vision OCR returned an empty response");
        }
        JsonNode firstResponse = response.path("responses").path(0);
        if (firstResponse.has("error")) {
            throw new BadRequestException("Google Vision OCR error: " + firstResponse.path("error").path("message").asText());
        }

        JsonNode fullTextAnnotation = firstResponse.path("fullTextAnnotation");
        if (fullTextAnnotation.isMissingNode()) {
            return OcrExtractionResult.empty(providerKey());
        }

        String fullText = fullTextAnnotation.path("text").asText("");
        List<OcrParagraph> paragraphs = new ArrayList<>();
        List<Double> allConfidences = new ArrayList<>();

        for (JsonNode page : fullTextAnnotation.path("pages")) {
            for (JsonNode block : page.path("blocks")) {
                for (JsonNode paragraphNode : block.path("paragraphs")) {
                    OcrParagraph paragraph = parseParagraph(paragraphNode, imageWidth, imageHeight);
                    paragraphs.add(paragraph);
                    paragraph.lines().forEach(line -> line.words().forEach(w -> allConfidences.add(w.confidence())));
                }
            }
        }

        double overallConfidence = allConfidences.isEmpty() ? 0.0
                : allConfidences.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);

        LanguageCode detectedLanguage = paragraphs.stream()
                .map(OcrParagraph::language)
                .filter(lang -> lang != LanguageCode.UNKNOWN)
                .findFirst()
                .orElseGet(() -> ScriptBasedLanguageDetector.detect(fullText));

        return new OcrExtractionResult(providerKey(), detectedLanguage, fullText, paragraphs, overallConfidence, Instant.now());
    }

    private OcrParagraph parseParagraph(JsonNode paragraphNode, int imageWidth, int imageHeight) {
        List<WordInfo> words = new ArrayList<>();
        for (JsonNode wordNode : paragraphNode.path("words")) {
            words.add(parseWord(wordNode, imageWidth, imageHeight));
        }

        List<OcrLine> lines = groupIntoLines(words);
        String paragraphText = String.join("\n", lines.stream().map(OcrLine::text).toList());
        VisionBoundingBox paragraphBox = parseBoundingBox(paragraphNode.path("boundingBox"), imageWidth, imageHeight);
        double paragraphConfidence = lines.isEmpty() ? 0.0
                : lines.stream().mapToDouble(OcrLine::confidence).average().orElse(0.0);
        LanguageCode language = parseLanguage(paragraphNode);

        return new OcrParagraph(paragraphText, paragraphBox, paragraphConfidence, language, lines);
    }

    private record WordInfo(String text, VisionBoundingBox box, double confidence) {
    }

    private WordInfo parseWord(JsonNode wordNode, int imageWidth, int imageHeight) {
        StringBuilder text = new StringBuilder();
        for (JsonNode symbol : wordNode.path("symbols")) {
            text.append(symbol.path("text").asText(""));
        }
        VisionBoundingBox box = parseBoundingBox(wordNode.path("boundingBox"), imageWidth, imageHeight);
        double confidence = wordNode.path("confidence").asDouble(0.0);
        return new WordInfo(text.toString(), box, confidence);
    }

    /**
     * Vision's paragraph→word structure has no explicit "line" level, so
     * lines are reconstructed by clustering words whose vertical centers
     * fall within one word-height of each other, processed in reading
     * order (top-to-bottom, then left-to-right) — the standard approach
     * for turning a word-position list into text lines.
     */
    private List<OcrLine> groupIntoLines(List<WordInfo> words) {
        List<WordInfo> sorted = words.stream()
                .sorted(Comparator.comparingDouble((WordInfo w) -> w.box().y()).thenComparingDouble(w -> w.box().x()))
                .toList();

        List<List<WordInfo>> lineGroups = new ArrayList<>();
        List<WordInfo> currentLine = new ArrayList<>();
        double currentLineYCenter = 0;

        for (WordInfo word : sorted) {
            double yCenter = word.box().y() + word.box().h() / 2.0;
            double lineHeightTolerance = Math.max(word.box().h() * 0.6, 0.005);

            if (currentLine.isEmpty() || Math.abs(yCenter - currentLineYCenter) <= lineHeightTolerance) {
                currentLine.add(word);
                currentLineYCenter = currentLine.stream().mapToDouble(w -> w.box().y() + w.box().h() / 2.0).average().orElse(yCenter);
            } else {
                lineGroups.add(currentLine);
                currentLine = new ArrayList<>(List.of(word));
                currentLineYCenter = yCenter;
            }
        }
        if (!currentLine.isEmpty()) {
            lineGroups.add(currentLine);
        }

        return lineGroups.stream().map(this::buildLine).toList();
    }

    private OcrLine buildLine(List<WordInfo> lineWords) {
        List<WordInfo> orderedByX = lineWords.stream()
                .sorted(Comparator.comparingDouble(w -> w.box().x()))
                .toList();

        String text = String.join(" ", orderedByX.stream().map(WordInfo::text).toList());
        double confidence = orderedByX.stream().mapToDouble(WordInfo::confidence).average().orElse(0.0);
        VisionBoundingBox lineBox = unionBox(orderedByX.stream().map(WordInfo::box).toList());
        List<OcrWord> words = orderedByX.stream().map(w -> new OcrWord(w.text(), w.box(), w.confidence())).toList();

        return new OcrLine(text, lineBox, confidence, words);
    }

    private VisionBoundingBox unionBox(List<VisionBoundingBox> boxes) {
        if (boxes.isEmpty()) {
            return new VisionBoundingBox(0, 0, 0, 0);
        }
        double minX = boxes.stream().mapToDouble(VisionBoundingBox::x).min().orElse(0);
        double minY = boxes.stream().mapToDouble(VisionBoundingBox::y).min().orElse(0);
        double maxX = boxes.stream().mapToDouble(b -> b.x() + b.w()).max().orElse(0);
        double maxY = boxes.stream().mapToDouble(b -> b.y() + b.h()).max().orElse(0);
        return new VisionBoundingBox(minX, minY, maxX - minX, maxY - minY);
    }

    private VisionBoundingBox parseBoundingBox(JsonNode boundingBoxNode, int imageWidth, int imageHeight) {
        List<double[]> vertices = new ArrayList<>();
        for (JsonNode vertex : boundingBoxNode.path("vertices")) {
            vertices.add(new double[]{vertex.path("x").asDouble(0), vertex.path("y").asDouble(0)});
        }
        if (vertices.isEmpty() || imageWidth == 0 || imageHeight == 0) {
            return new VisionBoundingBox(0, 0, 0, 0);
        }
        double minX = vertices.stream().mapToDouble(v -> v[0]).min().orElse(0);
        double minY = vertices.stream().mapToDouble(v -> v[1]).min().orElse(0);
        double maxX = vertices.stream().mapToDouble(v -> v[0]).max().orElse(0);
        double maxY = vertices.stream().mapToDouble(v -> v[1]).max().orElse(0);

        return new VisionBoundingBox(minX / imageWidth, minY / imageHeight,
                (maxX - minX) / imageWidth, (maxY - minY) / imageHeight);
    }

    private LanguageCode parseLanguage(JsonNode nodeWithProperty) {
        JsonNode detectedLanguages = nodeWithProperty.path("property").path("detectedLanguages");
        if (detectedLanguages.isArray() && !detectedLanguages.isEmpty()) {
            return LanguageCode.fromGoogleVisionCode(detectedLanguages.get(0).path("languageCode").asText(null));
        }
        return LanguageCode.UNKNOWN;
    }
}
