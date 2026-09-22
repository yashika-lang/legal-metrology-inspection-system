import { NavLink } from "react-router-dom";
import {
  LayoutDashboard,
  ScanLine,
  Package,
  FileText,
  BarChart3,
  History,
  Sparkles,
  ScrollText,
  Settings,
  ShieldCheck,
} from "lucide-react";
import { ROUTES } from "@/constants/routes";
import { useAuth } from "@/contexts/AuthContext";
import { cn } from "@/lib/cn";

/**
 * `requiresRole` mirrors each page's real backend `@PreAuthorize`. History
 * is hidden entirely for roles that can't see any of it (ADMIN or
 * SENIOR_OFFICER only, nothing useful behind it otherwise). Settings stays
 * visible for everyone — its System Settings section is ADMIN-only, but
 * every user has a real Account section on the same page, so hiding the
 * whole destination would remove a genuinely useful view for Inspectors.
 */
const NAV_ITEMS = [
  { to: ROUTES.dashboard, label: "Dashboard", icon: LayoutDashboard, end: true },
  { to: ROUTES.newInspection, label: "New Inspection", icon: ScanLine },
  { to: ROUTES.products, label: "Products", icon: Package },
  { to: ROUTES.reports, label: "Reports", icon: FileText },
  { to: ROUTES.analytics, label: "Analytics", icon: BarChart3 },
  { to: ROUTES.history, label: "History", icon: History, requiresRole: ["ADMIN", "SENIOR_OFFICER"] },
  { to: ROUTES.copilot, label: "AI Copilot", icon: Sparkles },
  { to: ROUTES.rules, label: "Rules", icon: ScrollText },
  { to: ROUTES.settings, label: "Settings", icon: Settings },
];

interface SidebarProps {
  className?: string;
  onNavigate?: () => void;
}

export function Sidebar({ className, onNavigate }: SidebarProps) {
  const { user } = useAuth();
  const visibleItems = NAV_ITEMS.filter(
    (item) => !item.requiresRole || item.requiresRole.some((r) => user?.roles.includes(r)),
  );

  return (
    <aside className={cn("flex h-dvh w-60 shrink-0 flex-col border-r border-border bg-surface", className)}>
      <div className="flex h-14 items-center gap-2 px-4">
        <div className="bg-gradient-ai flex size-7 items-center justify-center rounded-md text-white">
          <ShieldCheck className="size-4" />
        </div>
        <div className="leading-tight">
          <p className="text-sm font-semibold text-foreground">Nirikshan AI</p>
          <p className="text-[10px] text-faint-foreground">Officer Portal</p>
        </div>
      </div>

      <nav className="flex-1 space-y-0.5 overflow-y-auto px-2.5 py-2">
        {visibleItems.map((item) => (
          <NavLink
            key={item.to}
            to={item.to}
            end={item.end}
            onClick={onNavigate}
            className={({ isActive }) =>
              cn(
                "flex items-center gap-2.5 rounded-md px-2.5 py-1.5 text-sm font-medium transition-colors",
                isActive
                  ? "bg-accent-soft text-accent-soft-foreground"
                  : "text-foreground-secondary hover:bg-surface-sunken hover:text-foreground",
              )
            }
          >
            <item.icon className="size-4 shrink-0" />
            {item.label}
          </NavLink>
        ))}
      </nav>

      <div className="border-t border-border px-3 py-3">
        <p className="text-[10px] text-faint-foreground">
          Rule Engine is the sole authority on compliance decisions.
        </p>
      </div>
    </aside>
  );
}
