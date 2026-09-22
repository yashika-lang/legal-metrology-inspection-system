import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { rulesApi, type ListRulesParams } from "../api/rulesApi";
import { queryKeys } from "@/constants/queryKeys";

export function useRules(params: ListRulesParams) {
  return useQuery({ queryKey: queryKeys.rules(params), queryFn: () => rulesApi.list(params) });
}

export function useRuleHistory(ruleCode: string | null) {
  return useQuery({
    queryKey: queryKeys.ruleHistory(ruleCode ?? ""),
    queryFn: () => rulesApi.history(ruleCode!),
    enabled: !!ruleCode,
  });
}

export function useDeactivateRule() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (id: string) => rulesApi.deactivate(id),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ["rules"] }),
  });
}

export function useRefreshRuleCache() {
  return useMutation({ mutationFn: () => rulesApi.refreshCache() });
}
