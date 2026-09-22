import { ChevronLeft, ChevronRight } from "lucide-react";
import { Button } from "@/components/ui/button";

/** Drives directly off the backend's real `PagedResponse<T>` shape — no client-side page math beyond what it already returns. */
export function Pagination({
  page,
  totalPages,
  totalItems,
  onPageChange,
}: {
  page: number;
  totalPages: number;
  totalItems: number;
  onPageChange: (page: number) => void;
}) {
  if (totalItems === 0) return null;

  return (
    <div className="flex items-center justify-between gap-3 border-t border-border px-4 py-3 sm:px-6">
      <p className="text-xs text-muted-foreground">
        Page <span className="tabular font-medium text-foreground">{page + 1}</span> of{" "}
        <span className="tabular font-medium text-foreground">{Math.max(totalPages, 1)}</span> ·{" "}
        <span className="tabular">{totalItems}</span> total
      </p>
      <div className="flex items-center gap-1.5">
        <Button variant="outline" size="sm" disabled={page <= 0} onClick={() => onPageChange(page - 1)}>
          <ChevronLeft className="size-3.5" />
          Prev
        </Button>
        <Button variant="outline" size="sm" disabled={page + 1 >= totalPages} onClick={() => onPageChange(page + 1)}>
          Next
          <ChevronRight className="size-3.5" />
        </Button>
      </div>
    </div>
  );
}
