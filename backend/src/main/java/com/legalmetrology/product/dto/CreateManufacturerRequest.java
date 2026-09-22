package com.legalmetrology.product.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateManufacturerRequest(@NotBlank String name, String gstin, String address, String region) {
}
