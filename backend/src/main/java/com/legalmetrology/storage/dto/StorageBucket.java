package com.legalmetrology.storage.dto;

/**
 * The Supabase Storage buckets this system provisions — see
 * docs/ARCHITECTURE.md §6. {@code IMAGES}, {@code REPORTS}, and
 * {@code EVIDENCE} are the three actually in active use (raw inspection
 * photos, generated PDF/DOCX reports, and cropped/annotated evidence
 * images respectively — evidence deliberately has its own bucket rather
 * than nesting under images, since legal evidence may need different
 * retention/access rules than working photos). {@code TEMP},
 * {@code EXPORTS}, and {@code TRAINING_DATA} are reserved for planned
 * features (a temp-upload staging area, bulk data exports, and a future
 * ML training-data pipeline) — nothing calls them yet, so their buckets
 * don't need to exist for the system to run.
 */
public enum StorageBucket {
    IMAGES,
    REPORTS,
    EVIDENCE,
    TEMP,
    EXPORTS,
    TRAINING_DATA
}
