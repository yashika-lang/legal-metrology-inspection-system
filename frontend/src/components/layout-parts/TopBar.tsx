import { useNavigate } from "react-router-dom";
import { Search, Bell, Sun, Moon, Plus, Menu } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Avatar, AvatarFallback } from "@/components/ui/avatar";
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuLabel,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";
import { useTheme } from "@/contexts/ThemeContext";
import { useAuth } from "@/contexts/AuthContext";
import { ROUTES } from "@/constants/routes";

function initials(name: string) {
  return name
    .split(" ")
    .map((part) => part[0])
    .slice(0, 2)
    .join("")
    .toUpperCase();
}

export function TopBar({ onMenuClick }: { onMenuClick?: () => void }) {
  const { theme, toggleTheme } = useTheme();
  const { user, logout } = useAuth();
  const navigate = useNavigate();

  return (
    <header className="flex h-14 shrink-0 items-center gap-2 border-b border-border bg-surface-raised/80 px-3 backdrop-blur-sm sm:gap-3 sm:px-4">
      <Button variant="ghost" size="icon" className="lg:hidden" onClick={onMenuClick} aria-label="Open navigation">
        <Menu className="size-5" />
      </Button>

      <div className="relative hidden w-full max-w-sm sm:block">
        <Search className="pointer-events-none absolute left-2.5 top-1/2 size-3.5 -translate-y-1/2 text-faint-foreground" />
        <input
          type="text"
          placeholder="Search inspections, products, rules…"
          className="h-8 w-full rounded-md border border-border bg-surface pl-8 pr-12 text-xs text-foreground placeholder:text-faint-foreground focus-visible:border-ring focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-accent-ring"
        />
        <kbd className="pointer-events-none absolute right-2 top-1/2 -translate-y-1/2 rounded border border-border bg-surface-sunken px-1.5 py-0.5 font-mono text-[10px] text-faint-foreground">
          ⌘K
        </kbd>
      </div>

      <Button variant="ghost" size="icon" className="sm:hidden" aria-label="Search">
        <Search className="size-4" />
      </Button>

      <div className="ml-auto flex items-center gap-1 sm:gap-1.5">
        <Button variant="ai" size="sm" onClick={() => navigate(ROUTES.newInspection)} className="hidden sm:inline-flex">
          <Plus className="size-3.5" />
          New Inspection
        </Button>
        <Button variant="ai" size="icon" onClick={() => navigate(ROUTES.newInspection)} className="sm:hidden" aria-label="New inspection">
          <Plus className="size-4" />
        </Button>

        <Button variant="ghost" size="icon" onClick={toggleTheme} aria-label="Toggle theme">
          {theme === "dark" ? <Sun className="size-4" /> : <Moon className="size-4" />}
        </Button>

        <Button variant="ghost" size="icon" aria-label="Notifications" className="relative hidden sm:inline-flex">
          <Bell className="size-4" />
          <span className="absolute right-2 top-2 size-1.5 rounded-full bg-critical" />
        </Button>

        <DropdownMenu>
          <DropdownMenuTrigger className="ml-1 rounded-full outline-none focus-visible:ring-2 focus-visible:ring-accent-ring">
            <Avatar>
              <AvatarFallback>{user ? initials(user.fullName) : "?"}</AvatarFallback>
            </Avatar>
          </DropdownMenuTrigger>
          <DropdownMenuContent align="end">
            <DropdownMenuLabel>
              <p className="text-foreground">{user?.fullName}</p>
              <p className="font-normal text-faint-foreground">{user?.email}</p>
            </DropdownMenuLabel>
            <DropdownMenuSeparator />
            <DropdownMenuItem onSelect={() => navigate(ROUTES.settings)}>Settings</DropdownMenuItem>
            <DropdownMenuSeparator />
            <DropdownMenuItem onSelect={logout} className="text-critical focus:bg-critical-soft">
              Sign out
            </DropdownMenuItem>
          </DropdownMenuContent>
        </DropdownMenu>
      </div>
    </header>
  );
}
