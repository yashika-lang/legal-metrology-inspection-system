package com.legalmetrology.inspection.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Size;

import java.util.UUID;

/** productId/location/region are all genuinely optional (an inspection can be created before a product or GPS fix is available) — validation here only bounds values that ARE supplied, never requires them. */
public record InspectionRequest(
        UUID productId,

        @DecimalMin(value = "-90.0", message = "Latitude must be between -90 and 90")
        @DecimalMax(value = "90.0", message = "Latitude must be between -90 and 90")
        Double locationLat,

        @DecimalMin(value = "-180.0", message = "Longitude must be between -180 and 180")
        @DecimalMax(value = "180.0", message = "Longitude must be between -180 and 180")
        Double locationLng,

        @Size(max = 100, message = "Region must not exceed 100 characters")
        String region
) {
}
