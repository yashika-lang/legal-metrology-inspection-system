package com.legalmetrology.inspection.repository;

import com.legalmetrology.inspection.entity.Image;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ImageRepository extends JpaRepository<Image, UUID> {

    List<Image> findByInspectionId(UUID inspectionId);
}
