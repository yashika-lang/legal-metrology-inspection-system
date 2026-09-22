package com.legalmetrology.product.entity;

import com.legalmetrology.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "manufacturers")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Manufacturer extends BaseEntity {

    @Column(nullable = false, length = 255)
    private String name;

    @Column(length = 20)
    private String gstin;

    @Column(length = 500)
    private String address;

    @Column(length = 100)
    private String region;
}
