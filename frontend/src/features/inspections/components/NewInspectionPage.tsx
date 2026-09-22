import { useEffect, useRef } from "react";
import { Navigate } from "react-router-dom";
import { Loader2 } from "lucide-react";
import { useCreateInspection } from "../hooks/useInspection";
import { ROUTES } from "@/constants/routes";

/**
 * Real entry point into the workspace: creates a genuine inspection via
 * `POST /inspections` (region/location/product all optional per the
 * backend contract) and redirects to its workspace. Not a designed
 * "start inspection" form — that's a later deliverable.
 */
export function NewInspectionPage() {
  const createInspection = useCreateInspection();
  const hasFired = useRef(false);

  useEffect(() => {
    if (hasFired.current) return;
    hasFired.current = true;
    createInspection.mutate({});
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  if (createInspection.data) {
    return <Navigate to={ROUTES.workspace(createInspection.data.id)} replace />;
  }

  return (
    <div className="flex h-full items-center justify-center gap-2 text-muted-foreground">
      <Loader2 className="size-4 animate-spin" />
      Starting a new inspection…
    </div>
  );
}
