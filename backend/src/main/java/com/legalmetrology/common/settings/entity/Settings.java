package com.legalmetrology.common.settings.entity;

import com.legalmetrology.auth.entity.User;
import com.legalmetrology.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** System-wide configuration as key/JSON-value pairs (feature flags, AI thresholds, locale defaults, etc.). */
@Entity
@Table(name = "settings")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Settings extends BaseEntity {

    @Column(nullable = false, unique = true, length = 150)
    private String key;

    @Column(nullable = false, columnDefinition = "text")
    private String value;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "updated_by")
    private User updatedBy;
}
