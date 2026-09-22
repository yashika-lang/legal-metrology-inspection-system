package com.legalmetrology.report.assembler.impl;

import com.legalmetrology.ai.copilot.dto.CopilotAnswerResponse;
import com.legalmetrology.ai.copilot.service.CopilotService;
import com.legalmetrology.evidence.entity.Evidence;
import com.legalmetrology.evidence.repository.EvidenceRepository;
import com.legalmetrology.exception.ResourceNotFoundException;
import com.legalmetrology.inspection.entity.Inspection;
import com.legalmetrology.inspection.entity.InspectionStatusHistory;
import com.legalmetrology.inspection.entity.Violation;
import com.legalmetrology.inspection.repository.InspectionRepository;
import com.legalmetrology.inspection.repository.InspectionStatusHistoryRepository;
import com.legalmetrology.inspection.repository.ViolationRepository;
import com.legalmetrology.ocr.entity.FusedDeclaration;
import com.legalmetrology.ocr.repository.FusedDeclarationRepository;
import com.legalmetrology.product.entity.Product;
import com.legalmetrology.report.assembler.ReportDataAssembler;
import com.legalmetrology.report.model.ReportData;
import com.legalmetrology.storage.dto.StorageBucket;
import com.legalmetrology.storage.service.StorageService;
import com.legalmetrology.trace.entity.DecisionTraceStep;
import com.legalmetrology.trace.repository.DecisionTraceStepRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Builds {@link ReportData} straight from the domain repositories — never
 * from {@code CopilotContext}, which is deliberately scoped to "what the
 * LLM may see," not "what a report may show." The two narrative fields
 * (executive summary, recommendations) are the only place this class talks
 * to the Copilot, and both are wrapped so a Gemini/Claude outage degrades
 * to a deterministic fallback instead of failing report generation — the
 * compliance facts in a report must never depend on an LLM being up.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ReportDataAssemblerImpl implements ReportDataAssembler {

    private final InspectionRepository inspectionRepository;
    private final FusedDeclarationRepository fusedDeclarationRepository;
    private final ViolationRepository violationRepository;
    private final EvidenceRepository evidenceRepository;
    private final DecisionTraceStepRepository decisionTraceStepRepository;
    private final InspectionStatusHistoryRepository statusHistoryRepository;
    private final CopilotService copilotService;
    private final StorageService storageService;

    // Not read-only: summarizeInspection/generateManufacturerRecommendations below write chat
    // history and an AI-response audit row, and — joining this same transaction under Spring's
    // default REQUIRED propagation — would fail against a read-only connection.
    @Override
    @Transactional
    public ReportData assemble(UUID inspectionId, UUID requestedByUserId) {
        Inspection inspection = inspectionRepository.findById(inspectionId)
                .orElseThrow(() -> ResourceNotFoundException.of("Inspection", inspectionId));

        List<Violation> violations = violationRepository.findByInspectionId(inspectionId);
        List<Evidence> evidence = evidenceRepository.findByInspectionId(inspectionId);
        List<FusedDeclaration> declarations = fusedDeclarationRepository.findByInspectionId(inspectionId);
        List<DecisionTraceStep> trace = decisionTraceStepRepository.findByInspectionIdOrderByCreatedAtAsc(inspectionId);
        List<InspectionStatusHistory> history = statusHistoryRepository.findByInspectionIdOrderByCreatedAtAsc(inspectionId);

        return new ReportData(
                inspectionId,
                null,
                Instant.now(),
                inspectionDetails(inspection),
                productDetails(inspection.getProduct()),
                complianceSummary(inspection, violations),
                declarations.stream().map(this::declarationEntry).toList(),
                violations.stream().map(this::violationEntry).toList(),
                evidence.stream().map(this::evidenceEntry).toList(),
                trace.stream().map(this::traceEntry).toList(),
                history.stream().map(this::timelineEntry).toList(),
                violations.stream().map(Violation::getLegalReference).filter(java.util.Objects::nonNull).distinct().sorted().toList(),
                narrativeOrFallback(() -> copilotService.summarizeInspection(inspectionId, requestedByUserId),
                        "Executive summary", deterministicSummary(inspection, violations)),
                narrativeOrFallback(() -> copilotService.generateManufacturerRecommendations(inspectionId, requestedByUserId),
                        "Recommendations", deterministicRecommendations(violations))
        );
    }

    private ReportData.InspectionDetails inspectionDetails(Inspection inspection) {
        return new ReportData.InspectionDetails(
                inspection.getInspector().getFullName(),
                inspection.getStatus(),
                inspection.getRegion(),
                inspection.getLocationLat(),
                inspection.getLocationLng(),
                inspection.getStartedAt(),
                inspection.getCompletedAt()
        );
    }

    private ReportData.ProductDetails productDetails(Product product) {
        if (product == null) {
            return new ReportData.ProductDetails(null, null, null, null, null);
        }
        return new ReportData.ProductDetails(
                product.getName(),
                product.getCategory() != null ? product.getCategory().getName() : null,
                product.getManufacturer() != null ? product.getManufacturer().getName() : null,
                product.getManufacturer() != null ? product.getManufacturer().getAddress() : null,
                product.getBarcode()
        );
    }

    private ReportData.ComplianceSummary complianceSummary(Inspection inspection, List<Violation> violations) {
        long criticalCount = violations.stream().filter(v -> v.getSeverity() == com.legalmetrology.common.enums.Severity.CRITICAL).count();
        return new ReportData.ComplianceSummary(inspection.getComplianceScore(), inspection.getFraudRisk(), violations.size(), (int) criticalCount);
    }

    private ReportData.DeclarationEntry declarationEntry(FusedDeclaration declaration) {
        return new ReportData.DeclarationEntry(
                declaration.getDeclarationType(), declaration.isPresent(), declaration.getFusedValue(),
                declaration.getFusedConfidence(), declaration.getLabelSection() != null ? declaration.getLabelSection().name() : null
        );
    }

    private ReportData.ViolationEntry violationEntry(Violation violation) {
        return new ReportData.ViolationEntry(
                violation.getId(), violation.getRule().getRuleCode(), violation.getRule().getTitle(), violation.getSeverity(),
                violation.getField(), violation.getDescription(), violation.getActualValue(), violation.getExpectedValue(),
                violation.getSuggestedFix(), violation.getLegalReference()
        );
    }

    private ReportData.EvidenceEntry evidenceEntry(Evidence e) {
        String originalUrl = e.getOriginalImagePath() != null ? storageService.generateSignedUrl(StorageBucket.EVIDENCE, e.getOriginalImagePath()) : null;
        String annotatedUrl = e.getAnnotatedImagePath() != null ? storageService.generateSignedUrl(StorageBucket.EVIDENCE, e.getAnnotatedImagePath()) : null;
        return new ReportData.EvidenceEntry(
                e.getId(), e.getViolation().getId(), e.getViolation().getRule().getRuleCode(), e.getSeverity(), e.getReason(),
                e.getSha256Hash(), originalUrl, annotatedUrl, e.getAnnotatedImagePath()
        );
    }

    private ReportData.TraceEntry traceEntry(DecisionTraceStep step) {
        return new ReportData.TraceEntry(
                step.getStepName(), step.getModule(), step.getStatus(), step.getConfidence(),
                step.getExecutionTimeMs(), step.getStartedAt(), step.getOutputSummary()
        );
    }

    private ReportData.TimelineEntry timelineEntry(InspectionStatusHistory history) {
        return new ReportData.TimelineEntry(
                history.getFromStatus(), history.getToStatus(),
                history.getChangedBy() != null ? history.getChangedBy().getFullName() : null,
                history.getNote(), history.getCreatedAt()
        );
    }

    private String narrativeOrFallback(java.util.function.Supplier<CopilotAnswerResponse> generator, String label, String fallback) {
        try {
            return generator.get().answer();
        } catch (Exception ex) {
            log.warn("{} narrative unavailable (LLM provider failed), using deterministic fallback: {}", label, ex.getMessage());
            return fallback;
        }
    }

    private String deterministicSummary(Inspection inspection, List<Violation> violations) {
        long critical = violations.stream().filter(v -> v.getSeverity() == com.legalmetrology.common.enums.Severity.CRITICAL).count();
        return "Inspection " + inspection.getId() + " recorded a compliance score of "
                + (inspection.getComplianceScore() != null ? inspection.getComplianceScore() : "N/A")
                + " with " + violations.size() + " violation(s), " + critical + " of which are critical. "
                + "Fraud risk band: " + (inspection.getFraudRisk() != null ? inspection.getFraudRisk() : "N/A") + ".";
    }

    private String deterministicRecommendations(List<Violation> violations) {
        List<String> fixes = violations.stream()
                .map(Violation::getSuggestedFix)
                .filter(java.util.Objects::nonNull)
                .distinct()
                .sorted()
                .toList();
        if (fixes.isEmpty()) {
            return "No corrective actions required — no open violations.";
        }
        StringBuilder sb = new StringBuilder("Recommended corrective actions:\n");
        fixes.forEach(fix -> sb.append("- ").append(fix).append('\n'));
        return sb.toString();
    }
}
