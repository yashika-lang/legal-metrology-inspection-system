package com.legalmetrology.evidence.hash;

import com.legalmetrology.common.enums.Severity;
import com.legalmetrology.evidence.entity.Evidence;
import com.legalmetrology.inspection.entity.Inspection;
import com.legalmetrology.inspection.entity.Violation;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class EvidenceHasherTest {

    private final EvidenceHasher hasher = new EvidenceHasher();

    @Test
    void hashingTheSameContentTwiceProducesTheSameHash() {
        Evidence evidence = evidence("Rs 100.00", "Missing tax wording");

        String first = hasher.hash(evidence, null);
        String second = hasher.hash(evidence, null);

        assertThat(first).isEqualTo(second);
        assertThat(first).hasSize(64); // SHA-256 hex
    }

    @Test
    void changingAnyLegallyRelevantFieldChangesTheHash() {
        Evidence original = evidence("Rs 100.00", "Missing tax wording");
        Evidence tampered = evidence("Rs 999.00", "Missing tax wording");

        assertThat(hasher.hash(original, null)).isNotEqualTo(hasher.hash(tampered, null));
    }

    @Test
    void changingTheAnnotatedImageBytesChangesTheHash() {
        Evidence evidence = evidence("Rs 100.00", "Missing tax wording");

        String withoutImage = hasher.hash(evidence, null);
        String withImageA = hasher.hash(evidence, new byte[]{1, 2, 3});
        String withImageB = hasher.hash(evidence, new byte[]{4, 5, 6});

        assertThat(withoutImage).isNotEqualTo(withImageA);
        assertThat(withImageA).isNotEqualTo(withImageB);
    }

    private Evidence evidence(String actualValue, String reason) {
        Inspection inspection = Inspection.builder().build();
        setId(inspection, UUID.randomUUID());
        Violation violation = Violation.builder().build();
        setId(violation, UUID.randomUUID());

        return Evidence.builder()
                .inspection(inspection)
                .violation(violation)
                .actualValue(actualValue)
                .reason(reason)
                .severity(Severity.MAJOR)
                .build();
    }

    private void setId(com.legalmetrology.common.entity.BaseEntity entity, UUID id) {
        entity.setId(id);
    }
}
