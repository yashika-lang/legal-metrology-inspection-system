package com.legalmetrology.scanner.entity;

import com.legalmetrology.common.entity.BaseEntity;
import com.legalmetrology.common.enums.ScanType;
import com.legalmetrology.inspection.entity.Inspection;
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

@Entity
@Table(name = "scans")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Scan extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "inspection_id", nullable = false)
    private Inspection inspection;

    @Enumerated(EnumType.STRING)
    @Column(name = "scan_type", nullable = false, length = 10)
    private ScanType scanType;

    @Column(name = "scan_value", nullable = false, length = 255)
    private String scanValue;
}
