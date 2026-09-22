package com.legalmetrology.product.dto;

import jakarta.validation.constraints.NotBlank;

import java.util.UUID;

public record CreateCategoryRequest(@NotBlank String name, UUID parentCategoryId) {
}
