package com.legalmetrology.common.settings.repository;

import com.legalmetrology.common.settings.entity.Settings;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface SettingsRepository extends JpaRepository<Settings, UUID> {

    Optional<Settings> findByKey(String key);

    boolean existsByKey(String key);
}
