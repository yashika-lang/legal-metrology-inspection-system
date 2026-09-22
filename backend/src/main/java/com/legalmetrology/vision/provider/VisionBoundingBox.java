package com.legalmetrology.vision.provider;

/** Image-relative bounding box: x/y/w/h each in [0,1], origin at top-left — portable across any actual image resolution. */
public record VisionBoundingBox(double x, double y, double w, double h) {
}
