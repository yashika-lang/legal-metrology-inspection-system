package com.legalmetrology.analytics.model;

/** One cell of the (region × severity) heatmap — where critical violations concentrate geographically. */
public record RegionSeverityCount(String region, String severity, long count) {
}
