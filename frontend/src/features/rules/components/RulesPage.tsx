import { useState } from "react";
import { ScrollText, RefreshCw, History, Ban, Loader2 } from "lucide-react";
import { motion } from "framer-motion";
import { PageHeader } from "@/components/common/PageHeader";
import { Pagination } from "@/components/common/Pagination";
import { EmptyState } from "@/components/common/EmptyState";
import { SeverityBadge } from "@/components/common/SeverityBadge";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Skeleton } from "@/components/ui/skeleton";
import { Sheet, SheetContent, SheetHeader, SheetTitle, SheetDescription } from "@/components/ui/sheet";
import { useAuth } from "@/contexts/AuthContext";
import { useRules, useRuleHistory, useDeactivateRule, useRefreshRuleCache } from "../hooks/useRules";
import type { RuleResponse } from "../types/rule.types";

const ADMIN_ROLES = ["ADMIN", "SENIOR_OFFICER"];

export function RulesPage() {
  const { user } = useAuth();
  const canManage = !!user && user.roles.some((r) => ADMIN_ROLES.includes(r));

  const [page, setPage] = useState(0);
  const [activeOnly, setActiveOnly] = useState(true);
  const [selected, setSelected] = useState<RuleResponse | null>(null);

  const { data, isLoading } = useRules({ activeOnly, page, size: 20, sort: "ruleCode" });
  const refreshCache = useRefreshRuleCache();

  return (
    <div className="flex h-full flex-col overflow-hidden">
      <PageHeader
        title="Rules"
        description="The versioned, database-driven Legal Metrology rules the Rule Engine evaluates."
        actions={
          canManage && (
            <Button variant="outline" size="sm" disabled={refreshCache.isPending} onClick={() => refreshCache.mutate()}>
              {refreshCache.isPending ? <Loader2 className="size-3.5 animate-spin" /> : <RefreshCw className="size-3.5" />}
              Refresh Cache
            </Button>
          )
        }
      />

      <div className="flex items-center gap-2 border-b border-border px-4 py-3 sm:px-6">
        <Button variant={activeOnly ? "primary" : "outline"} size="sm" onClick={() => { setActiveOnly(true); setPage(0); }}>
          Active Only
        </Button>
        <Button variant={!activeOnly ? "primary" : "outline"} size="sm" onClick={() => { setActiveOnly(false); setPage(0); }}>
          All Versions
        </Button>
      </div>

      <div className="min-h-0 flex-1 overflow-y-auto p-4 sm:p-6">
        {isLoading ? (
          <div className="space-y-2">{Array.from({ length: 8 }).map((_, i) => <Skeleton key={i} className="h-16 w-full" />)}</div>
        ) : !data || data.items.length === 0 ? (
          <EmptyState icon={ScrollText} title="No rules found" className="h-full" />
        ) : (
          <ul className="space-y-2">
            {data.items.map((rule, index) => (
              <motion.li
                key={rule.id}
                initial={{ opacity: 0, y: 4 }}
                animate={{ opacity: 1, y: 0 }}
                transition={{ delay: index * 0.02 }}
              >
                <button
                  onClick={() => setSelected(rule)}
                  className="card-hover flex w-full flex-wrap items-center justify-between gap-2 rounded-lg border border-border bg-surface-raised p-3 text-left"
                >
                  <div className="min-w-0">
                    <div className="flex items-center gap-2">
                      <span className="font-mono text-xs text-faint-foreground">{rule.ruleCode}</span>
                      <span className="text-[10px] text-faint-foreground">v{rule.version}</span>
                      {!rule.active && <Badge variant="outline">Inactive</Badge>}
                    </div>
                    <p className="mt-0.5 truncate text-sm font-medium text-foreground">{rule.title}</p>
                  </div>
                  <div className="flex items-center gap-2">
                    {rule.mandatory && <Badge variant="accent">Mandatory</Badge>}
                    <SeverityBadge severity={rule.severity} />
                  </div>
                </button>
              </motion.li>
            ))}
          </ul>
        )}
      </div>

      {data && <Pagination page={data.page} totalPages={data.totalPages} totalItems={data.totalItems} onPageChange={setPage} />}

      <Sheet open={!!selected} onOpenChange={(open) => !open && setSelected(null)}>
        {selected && <RuleDetail rule={selected} canManage={canManage} onClose={() => setSelected(null)} />}
      </Sheet>
    </div>
  );
}

