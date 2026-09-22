import { useState, type FormEvent } from "react";
import type { AxiosError } from "axios";
import { Settings as SettingsIcon, ShieldOff, Plus, Loader2, Pencil, User, Mail, Shield } from "lucide-react";
import { motion } from "framer-motion";
import { PageHeader } from "@/components/common/PageHeader";
import { EmptyState } from "@/components/common/EmptyState";
import { Skeleton } from "@/components/ui/skeleton";
import { Button } from "@/components/ui/button";
import { Input, Label } from "@/components/ui/input";
import { Badge } from "@/components/ui/badge";
import { useAuth } from "@/contexts/AuthContext";
import { useSettings, useUpsertSetting } from "../hooks/useSettings";

export function SettingsPage() {
  const { user } = useAuth();
  const { data, isLoading, isError, error } = useSettings();
  const upsert = useUpsertSetting();
  const [editingKey, setEditingKey] = useState<string | null>(null);
  const [newKey, setNewKey] = useState("");
  const [newValue, setNewValue] = useState("");
  const [addingNew, setAddingNew] = useState(false);

  const status = (error as AxiosError)?.response?.status;
  const forbidden = isError && status === 403;

  async function handleUpsert(key: string, value: string) {
    await upsert.mutateAsync({ key, value });
    setEditingKey(null);
    setAddingNew(false);
    setNewKey("");
    setNewValue("");
  }

  return (
    <div className="h-full overflow-y-auto">
      <PageHeader title="Settings" description="Account details and system-wide, database-backed configuration." />

      <div className="space-y-6 p-4 sm:p-6">
        {/* Account — real, always visible regardless of role */}
        <div className="rounded-xl border border-border bg-surface-raised p-4">
          <p className="mb-3 text-sm font-semibold text-foreground">Your Account</p>
          <div className="grid grid-cols-1 gap-3 sm:grid-cols-3">
            <AccountField icon={User} label="Full Name" value={user?.fullName ?? "—"} />
            <AccountField icon={Mail} label="Email" value={user?.email ?? "—"} />
            <AccountField icon={Shield} label="Roles" value={user?.roles.join(", ") ?? "—"} />
          </div>
        </div>

        {/* System settings — ADMIN only on the backend */}
        <div className="rounded-xl border border-border bg-surface-raised">
          <div className="flex items-center justify-between border-b border-border p-4">
            <div>
              <p className="text-sm font-semibold text-foreground">System Settings</p>
              <p className="text-xs text-muted-foreground">Runtime-configurable key/value settings (e.g. scoring weights).</p>
            </div>
            {!forbidden && (
              <Button variant="ai" size="sm" onClick={() => setAddingNew(true)}>
                <Plus className="size-3.5" /> Add Setting
              </Button>
            )}
          </div>

          {forbidden ? (
            <EmptyState
              icon={ShieldOff}
              title="Admin access required"
              description="System settings are restricted to the Admin role. Your account is signed in as an Inspector."
              className="py-10"
            />
          ) : isLoading ? (
            <div className="space-y-2 p-4">{Array.from({ length: 4 }).map((_, i) => <Skeleton key={i} className="h-12 w-full" />)}</div>
          ) : (
            <div className="divide-y divide-border">
              {addingNew && (
                <SettingRow
                  isNew
                  keyValue=""
                  value=""
                  onCancel={() => setAddingNew(false)}
                  onSave={handleUpsert}
                  saving={upsert.isPending}
                  draftKey={newKey}
                  setDraftKey={setNewKey}
                  draftValue={newValue}
                  setDraftValue={setNewValue}
                />
              )}
              {!data || data.length === 0 ? (
                !addingNew && <EmptyState icon={SettingsIcon} title="No settings configured yet" className="py-10" />
              ) : (
                data.map((setting) => (
                  <motion.div key={setting.id} initial={{ opacity: 0 }} animate={{ opacity: 1 }}>
                    {editingKey === setting.key ? (
                      <SettingRow
                        keyValue={setting.key}
                        value={setting.value}
                        onCancel={() => setEditingKey(null)}
                        onSave={handleUpsert}
                        saving={upsert.isPending}
                        draftKey={setting.key}
                        setDraftKey={() => {}}
                        draftValue={newValue || setting.value}
                        setDraftValue={setNewValue}
                        keyLocked
                      />
                    ) : (
                      <div className="flex items-center justify-between gap-3 px-4 py-3">
                        <div className="min-w-0">
                          <p className="font-mono text-xs text-foreground">{setting.key}</p>
                          <p className="mt-0.5 truncate text-sm text-foreground-secondary">{setting.value}</p>
                        </div>
                        <div className="flex shrink-0 items-center gap-2">
                          <Badge variant="neutral">{new Date(setting.updatedAt).toLocaleDateString()}</Badge>
                          <Button
                            variant="ghost"
                            size="icon"
                            onClick={() => {
                              setNewValue(setting.value);
                              setEditingKey(setting.key);
                            }}
                          >
                            <Pencil className="size-3.5" />
                          </Button>
                        </div>
                      </div>
                    )}
                  </motion.div>
                ))
              )}
            </div>
          )}
        </div>
      </div>
    </div>
  );
}

function AccountField({ icon: Icon, label, value }: { icon: typeof User; label: string; value: string }) {
  return (
    <div className="flex items-center gap-2.5 rounded-lg bg-surface-sunken p-3">
      <Icon className="size-4 text-muted-foreground" />
      <div className="min-w-0">
        <p className="text-[10px] uppercase text-faint-foreground">{label}</p>
        <p className="truncate text-sm font-medium text-foreground">{value}</p>
      </div>
    </div>
  );
}

function SettingRow({
  isNew,
  keyValue,
  onCancel,
  onSave,
  saving,
  draftKey,
  setDraftKey,
  draftValue,
  setDraftValue,
  keyLocked,
}: {
  isNew?: boolean;
  keyValue: string;
  value: string;
  onCancel: () => void;
  onSave: (key: string, value: string) => void;
  saving: boolean;
  draftKey: string;
  setDraftKey: (v: string) => void;
  draftValue: string;
  setDraftValue: (v: string) => void;
  keyLocked?: boolean;
}) {
  function handleSubmit(e: FormEvent) {
    e.preventDefault();
    onSave(keyLocked ? keyValue : draftKey, draftValue);
  }

  return (
    <form onSubmit={handleSubmit} className="flex flex-wrap items-end gap-2 bg-accent-soft/40 px-4 py-3">
      <div className="min-w-40 flex-1 space-y-1">
        <Label className="text-[10px]">Key</Label>
        <Input value={keyLocked ? keyValue : draftKey} disabled={keyLocked} onChange={(e) => setDraftKey(e.target.value)} placeholder="scoring.severity.critical.weight" className="h-8 font-mono text-xs" />
      </div>
      <div className="min-w-40 flex-1 space-y-1">
        <Label className="text-[10px]">Value</Label>
        <Input value={draftValue} onChange={(e) => setDraftValue(e.target.value)} placeholder="-25" className="h-8 text-xs" />
      </div>
      <div className="flex gap-1.5">
        <Button type="button" variant="ghost" size="sm" onClick={onCancel}>Cancel</Button>
        <Button type="submit" variant="ai" size="sm" disabled={saving || (!keyLocked && !draftKey.trim())}>
          {saving && <Loader2 className="size-3 animate-spin" />}
          {isNew ? "Add" : "Save"}
        </Button>
      </div>
    </form>
  );
}
