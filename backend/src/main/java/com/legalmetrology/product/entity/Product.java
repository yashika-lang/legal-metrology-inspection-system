package com.legalmetrology.product.entity;

import com.legalmetrology.auth.entity.User;
import com.legalmetrology.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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

@Entity
@Table(name = "products", uniqueConstraints = @UniqueConstraint(columnNames = "barcode"))
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Product extends BaseEntity {

    @Column(nullable = false, length = 255)
    private String name;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private ProductCategory category;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "manufacturer_id")
    private Manufacturer manufacturer;

    @Column(length = 50)
    private String barcode;

    @Column(name = "default_unit", length = 20)
    private String defaultUnit;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User createdBy;

    /**
     * Informational only — none of these map to a {@code DeclarationType}
     * the Rule Engine evaluates, unlike the fields captured in {@link
     * com.legalmetrology.product.entity.ProductDeclaration}.
     */
    @Column(length = 255)
    private String website;

    @Column(columnDefinition = "text")
    private String ingredients;

    @Column(name = "expiry_date")
    private java.time.LocalDate expiryDate;

    @Column(name = "reference_image_url", columnDefinition = "text")
    private String referenceImageUrl;
}
