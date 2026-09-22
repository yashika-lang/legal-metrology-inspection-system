package com.legalmetrology.common.settings.service.impl;

import com.legalmetrology.auth.repository.UserRepository;
import com.legalmetrology.common.settings.dto.SettingsRequest;
import com.legalmetrology.common.settings.dto.SettingsResponse;
import com.legalmetrology.common.settings.entity.Settings;
import com.legalmetrology.common.settings.mapper.SettingsMapper;
import com.legalmetrology.common.settings.repository.SettingsRepository;
import com.legalmetrology.common.settings.service.SettingsService;
import com.legalmetrology.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SettingsServiceImpl implements SettingsService {

    private final SettingsRepository settingsRepository;
    private final UserRepository userRepository;
    private final SettingsMapper settingsMapper;

    @Override
    @Transactional(readOnly = true)
    public List<SettingsResponse> listAll() {
        return settingsRepository.findAll().stream().map(settingsMapper::toResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public SettingsResponse getByKey(String key) {
        return settingsMapper.toResponse(settingsRepository.findByKey(key)
                .orElseThrow(() -> ResourceNotFoundException.of("Settings", key)));
    }

    @Override
    @Transactional
    public SettingsResponse upsert(SettingsRequest request, UUID updatedByUserId) {
        Settings settings = settingsRepository.findByKey(request.key())
                .orElseGet(() -> Settings.builder().key(request.key()).build());

        settings.setValue(request.value());
        settings.setUpdatedBy(updatedByUserId != null ? userRepository.findById(updatedByUserId).orElse(null) : null);

        return settingsMapper.toResponse(settingsRepository.save(settings));
    }
}
