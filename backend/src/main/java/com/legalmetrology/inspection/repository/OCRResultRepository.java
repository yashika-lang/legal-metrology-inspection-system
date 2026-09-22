package com.legalmetrology.inspection.repository;

import com.legalmetrology.inspection.entity.OCRResult;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OCRResultRepository extends JpaRepository<OCRResult, UUID> {

    List<OCRResult> findByImageId(UUID imageId);

    /** Full run history for an image, most recent first — the OCR audit trail. */
    List<OCRResult> findByImageIdOrderByCreatedAtDesc(UUID imageId);

    Optional<OCRResult> findFirstByImageIdOrderByCreatedAtDesc(UUID imageId);
}
