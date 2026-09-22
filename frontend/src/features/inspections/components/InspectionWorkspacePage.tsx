import { useParams } from "react-router-dom";
import { InspectionWorkspace } from "./InspectionWorkspace";

export function InspectionWorkspacePage() {
  const { inspectionId } = useParams<{ inspectionId: string }>();
  if (!inspectionId) return null;
  return <InspectionWorkspace inspectionId={inspectionId} />;
}
