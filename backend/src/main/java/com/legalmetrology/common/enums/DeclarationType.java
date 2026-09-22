package com.legalmetrology.common.enums;

/**
 * Every mandatory (or commonly checked) declaration this system looks for on
 * a packaged-commodity label, per the Legal Metrology (Packaged Commodities)
 * Rules. Shared by {@code ocr} (text-based field extraction), {@code vision}
 * (visual/multimodal detection), and {@code rules} (the SpEL evaluation
 * context keys the seeded rule expressions reference by this enum's name).
 */
public enum DeclarationType {
    PRODUCT_NAME,
    GENERIC_NAME,
    MANUFACTURER,
    PACKER,
    IMPORTER,
    ADDRESS,
    MRP,
    NET_QUANTITY,
    MFG_MONTH,
    MFG_YEAR,
    CUSTOMER_CARE,
    EMAIL,
    LICENSE_NUMBER,
    BATCH_NUMBER,
    COUNTRY_OF_ORIGIN
}