function RuleDetail({ rule, canManage, onClose }: { rule: RuleResponse; canManage: boolean; onClose: () => void }) {
  const { data: history } = useRuleHistory(rule.ruleCode);
  const deactivate = useDeactivateRule();

  let prettyExpression = rule.validationExpression;
  try {
    prettyExpression = JSON.stringify(JSON.parse(rule.validationExpression), null, 2);
  } catch {
    // not valid JSON — show raw
  }

  return (
    <SheetContent side="right" className="w-full overflow-y-auto sm:max-w-lg">
      <SheetHeader>
        <SheetTitle className="flex items-center gap-2">
          <span className="font-mono text-sm">{rule.ruleCode}</span>
          <SeverityBadge severity={rule.severity} />
        </SheetTitle>
        <SheetDescription>{rule.title}</SheetDescription>
      </SheetHeader>

      <div className="space-y-5 overflow-y-auto px-5 py-4">
        {rule.description && (
          <div>
            <p className="mb-1 text-xs font-semibold text-foreground">Description</p>
            <p className="text-xs text-foreground-secondary">{rule.description}</p>
          </div>
        )}

        <div className="grid grid-cols-2 gap-3 text-xs">
          <Field label="Validation Type" value={rule.validationType} />
          <Field label="Category" value={rule.category ?? "—"} />
          <Field label="Version" value={String(rule.version)} />
          <Field label="Effective" value={rule.effectiveDate} />
          <Field label="Mandatory" value={rule.mandatory ? "Yes" : "No"} />
          <Field label="Status" value={rule.active ? "Active" : "Inactive"} />
        </div>

        {rule.legalReference && <Field label="Legal Reference" value={rule.legalReference} block />}
        {rule.penaltyReference && <Field label="Penalty Reference" value={rule.penaltyReference} block />}
        {rule.suggestion && <Field label="Suggested Fix" value={rule.suggestion} block />}

        <div>
          <p className="mb-1 text-xs font-semibold text-foreground">Validation Expression</p>
          <pre className="overflow-x-auto rounded-md bg-surface-sunken p-2.5 font-mono text-[11px] text-foreground-secondary">
            {prettyExpression}
          </pre>
        </div>

        {history && history.length > 1 && (
          <div>
            <p className="mb-1.5 flex items-center gap-1.5 text-xs font-semibold text-foreground">
              <History className="size-3.5" /> Version History
            </p>
            <ul className="space-y-1">
              {history.map((h) => (
                <li key={h.id} className="flex items-center justify-between text-[11px] text-muted-foreground">
                  <span>v{h.version} · {h.effectiveDate}</span>
                  <span>{h.active ? "Active" : "Superseded"}</span>
                </li>
              ))}
            </ul>
          </div>
        )}

        {canManage && rule.active && (
          <Button
            variant="destructive"
            size="sm"
            disabled={deactivate.isPending}
            onClick={() => deactivate.mutate(rule.id, { onSuccess: onClose })}
          >
            {deactivate.isPending ? <Loader2 className="size-3.5 animate-spin" /> : <Ban className="size-3.5" />}
            Deactivate Rule
          </Button>
        )}
      </div>
    </SheetContent>
  );
}

function Field({ label, value, block }: { label: string; value: string; block?: boolean }) {
  return (
    <div className={block ? "col-span-2" : undefined}>
      <p className="text-[10px] uppercase tracking-wide text-faint-foreground">{label}</p>
      <p className="mt-0.5 text-foreground-secondary">{value}</p>
    </div>
  );
}
