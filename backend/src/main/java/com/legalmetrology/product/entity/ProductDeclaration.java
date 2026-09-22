package com.legalmetrology.product.entity;

import com.legalmetrology.common.entity.BaseEntity;
import com.legalmetrology.common.enums.DeclarationType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * One verified declaration value for a known product — the barcode-first
 * workflow's equivalent of a {@code label_detections} row, except sourced
 * from product master data instead of an OCR/Vision AI read of a specific
 * photo. {@code DeclarationFusionService} treats a product's declarations
 * as top-priority candidates (known-certain, not inferred) alongside
 * whatever OCR/Vision AI found on the uploaded images, so a scanned known
 * product still benefits from OCR filling in anything the catalog doesn't
 * have (see {@code declarationFusionServiceImpl}), and the Rule Engine
 * evaluates the resulting fused declarations exactly as it would for any
 * other inspection — nothing about rule evaluation is special-cased here.
 */
@Entity
@Table(name = "product_declarations", uniqueConstraints = @UniqueConstraint(columnNames = {"product_id", "declaration_type"}))
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductDeclaration extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Enumerated(EnumType.STRING)
    @Column(name = "declaration_type", nullable = false, length = 30)
    private DeclarationType declarationType;

    @Column(nullable = false, columnDefinition = "text")
    private String value;
}
