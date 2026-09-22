package com.legalmetrology.vision.repository;

import com.legalmetrology.common.enums.DeclarationType;
import com.legalmetrology.common.enums.DetectionSource;
import com.legalmetrology.vision.entity.LabelDetection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LabelDetectionRepository extends JpaRepository<LabelDetection, UUID> {

    List<LabelDetection> findByImageId(UUID imageId);

    List<LabelDetection> findByImageIdIn(List<UUID> imageIds);

    Optional<LabelDetection> findByImageIdAndDeclarationTypeAndSource(
            UUID imageId, DeclarationType declarationType, DetectionSource source);

    List<LabelDetection> findByImageIdAndSource(UUID imageId, DetectionSource source);

    /** Detections are the *current* derived state for an image, not an audit log (the OCRResult table is) — re-analysis replaces them rather than accumulating duplicates. */
    @Transactional
    void deleteByImageIdAndSource(UUID imageId, DetectionSource source);
}
