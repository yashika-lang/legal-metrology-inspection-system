package com.legalmetrology.product.dto;

import java.time.Instant;
import java.util.UUID;

public record ProductResponse(
        UUID id,
        String name,
        UUID categoryId,
        String categoryName,
        UUID manufacturerId,
        String manufacturerName,
        String barcode,
        String defaultUnit,
        Instant createdAt
) {
}
