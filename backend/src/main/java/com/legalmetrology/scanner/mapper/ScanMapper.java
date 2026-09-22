package com.legalmetrology.scanner.mapper;

import com.legalmetrology.product.dto.ProductResponse;
import com.legalmetrology.scanner.dto.ScanResponse;
import com.legalmetrology.scanner.entity.Scan;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ScanMapper {

    default ScanResponse toResponse(Scan scan, ProductResponse matchedProduct) {
        if (scan == null) {
            return null;
        }
        return new ScanResponse(
                scan.getId(),
                scan.getInspection().getId(),
                scan.getScanType(),
                scan.getScanValue(),
                scan.getCreatedAt(),
                matchedProduct != null ? matchedProduct.id() : null,
                matchedProduct != null ? matchedProduct.name() : null
        );
    }
}
