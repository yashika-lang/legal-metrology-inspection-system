package com.legalmetrology.product.repository;

import com.legalmetrology.product.entity.ProductDeclaration;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ProductDeclarationRepository extends JpaRepository<ProductDeclaration, UUID> {

    List<ProductDeclaration> findByProductId(UUID productId);
}
