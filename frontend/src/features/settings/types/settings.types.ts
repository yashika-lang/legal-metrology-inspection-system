/** Mirrors `com.legalmetrology.common.settings.dto.SettingsResponse`. */
export interface SettingsResponse {
  id: string;
  key: string;
  value: string;
  updatedAt: string;
}

/** Mirrors `com.legalmetrology.common.settings.dto.SettingsRequest`. */
export interface SettingsRequest {
  key: string;
  value: string;
}
