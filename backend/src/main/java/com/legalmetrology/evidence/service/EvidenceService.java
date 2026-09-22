package com.legalmetrology.evidence.service;

import com.legalmetrology.evidence.entity.Evidence;

import java.util.List;
import java.util.UUID;

/**
 * Generates and serves the Explainable Evidence Framework's evidence
 * records — see {@link Evidence}'s Javadoc for what one row contains and
 * why. Independent of the Report/Dashboard/Copilot modules that consume
 * it: this service only knows Violations, Images, and the OCR/Vision
 * signals already persisted by the pipeline.
 */
public interface EvidenceService {

    /** Generates (or regenerates, if not yet finalized) evidence for every violation on the inspection that doesn't already have it. */
    List<Evidence> generateForInspection(UUID inspectionId);

    /** Generates (or regenerates) the evidence for one specific violation. */
    Evidence generateForViolation(UUID violationId);

    List<Evidence> getByInspection(UUID inspectionId);

    Evidence getById(UUID evidenceId);

    /** Marks every evidence row for the inspection immutable — called when the inspection is finalized (status COMPLETED). Idempotent. */
    void finalizeEvidence(UUID inspectionId);
}
