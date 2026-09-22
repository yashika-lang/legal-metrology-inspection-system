package com.legalmetrology.product.dto;

import java.util.UUID;

public record ProductCategoryResponse(UUID id, String name, UUID parentCategoryId) {
}
