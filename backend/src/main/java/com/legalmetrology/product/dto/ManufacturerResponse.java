package com.legalmetrology.product.dto;

import java.util.UUID;

public record ManufacturerResponse(UUID id, String name, String gstin, String address, String region) {
}
