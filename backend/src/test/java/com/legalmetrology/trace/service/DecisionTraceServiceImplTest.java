package com.legalmetrology.trace.service;

import com.legalmetrology.common.enums.TraceStatus;
import com.legalmetrology.common.enums.TraceStepName;
import com.legalmetrology.exception.ResourceNotFoundException;
import com.legalmetrology.inspection.entity.Inspection;
import com.legalmetrology.inspection.repository.InspectionRepository;
import com.legalmetrology.trace.entity.DecisionTraceStep;
import com.legalmetrology.trace.repository.DecisionTraceStepRepository;
import com.legalmetrology.trace.service.impl.DecisionTraceServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DecisionTraceServiceImplTest {

    @Mock
    private DecisionTraceStepRepository decisionTraceStepRepository;
    @Mock
    private InspectionRepository inspectionRepository;

    private DecisionTraceServiceImpl decisionTraceService;

    @BeforeEach
    void setUp() {
        decisionTraceService = new DecisionTraceServiceImpl(decisionTraceStepRepository, inspectionRepository);
    }

    @Test
    void recordStep_persistsEveryFieldFromTheCommandOntoTheEntity() {
        UUID inspectionId = UUID.randomUUID();
        UUID ruleId = UUID.randomUUID();
        Inspection inspection = Inspection.builder().build();
        when(inspectionRepository.getReferenceById(inspectionId)).thenReturn(inspection);
        when(decisionTraceStepRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        long startedAtMs = System.currentTimeMillis() - 50;
        RecordStepCommand command = RecordStepCommand.success(inspectionId, TraceStepName.RULE_EVALUATION, "TestModule",
                        "input", "output", BigDecimal.valueOf(0.87), startedAtMs)
                .withReferences(ruleId, null, null);

        DecisionTraceStep result = decisionTraceService.recordStep(command);

        assertThat(result.getInspection()).isSameAs(inspection);
        assertThat(result.getStepName()).isEqualTo(TraceStepName.RULE_EVALUATION);
        assertThat(result.getModule()).isEqualTo("TestModule");
        assertThat(result.getInputSummary()).isEqualTo("input");
        assertThat(result.getOutputSummary()).isEqualTo("output");
        assertThat(result.getConfidence()).isEqualByComparingTo("0.87");
        assertThat(result.getStatus()).isEqualTo(TraceStatus.SUCCESS);
        assertThat(result.getReferencedRuleId()).isEqualTo(ruleId);
        assertThat(result.getExecutionTimeMs()).isGreaterThanOrEqualTo(0);
    }

    @Test
    void recordStep_failureFactorySetsFailedStatusAndReason() {
        UUID inspectionId = UUID.randomUUID();
        when(inspectionRepository.getReferenceById(inspectionId)).thenReturn(Inspection.builder().build());
        when(decisionTraceStepRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        RecordStepCommand command = RecordStepCommand.failure(inspectionId, TraceStepName.OCR, "OcrServiceImpl",
                "image-id", "Google Vision API timed out", System.currentTimeMillis());

        DecisionTraceStep result = decisionTraceService.recordStep(command);

        assertThat(result.getStatus()).isEqualTo(TraceStatus.FAILED);
        assertThat(result.getReason()).isEqualTo("Google Vision API timed out");
        assertThat(result.getConfidence()).isNull();
    }

    @Test
    void getTrace_throwsForAnInspectionThatDoesNotExist() {
        UUID inspectionId = UUID.randomUUID();
        when(inspectionRepository.existsById(inspectionId)).thenReturn(false);

        assertThatThrownBy(() -> decisionTraceService.getTrace(inspectionId))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void getTrace_returnsStepsInCreationOrderForAnExistingInspection() {
        UUID inspectionId = UUID.randomUUID();
        when(inspectionRepository.existsById(inspectionId)).thenReturn(true);

        DecisionTraceStep first = DecisionTraceStep.builder().stepName(TraceStepName.IMAGE_UPLOADED).startedAt(Instant.now()).status(TraceStatus.SUCCESS).build();
        DecisionTraceStep second = DecisionTraceStep.builder().stepName(TraceStepName.OCR).startedAt(Instant.now()).status(TraceStatus.SUCCESS).build();
        when(decisionTraceStepRepository.findByInspectionIdOrderByCreatedAtAsc(inspectionId)).thenReturn(List.of(first, second));

        List<DecisionTraceStep> result = decisionTraceService.getTrace(inspectionId);

        assertThat(result).containsExactly(first, second);
    }

    @Test
    void recordStep_referencesResolveToInspectionRepositoryReferenceNotAFullLoad() {
        UUID inspectionId = UUID.randomUUID();
        when(inspectionRepository.getReferenceById(inspectionId)).thenReturn(Inspection.builder().build());
        ArgumentCaptor<DecisionTraceStep> captor = ArgumentCaptor.forClass(DecisionTraceStep.class);
        when(decisionTraceStepRepository.save(captor.capture())).thenAnswer(invocation -> invocation.getArgument(0));

        decisionTraceService.recordStep(RecordStepCommand.success(inspectionId, TraceStepName.EVIDENCE_GENERATED,
                "EvidenceServiceImpl", "in", "out", null, System.currentTimeMillis()));

        verify(inspectionRepository).getReferenceById(inspectionId);
        assertThat(captor.getValue().getInspection()).isNotNull();
    }
}
