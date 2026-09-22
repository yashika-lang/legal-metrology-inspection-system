package com.legalmetrology.trace.service;

import com.legalmetrology.trace.entity.DecisionTraceStep;

import java.util.List;
import java.util.UUID;

/**
 * Records and retrieves the decision trace — the ordered, append-only log
 * of every pipeline stage executed for an inspection. Deliberately a
 * single-shot {@link #recordStep} rather than a start/complete handle: the
 * caller already knows when a stage starts and ends (it just ran it), so
 * there is nothing a stateful handle would buy beyond extra API surface.
 */
public interface DecisionTraceService {

    DecisionTraceStep recordStep(RecordStepCommand command);

    List<DecisionTraceStep> getTrace(UUID inspectionId);
}
