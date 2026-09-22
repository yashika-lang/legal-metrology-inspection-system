package com.legalmetrology.vision.quality;

/** Individual image defects the preprocessing analyzer (Step 1) checks for. */
public enum QualityWarning {
    BLUR,
    NOISE,
    ROTATED,
    LOW_BRIGHTNESS,
    HIGH_BRIGHTNESS,
    LOW_CONTRAST,
    GLARE,
    CROPPED,
    LOW_RESOLUTION,
    PERSPECTIVE_DISTORTION
}
