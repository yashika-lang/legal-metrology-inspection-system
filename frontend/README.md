# Frontend — React + Vite + TypeScript

See [`../docs/ARCHITECTURE.md`](../docs/ARCHITECTURE.md) §3 for the full
rationale. Quick orientation:

- `src/features/*` — one folder per domain (auth, inspections, products,
  scans, violations, rules, reports, ai-assistant, ai-vision, analytics,
  history, settings, admin-users, admin-roles, offline-sync). Each has
  `components/ hooks/ api/ types/ schemas/`.
- `src/components/*` — cross-feature reusable UI (`ui/` = shadcn primitives,
  `common/`, `charts/`, `forms/`, `scanners/`, `layout-parts/`).
- `src/{hooks,contexts,layouts,routes,services,utils,constants,types,styles,i18n,lib}`
  — app-wide, feature-agnostic code.

Not yet initialized as a Vite project — scaffold with
`npm create vite@latest . -- --template react-ts` from this directory,
then wire in Tailwind, shadcn/ui, React Router, TanStack Query, React Hook
Form + Zod, Framer Motion, and Recharts per the architecture doc before
moving files into the structure above.
