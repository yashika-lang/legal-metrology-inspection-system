package com.legalmetrology.product.mapper;

import com.legalmetrology.product.dto.ManufacturerResponse;
import com.legalmetrology.product.dto.ProductCategoryResponse;
import com.legalmetrology.product.dto.ProductResponse;
import com.legalmetrology.product.entity.Manufacturer;
import com.legalmetrology.product.entity.Product;
import com.legalmetrology.product.entity.ProductCategory;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ProductMapper {

    default ProductResponse toResponse(Product product) {
        if (product == null) {
            return null;
        }
        return new ProductResponse(
                product.getId(),
                product.getName(),
                product.getCategory() != null ? product.getCategory().getId() : null,
                product.getCategory() != null ? product.getCategory().getName() : null,
                product.getManufacturer() != null ? product.getManufacturer().getId() : null,
                product.getManufacturer() != null ? product.getManufacturer().getName() : null,
                product.getBarcode(),
                product.getDefaultUnit(),
                product.getCreatedAt()
        );
    }

    default ProductCategoryResponse toResponse(ProductCategory category) {
        if (category == null) {
            return null;
        }
        return new ProductCategoryResponse(
                category.getId(),
                category.getName(),
                category.getParentCategory() != null ? category.getParentCategory().getId() : null
        );
    }

    default ManufacturerResponse toResponse(Manufacturer manufacturer) {
        if (manufacturer == null) {
            return null;
        }
        return new ManufacturerResponse(
                manufacturer.getId(),
                manufacturer.getName(),
                manufacturer.getGstin(),
                manufacturer.getAddress(),
                manufacturer.getRegion()
        );
    }
}
