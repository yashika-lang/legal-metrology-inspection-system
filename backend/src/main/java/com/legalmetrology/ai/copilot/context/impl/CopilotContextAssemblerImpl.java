package com.legalmetrology.ai.copilot.context.impl;

import com.legalmetrology.ai.copilot.context.CopilotContext;
import com.legalmetrology.ai.copilot.context.CopilotContextAssembler;
import com.legalmetrology.exception.ResourceNotFoundException;
import com.legalmetrology.inspection.entity.Image;
import com.legalmetrology.inspection.entity.Inspection;
import com.legalmetrology.inspection.entity.Violation;
import com.legalmetrology.inspection.repository.ImageRepository;
import com.legalmetrology.inspection.repository.InspectionRepository;
import com.legalmetrology.inspection.repository.ViolationRepository;
import com.legalmetrology.ocr.entity.FusedDeclaration;
import com.legalmetrology.ocr.fusion.DeclarationFusionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * The one class in {@code ai.copilot} that reads {@code inspection}/{@code ocr}
 * repositories directly — everything above this (prompt building, LLM
 * calls, response parsing) works only with {@link CopilotContext}, which
 * carries no OCR/Vision provider information at all.
 */
@Component
@RequiredArgsConstructor
public class CopilotContextAssemblerImpl implements CopilotContextAssembler {

    private final InspectionRepository inspectionRepository;
    private final ViolationRepository violationRepository;
    private final ImageRepository imageRepository;
    private final DeclarationFusionService declarationFusionService;

    @Override
    @Transactional(readOnly = true)
    public CopilotContext assemble(UUID inspectionId) {
        Inspection inspection = inspectionRepository.findById(inspectionId)
                .orElseThrow(() -> ResourceNotFoundException.of("Inspection", inspectionId));

        var violations = violationRepository.findByInspectionId(inspectionId).stream()
                .map(this::toViolationSummary)
                .toList();

        var declarations = declarationFusionService.getFusedDeclarations(inspectionId).stream()
                .map(this::toDeclarationSummary)
                .toList();

        var images = imageRepository.findByInspectionId(inspectionId).stream()
                .map(this::toImageQualitySummary)
                .toList();

        return new CopilotContext(
                inspectionId,
                inspection.getProduct() != null ? inspection.getProduct().getName() : "Unknown product",
                inspection.getProduct() != null && inspection.getProduct().getManufacturer() != null
                        ? inspection.getProduct().getManufacturer().getName() : "Unknown manufacturer",
                inspection.getProduct() != null && inspection.getProduct().getCategory() != null
                        ? inspection.getProduct().getCategory().getName() : "Uncategorized",
                inspection.getStatus() != null ? inspection.getStatus().name() : null,
                inspection.getComplianceScore(),
                inspection.getFraudRisk() != null ? inspection.getFraudRisk().name() : "UNKNOWN",
                violations,
                declarations,
                images
        );
    }

    private CopilotContext.ViolationSummary toViolationSummary(Violation violation) {
        return new CopilotContext.ViolationSummary(
                violation.getRule().getRuleCode(),
                violation.getRule().getTitle(),
                violation.getSeverity() != null ? violation.getSeverity().name() : null,
                violation.getField() != null ? violation.getField().name() : null,
                violation.getDescription(),
                violation.getActualValue(),
                violation.getExpectedValue(),
                violation.getConfidence(),
                violation.getSuggestedFix(),
                violation.getLegalReference()
        );
    }

    private CopilotContext.DeclarationSummary toDeclarationSummary(FusedDeclaration declaration) {
        return new CopilotContext.DeclarationSummary(
                declaration.getDeclarationType().name(),
                declaration.isPresent(),
                declaration.getFusedValue(),
                declaration.getFusedConfidence(),
                declaration.getReadabilityScore(),
                declaration.getLabelSection() != null ? declaration.getLabelSection().name() : null
        );
    }

    private CopilotContext.ImageQualitySummary toImageQualitySummary(Image image) {
        return new CopilotContext.ImageQualitySummary(
                image.getImageType() != null ? image.getImageType().name() : null,
                image.getQualityScore(),
                image.getQualityWarnings(),
                image.getRecommendedAction() != null ? image.getRecommendedAction().name() : null
        );
    }
}
