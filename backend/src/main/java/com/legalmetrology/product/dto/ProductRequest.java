package com.legalmetrology.product.dto;

import com.legalmetrology.validation.ValidBarcode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record ProductRequest(

        @NotBlank(message = "Product name is required")
        @Size(max = 255)
        String name,

        UUID categoryId,

        UUID manufacturerId,

        @ValidBarcode
        String barcode,

        @Size(max = 20)
        String defaultUnit
) {
}
