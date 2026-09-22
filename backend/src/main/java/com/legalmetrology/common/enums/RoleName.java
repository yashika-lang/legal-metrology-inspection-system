package com.legalmetrology.common.enums;

/** System roles. Kept as a fixed enum (not a free-text role table) because
 * the set of roles is small, stable, and referenced throughout security
 * annotations at compile time. */
public enum RoleName {
    ADMIN,
    SENIOR_OFFICER,
    INSPECTOR
}
