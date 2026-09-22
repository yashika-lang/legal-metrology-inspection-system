import { useState } from "react";
import { Outlet } from "react-router-dom";
import { Sidebar } from "@/components/layout-parts/Sidebar";
import { TopBar } from "@/components/layout-parts/TopBar";
import { Sheet, SheetContent } from "@/components/ui/sheet";

export function AppLayout() {
  const [mobileNavOpen, setMobileNavOpen] = useState(false);

  return (
    // h-dvh, not h-screen — see LoginPage.tsx for why 100vh is unsafe on real mobile browsers.
    <div className="flex h-dvh w-screen overflow-hidden bg-background">
      <Sidebar className="hidden lg:flex" />

      <Sheet open={mobileNavOpen} onOpenChange={setMobileNavOpen}>
        <SheetContent side="left" className="w-64 p-0 sm:max-w-64">
          <Sidebar className="w-full border-r-0" onNavigate={() => setMobileNavOpen(false)} />
        </SheetContent>
      </Sheet>

      <div className="flex min-w-0 flex-1 flex-col">
        <TopBar onMenuClick={() => setMobileNavOpen(true)} />
        <main className="min-h-0 flex-1 overflow-hidden">
          <Outlet />
        </main>
      </div>
    </div>
  );
}
