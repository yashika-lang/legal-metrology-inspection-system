package com.legalmetrology.vision.quality.impl;

import com.legalmetrology.inspection.entity.Image;
import com.legalmetrology.inspection.repository.ImageRepository;
import com.legalmetrology.utils.ImageUtil;
import com.legalmetrology.vision.quality.ImageQualityAnalyzer;
import com.legalmetrology.vision.quality.ImageQualityService;
import com.legalmetrology.vision.quality.QualityAnalysisResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;

@Slf4j
@Service
@RequiredArgsConstructor
public class ImageQualityServiceImpl implements ImageQualityService {

    private final ImageQualityAnalyzer imageQualityAnalyzer;
    private final ImageRepository imageRepository;

    @Override
    @Transactional
    public QualityAnalysisResult analyzeAndPersist(Image image, byte[] imageBytes) {
        var buffered = ImageUtil.read(new ByteArrayInputStream(imageBytes));
        QualityAnalysisResult result = imageQualityAnalyzer.analyze(buffered);

        image.setQualityScore(BigDecimal.valueOf(result.score()));
        image.setQualityWarnings(String.join(",", result.warningNames()));
        image.setRecommendedAction(result.recommendedAction());
        imageRepository.save(image);

        log.info("Image quality analyzed: imageId={} score={} warnings={} action={}",
                image.getId(), result.score(), result.warningNames(), result.recommendedAction());

        return result;
    }
}
