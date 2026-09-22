package com.legalmetrology.evidence.hash;

import com.legalmetrology.evidence.entity.Evidence;
import com.legalmetrology.utils.ImageUtil;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;

/**
 * Computes the SHA-256 hash stored on every {@link Evidence} row. Kept as
 * its own class (not inlined in the service) because a future digital
 * signature / tamper-check feature signs or re-verifies exactly this same
 * hash — one canonical definition of "what this evidence's content is"
 * that both features can share without re-deriving it differently.
 * <p>
 * The hash covers every legally-relevant field (never the mutable
 * {@code isImmutable}/{@code signature} bookkeeping columns) plus the
 * annotated image bytes when one was generated, so any tampering with
 * either the recorded facts or the image itself changes the hash.
 */
@Component
public class EvidenceHasher {

    public String hash(Evidence evidence, byte[] annotatedImageBytes) {
        String canonical = String.join("|",
                nullToEmpty(evidence.getInspection().getId()),
                nullToEmpty(evidence.getViolation().getId()),
                nullToEmpty(evidence.getOcrText()),
                nullToEmpty(evidence.getNormalizedValue()),
                nullToEmpty(evidence.getExpectedValue()),
                nullToEmpty(evidence.getActualValue()),
                nullToEmpty(evidence.getReason()),
                nullToEmpty(evidence.getSuggestedFix()),
                nullToEmpty(evidence.getLegalRuleReference()),
                nullToEmpty(evidence.getSeverity()),
                nullToEmpty(evidence.getBoundingBox())
        );

        ByteArrayOutputStream payload = new ByteArrayOutputStream();
        payload.writeBytes(canonical.getBytes(StandardCharsets.UTF_8));
        if (annotatedImageBytes != null) {
            payload.writeBytes(annotatedImageBytes);
        }
        return ImageUtil.sha256Hex(payload.toByteArray());
    }

    private String nullToEmpty(Object value) {
        return value == null ? "" : value.toString();
    }
}
