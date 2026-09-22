package com.legalmetrology.common.settings.service;

import com.legalmetrology.common.settings.dto.SettingsRequest;
import com.legalmetrology.common.settings.dto.SettingsResponse;

import java.util.List;
import java.util.UUID;

public interface SettingsService {

    List<SettingsResponse> listAll();

    SettingsResponse getByKey(String key);

    SettingsResponse upsert(SettingsRequest request, UUID updatedByUserId);
}
