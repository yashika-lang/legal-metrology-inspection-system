package com.legalmetrology.common.settings.mapper;

import com.legalmetrology.common.settings.dto.SettingsResponse;
import com.legalmetrology.common.settings.entity.Settings;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface SettingsMapper {

    default SettingsResponse toResponse(Settings settings) {
        if (settings == null) {
            return null;
        }
        return new SettingsResponse(settings.getId(), settings.getKey(), settings.getValue(), settings.getUpdatedAt());
    }
}
