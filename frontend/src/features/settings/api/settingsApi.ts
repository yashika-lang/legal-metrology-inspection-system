import { apiClient, unwrap } from "@/services/apiClient";
import type { ApiResponse } from "@/types/api.types";
import type { SettingsRequest, SettingsResponse } from "../types/settings.types";

export const settingsApi = {
  list: () => unwrap(apiClient.get<ApiResponse<SettingsResponse[]>>("/settings")),

  getByKey: (key: string) => unwrap(apiClient.get<ApiResponse<SettingsResponse>>(`/settings/${key}`)),

  upsert: (payload: SettingsRequest) => unwrap(apiClient.put<ApiResponse<SettingsResponse>>("/settings", payload)),
};
