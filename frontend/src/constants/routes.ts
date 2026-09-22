export const ROUTES = {
  dashboard: "/",
  newInspection: "/inspections/new",
  workspace: (inspectionId: string) => `/inspections/${inspectionId}/workspace`,
  products: "/products",
  reports: "/reports",
  analytics: "/analytics",
  history: "/history",
  copilot: "/copilot",
  rules: "/rules",
  settings: "/settings",
} as const;
