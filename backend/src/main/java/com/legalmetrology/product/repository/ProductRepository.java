package com.legalmetrology.product.repository;

import com.legalmetrology.product.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ProductRepository extends JpaRepository<Product, UUID> {

    Optional<Product> findByBarcode(String barcode);

    boolean existsByBarcode(String barcode);

    /** {@code @EntityGraph}: {@code ProductMapper} reads {@code category.getName()}/{@code manufacturer.getName()} for every row. */
    @EntityGraph(attributePaths = {"category", "manufacturer"})
    Page<Product> findByNameContainingIgnoreCase(String name, Pageable pageable);

    @Override
    @EntityGraph(attributePaths = {"category", "manufacturer"})
    Page<Product> findAll(Pageable pageable);
}
