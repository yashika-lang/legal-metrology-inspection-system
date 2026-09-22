package com.legalmetrology.product.service;

import com.legalmetrology.product.dto.ManufacturerResponse;
import com.legalmetrology.product.dto.ProductCategoryResponse;
import com.legalmetrology.product.dto.ProductRequest;
import com.legalmetrology.product.dto.ProductResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProductService {

    ProductResponse create(ProductRequest request, UUID createdByUserId);

    ProductResponse getById(UUID id);

    ProductResponse getByBarcode(String barcode);

    /** Non-throwing lookup for callers (e.g. the scanner module) that treat "no match" as a normal outcome. */
    Optional<ProductResponse> findByBarcode(String barcode);

    Page<ProductResponse> search(String nameQuery, Pageable pageable);

    ProductResponse update(UUID id, ProductRequest request);

    void delete(UUID id);

    List<ProductCategoryResponse> listCategories();

    ProductCategoryResponse createCategory(String name, UUID parentCategoryId);

    List<ManufacturerResponse> listManufacturers();

    ManufacturerResponse createManufacturer(String name, String gstin, String address, String region);
}
