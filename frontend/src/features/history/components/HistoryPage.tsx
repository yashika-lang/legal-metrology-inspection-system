import { useState } from "react";
import { History, ShieldOff } from "lucide-react";
import type { AxiosError } from "axios";
import { PageHeader } from "@/components/common/PageHeader";
import { Pagination } from "@/components/common/Pagination";
import { EmptyState } from "@/components/common/EmptyState";
import { Skeleton } from "@/components/ui/skeleton";
import { Input } from "@/components/ui/input";
import { Badge } from "@/components/ui/badge";
import { useAuditLogs } from "../hooks/useAuditLogs";

export function HistoryPage() {
  const [page, setPage] = useState(0);
  const [entityType, setEntityType] = useState("");
  const [entityId, setEntityId] = useState("");
  const { data, isLoading, isError, error } = useAuditLogs({
    entityType: entityType || undefined,
    entityId: entityId || undefined,
    page,
    size: 20,
  });

  const status = (error as AxiosError)?.response?.status;
  const forbidden = isError && status === 403;

  return (
    <div className="flex h-full flex-col overflow-hidden">
      <PageHeader title="Audit Log" description="Full history of state changes across the system." />

      {forbidden ? (
        <EmptyState
          icon={ShieldOff}
          title="Access restricted"
          description="The Audit Log requires an Admin or Senior Officer role. Your account is signed in as an Inspector."
          className="h-full"
        />
      ) : (
        <>
          <div className="flex flex-wrap gap-2 border-b border-border px-4 py-3 sm:px-6">
            <Input
              value={entityType}
              onChange={(e) => {
                setEntityType(e.target.value);
                setPage(0);
              }}
              placeholder="Filter by entity type (e.g. INSPECTION)"
              className="max-w-xs"
            />
            <Input
              value={entityId}
              onChange={(e) => {
                setEntityId(e.target.value);
                setPage(0);
              }}
              placeholder="Filter by entity id (UUID)"
              className="max-w-xs"
            />
          </div>

          <div className="min-h-0 flex-1 overflow-auto">
            {isLoading ? (
              <div className="space-y-2 p-4">{Array.from({ length: 8 }).map((_, i) => <Skeleton key={i} className="h-10 w-full" />)}</div>
            ) : !data || data.items.length === 0 ? (
              <EmptyState icon={History} title="No audit log entries" description="Nothing matches these filters." className="h-full" />
            ) : (
              <div className="overflow-x-auto">
              <table className="w-full min-w-[640px] text-left text-sm">
                <thead className="sticky top-0 border-b border-border bg-surface text-[11px] uppercase text-muted-foreground">
                  <tr>
                    <th className="px-4 py-2 font-medium sm:px-6">User</th>
                    <th className="px-3 py-2 font-medium">Action</th>
                    <th className="px-3 py-2 font-medium">Entity</th>
                    <th className="px-3 py-2 font-medium">IP</th>
                    <th className="px-3 py-2 font-medium">When</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-border">
                  {data.items.map((log) => (
                    <tr key={log.id} className="hover:bg-surface-sunken">
                      <td className="px-4 py-2.5 sm:px-6">{log.userName}</td>
                      <td className="px-3 py-2.5">
                        <Badge variant="accent">{log.action}</Badge>
                      </td>
                      <td className="px-3 py-2.5 font-mono text-xs text-muted-foreground">
                        {log.entityType} · {log.entityId.slice(0, 8)}
                      </td>
                      <td className="px-3 py-2.5 font-mono text-xs text-faint-foreground">{log.ipAddress}</td>
                      <td className="px-3 py-2.5 text-xs text-faint-foreground">{new Date(log.createdAt).toLocaleString()}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
              </div>
            )}
          </div>

          {data && <Pagination page={data.page} totalPages={data.totalPages} totalItems={data.totalItems} onPageChange={setPage} />}
        </>
      )}
    </div>
  );
}
