package com.legalmetrology.inspection.entity;

import com.legalmetrology.auth.entity.User;
import com.legalmetrology.common.entity.BaseEntity;
import com.legalmetrology.common.enums.FraudRisk;
import com.legalmetrology.common.enums.InspectionStatus;
import com.legalmetrology.product.entity.Product;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "inspections")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Inspection extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "inspector_id", nullable = false)
    private User inspector;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id")
    private Product product;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private InspectionStatus status = InspectionStatus.DRAFT;

    @Column(name = "location_lat")
    private Double locationLat;

    @Column(name = "location_lng")
    private Double locationLng;

    /** Free-text jurisdiction/region captured at inspection time — the dimension {@code analytics} region-level insights and risk scores group by. */
    @Column(length = 100)
    private String region;

    @Column(name = "compliance_score", precision = 5, scale = 2)
    private BigDecimal complianceScore;

    @Enumerated(EnumType.STRING)
    @Column(name = "fraud_risk", length = 10)
    private FraudRisk fraudRisk;

    @Column(name = "started_at", nullable = false)
    @Builder.Default
    private Instant startedAt = Instant.now();

    @Column(name = "completed_at")
    private Instant completedAt;
}
