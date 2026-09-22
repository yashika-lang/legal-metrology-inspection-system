package com.legalmetrology.analytics.model;

/** One cell of the (violated-field × product-category) cross-tab — e.g. "NET_QUANTITY issues are most common in Food". */
public record FieldCategoryCount(String field, String category, long count) {
}
