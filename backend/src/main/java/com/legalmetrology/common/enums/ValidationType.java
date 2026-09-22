package com.legalmetrology.common.enums;

/**
 * The fixed set of generic, reusable validation strategies the rule engine
 * dispatches on (see {@code rules.engine.validator}). A {@code Rule} row
 * selects one of these plus a JSON {@code validationExpression} parameter
 * blob — new rules are added as data, never as new Java validation logic,
 * except for genuinely novel composite checks under {@link #CUSTOM}.
 */
public enum ValidationType {
    FIELD_EXISTS,
    FIELD_NOT_EMPTY,
    REGEX,
    MIN_LENGTH,
    MAX_LENGTH,
    ENUM,
    DATE,
    NUMERIC,
    FONT_SIZE,
    READABILITY,
    PLACEMENT,
    CUSTOM
}
