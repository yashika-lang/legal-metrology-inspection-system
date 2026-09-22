import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { settingsApi } from "../api/settingsApi";
import { queryKeys } from "@/constants/queryKeys";
import type { SettingsRequest } from "../types/settings.types";

export function useSettings() {
  return useQuery({ queryKey: queryKeys.settings, queryFn: settingsApi.list, retry: false });
}

export function useUpsertSetting() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (payload: SettingsRequest) => settingsApi.upsert(payload),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: queryKeys.settings }),
  });
}
