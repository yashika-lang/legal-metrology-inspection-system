package com.legalmetrology.report.assembler;

import com.legalmetrology.ai.copilot.service.CopilotService;
import com.legalmetrology.common.enums.Severity;
import com.legalmetrology.evidence.repository.EvidenceRepository;
import com.legalmetrology.exception.ResourceNotFoundException;
import com.legalmetrology.inspection.entity.Inspection;
import com.legalmetrology.inspection.entity.Rule;
import com.legalmetrology.inspection.entity.Violation;
import com.legalmetrology.inspection.repository.InspectionRepository;
import com.legalmetrology.inspection.repository.InspectionStatusHistoryRepository;
import com.legalmetrology.inspection.repository.ViolationRepository;
import com.legalmetrology.ocr.repository.FusedDeclarationRepository;
import com.legalmetrology.report.assembler.impl.ReportDataAssemblerImpl;
import com.legalmetrology.report.model.ReportData;
import com.legalmetrology.storage.service.StorageService;
import com.legalmetrology.trace.repository.DecisionTraceStepRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReportDataAssemblerImplTest {

    @Mock
    private InspectionRepository inspectionRepository;
    @Mock
    private FusedDeclarationRepository fusedDeclarationRepository;
    @Mock
    private ViolationRepository violationRepository;
    @Mock
    private EvidenceRepository evidenceRepository;
    @Mock
    private DecisionTraceStepRepository decisionTraceStepRepository;
    @Mock
    private InspectionStatusHistoryRepository statusHistoryRepository;
    @Mock
    private CopilotService copilotService;
    @Mock
    private StorageService storageService;

    private ReportDataAssemblerImpl assembler;

    @BeforeEach
    void setUp() {
        assembler = new ReportDataAssemblerImpl(inspectionRepository, fusedDeclarationRepository, violationRepository,
                evidenceRepository, decisionTraceStepRepository, statusHistoryRepository, copilotService, storageService);
    }

    @Test
    void assemble_throwsWhenInspectionDoesNotExist() {
        UUID inspectionId = UUID.randomUUID();
        when(inspectionRepository.findById(inspectionId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> assemble(inspectionId))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void assemble_fallsBackToADeterministicSummaryWhenTheLlmProviderFails() {
        UUID inspectionId = UUID.randomUUID();
        Inspection inspection = inspectionFixture(inspectionId, new BigDecimal("62.50"));
        wireEmptyCollections(inspectionId, inspection);

        when(copilotService.summarizeInspection(eq(inspectionId), any()))
                .thenThrow(new RuntimeException("Gemini API key invalid"));
        when(copilotService.generateManufacturerRecommendations(eq(inspectionId), any()))
                .thenThrow(new RuntimeException("Gemini API key invalid"));

        ReportData data = assemble(inspectionId);

        assertThat(data.executiveSummary()).contains("62.50").contains("0 violation(s)");
        assertThat(data.recommendations()).isEqualTo("No corrective actions required — no open violations.");
    }

    @Test
    void assemble_usesTheLlmNarrativeWhenTheProviderSucceeds() {
        UUID inspectionId = UUID.randomUUID();
        Inspection inspection = inspectionFixture(inspectionId, new BigDecimal("90.00"));
        wireEmptyCollections(inspectionId, inspection);

        when(copilotService.summarizeInspection(eq(inspectionId), any()))
                .thenReturn(com.legalmetrology.ai.copilot.dto.CopilotAnswerResponse.generative("A clean inspection with no issues.", "gemini-2.5-pro"));
        when(copilotService.generateManufacturerRecommendations(eq(inspectionId), any()))
                .thenReturn(com.legalmetrology.ai.copilot.dto.CopilotAnswerResponse.generative("Keep up the good labeling practices.", "gemini-2.5-pro"));

        ReportData data = assemble(inspectionId);

        assertThat(data.executiveSummary()).isEqualTo("A clean inspection with no issues.");
        assertThat(data.recommendations()).isEqualTo("Keep up the good labeling practices.");
    }

    @Test
    void assemble_recommendationsFallbackListsEachDistinctSuggestedFix() {
        UUID inspectionId = UUID.randomUUID();
        Inspection inspection = inspectionFixture(inspectionId, new BigDecimal("40.00"));

        Rule rule = Rule.builder().ruleCode("LM-MRP-001").title("MRP declaration").build();
        Violation violation = Violation.builder()
                .inspection(inspection).rule(rule).severity(Severity.MAJOR)
                .suggestedFix("Add inclusive-of-taxes wording").legalReference("Rule 6")
                .build();

        when(inspectionRepository.findById(inspectionId)).thenReturn(Optional.of(inspection));
        when(violationRepository.findByInspectionId(inspectionId)).thenReturn(List.of(violation));
        when(evidenceRepository.findByInspectionId(inspectionId)).thenReturn(List.of());
        when(fusedDeclarationRepository.findByInspectionId(inspectionId)).thenReturn(List.of());
        when(decisionTraceStepRepository.findByInspectionIdOrderByCreatedAtAsc(inspectionId)).thenReturn(List.of());
        when(statusHistoryRepository.findByInspectionIdOrderByCreatedAtAsc(inspectionId)).thenReturn(List.of());
        when(copilotService.summarizeInspection(any(), any())).thenThrow(new RuntimeException("down"));
        when(copilotService.generateManufacturerRecommendations(any(), any())).thenThrow(new RuntimeException("down"));

        ReportData data = assemble(inspectionId);

        assertThat(data.recommendations()).contains("Add inclusive-of-taxes wording");
        assertThat(data.legalReferences()).containsExactly("Rule 6");
    }

    private void wireEmptyCollections(UUID inspectionId, Inspection inspection) {
        when(inspectionRepository.findById(inspectionId)).thenReturn(Optional.of(inspection));
        when(violationRepository.findByInspectionId(inspectionId)).thenReturn(List.of());
        when(evidenceRepository.findByInspectionId(inspectionId)).thenReturn(List.of());
        when(fusedDeclarationRepository.findByInspectionId(inspectionId)).thenReturn(List.of());
        when(decisionTraceStepRepository.findByInspectionIdOrderByCreatedAtAsc(inspectionId)).thenReturn(List.of());
        when(statusHistoryRepository.findByInspectionIdOrderByCreatedAtAsc(inspectionId)).thenReturn(List.of());
    }

    private Inspection inspectionFixture(UUID id, BigDecimal complianceScore) {
        var inspector = com.legalmetrology.auth.entity.User.builder().fullName("Officer Test").build();
        Inspection inspection = Inspection.builder()
                .inspector(inspector)
                .complianceScore(complianceScore)
                .build();
        setId(inspection, id);
        return inspection;
    }

    private ReportData assemble(UUID inspectionId) {
        return assembler.assemble(inspectionId, UUID.randomUUID());
    }

    private void setId(com.legalmetrology.common.entity.BaseEntity entity, UUID id) {
        entity.setId(id);
    }
}
