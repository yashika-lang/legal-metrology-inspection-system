import { createBrowserRouter, RouterProvider } from "react-router-dom";
import { AppLayout } from "@/layouts/AppLayout";
import { ProtectedRoute } from "./ProtectedRoute";
import { LoginPage } from "@/features/auth/components/LoginPage";
import { NewInspectionPage } from "@/features/inspections/components/NewInspectionPage";
import { InspectionWorkspacePage } from "@/features/inspections/components/InspectionWorkspacePage";
import { DashboardPage } from "@/features/dashboard/components/DashboardPage";
import { ProductsPage } from "@/features/products/components/ProductsPage";
import { ReportsPage } from "@/features/reports/components/ReportsPage";
import { AnalyticsPage } from "@/features/analytics/components/AnalyticsPage";
import { HistoryPage } from "@/features/history/components/HistoryPage";
import { RulesPage } from "@/features/rules/components/RulesPage";
import { SettingsPage } from "@/features/settings/components/SettingsPage";
import { CopilotPage } from "@/features/ai-assistant/components/CopilotPage";

const router = createBrowserRouter([
  { path: "/login", element: <LoginPage /> },
  {
    element: <ProtectedRoute />,
    children: [
      {
        element: <AppLayout />,
        children: [
          { path: "/", element: <DashboardPage /> },
          { path: "/inspections/new", element: <NewInspectionPage /> },
          { path: "/inspections/:inspectionId/workspace", element: <InspectionWorkspacePage /> },
          { path: "/products", element: <ProductsPage /> },
          { path: "/reports", element: <ReportsPage /> },
          { path: "/analytics", element: <AnalyticsPage /> },
          { path: "/history", element: <HistoryPage /> },
          { path: "/copilot", element: <CopilotPage /> },
          { path: "/rules", element: <RulesPage /> },
          { path: "/settings", element: <SettingsPage /> },
        ],
      },
    ],
  },
]);

export function AppRouter() {
  return <RouterProvider router={router} />;
}
