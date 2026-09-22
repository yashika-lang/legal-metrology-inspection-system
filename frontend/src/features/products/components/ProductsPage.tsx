import { useState, type FormEvent } from "react";
import { Package, Plus, Search, Trash2, Loader2, Barcode, Check, X } from "lucide-react";
import { motion } from "framer-motion";
import { PageHeader } from "@/components/common/PageHeader";
import { Pagination } from "@/components/common/Pagination";
import { EmptyState } from "@/components/common/EmptyState";
import { Button } from "@/components/ui/button";
import { Input, Label } from "@/components/ui/input";
import { Skeleton } from "@/components/ui/skeleton";
import { Badge } from "@/components/ui/badge";
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogDescription, DialogBody, DialogFooter } from "@/components/ui/dialog";
import { Select, SelectTrigger, SelectValue, SelectContent, SelectItem } from "@/components/ui/select";
import { useAuth } from "@/contexts/AuthContext";
import {
  useProducts,
  useProductCategories,
  useCreateProductCategory,
  useManufacturers,
  useCreateManufacturer,
  useCreateProduct,
  useDeleteProduct,
} from "../hooks/useProducts";

const ADMIN_ROLES = ["ADMIN", "SENIOR_OFFICER"];

export function ProductsPage() {
  const { user } = useAuth();
  const canManage = !!user && user.roles.some((r) => ADMIN_ROLES.includes(r));

  const [query, setQuery] = useState("");
  const [page, setPage] = useState(0);
  const [createOpen, setCreateOpen] = useState(false);

  const { data, isLoading } = useProducts({ query: query || undefined, page, size: 12 });
  const deleteProduct = useDeleteProduct();

  return (
    <div className="flex h-full flex-col overflow-hidden">
      <PageHeader
        title="Products"
        description="Product master data used for barcode lookup and inspection linking."
        actions={
          <Button variant="ai" size="sm" onClick={() => setCreateOpen(true)}>
            <Plus className="size-3.5" />
            Add Product
          </Button>
        }
      />

      <div className="border-b border-border px-4 py-3 sm:px-6">
        <div className="relative max-w-sm">
          <Search className="pointer-events-none absolute left-2.5 top-1/2 size-3.5 -translate-y-1/2 text-faint-foreground" />
          <Input
            value={query}
            onChange={(e) => {
              setQuery(e.target.value);
              setPage(0);
            }}
            placeholder="Search products by name…"
            className="pl-8"
          />
        </div>
      </div>

      <div className="min-h-0 flex-1 overflow-y-auto p-4 sm:p-6">
        {isLoading ? (
          <div className="grid grid-cols-1 gap-3 sm:grid-cols-2 lg:grid-cols-3">
            {Array.from({ length: 6 }).map((_, i) => <Skeleton key={i} className="h-32" />)}
          </div>
        ) : !data || data.items.length === 0 ? (
          <EmptyState
            icon={Package}
            title="No products found"
            description={query ? "Try a different search term." : "Add the first product to get started."}
            className="h-full"
          />
        ) : (
          <div className="grid grid-cols-1 gap-3 sm:grid-cols-2 lg:grid-cols-3">
            {data.items.map((product, index) => (
              <motion.div
                key={product.id}
                initial={{ opacity: 0, y: 6 }}
                animate={{ opacity: 1, y: 0 }}
                transition={{ delay: index * 0.03 }}
                className="card-hover flex flex-col gap-2 rounded-xl border border-border bg-surface-raised p-4"
              >
                <div className="flex items-start justify-between gap-2">
                  <p className="text-sm font-medium text-foreground">{product.name}</p>
                  {canManage && (
                    <Button
                      variant="ghost"
                      size="icon"
                      className="size-6 shrink-0 text-critical hover:bg-critical-soft"
                      disabled={deleteProduct.isPending}
                      onClick={() => deleteProduct.mutate(product.id)}
                      aria-label="Delete product"
                    >
                      <Trash2 className="size-3.5" />
                    </Button>
                  )}
                </div>
                <div className="flex flex-wrap gap-1.5">
                  {product.categoryName && <Badge variant="neutral">{product.categoryName}</Badge>}
                  {product.manufacturerName && <Badge variant="accent">{product.manufacturerName}</Badge>}
                </div>
                <div className="mt-auto flex items-center justify-between pt-2 text-[11px] text-faint-foreground">
                  {product.barcode ? (
                    <span className="flex items-center gap-1 font-mono">
                      <Barcode className="size-3" /> {product.barcode}
                    </span>
                  ) : (
                    <span>No barcode</span>
                  )}
                  {product.defaultUnit && <span>{product.defaultUnit}</span>}
                </div>
              </motion.div>
            ))}
          </div>
        )}
      </div>

      {data && (
        <Pagination page={data.page} totalPages={data.totalPages} totalItems={data.totalItems} onPageChange={setPage} />
      )}

      <CreateProductDialog open={createOpen} onOpenChange={setCreateOpen} />
    </div>
  );
}

/**
 * There is no seed data for product categories/manufacturers — they're
 * created by whoever needs one first. A plain dropdown with nothing to
 * pick from is a dead end, so this combines the picker with an inline
 * "+ create new" affordance instead of requiring a separate management
 * screen just to unblock adding a product.
 */
function SelectOrCreate({
  label,
  placeholder,
  items,
  value,
  onChange,
  onCreate,
  isCreating,
}: {
  label: string;
  placeholder: string;
  items: { id: string; name: string }[] | undefined;
  value: string;
  onChange: (id: string) => void;
  onCreate: (name: string) => Promise<{ id: string }>;
  isCreating: boolean;
}) {
  const [creatingNew, setCreatingNew] = useState(false);
  const [newName, setNewName] = useState("");

  async function handleCreate() {
    if (!newName.trim()) return;
    const created = await onCreate(newName.trim());
    onChange(created.id);
    setNewName("");
    setCreatingNew(false);
  }

  if (creatingNew) {
    return (
      <div className="space-y-1.5">
        <Label>{label}</Label>
        <div className="flex gap-1.5">
          <Input
            autoFocus
            value={newName}
            onChange={(e) => setNewName(e.target.value)}
            placeholder={`New ${label.toLowerCase()} name`}
            onKeyDown={(e) => {
              if (e.key === "Enter") {
                e.preventDefault();
                handleCreate();
              }
            }}
          />
          <Button type="button" size="icon" variant="secondary" disabled={isCreating || !newName.trim()} onClick={handleCreate}>
            {isCreating ? <Loader2 className="size-4 animate-spin" /> : <Check className="size-4" />}
          </Button>
          <Button type="button" size="icon" variant="ghost" onClick={() => setCreatingNew(false)}>
            <X className="size-4" />
          </Button>
        </div>
      </div>
    );
  }

  return (
    <div className="space-y-1.5">
      <Label>{label}</Label>
      <div className="flex gap-1.5">
        <Select value={value} onValueChange={onChange}>
          <SelectTrigger className="flex-1"><SelectValue placeholder={placeholder} /></SelectTrigger>
          <SelectContent>
            {items?.map((item) => (
              <SelectItem key={item.id} value={item.id}>{item.name}</SelectItem>
            ))}
          </SelectContent>
        </Select>
        <Button type="button" size="icon" variant="secondary" onClick={() => setCreatingNew(true)} title={`Add new ${label.toLowerCase()}`}>
          <Plus className="size-4" />
        </Button>
      </div>
    </div>
  );
}

function CreateProductDialog({ open, onOpenChange }: { open: boolean; onOpenChange: (open: boolean) => void }) {
  const { data: categories } = useProductCategories();
  const createCategory = useCreateProductCategory();
  const { data: manufacturers } = useManufacturers();
  const createManufacturer = useCreateManufacturer();
  const createProduct = useCreateProduct();

  const [name, setName] = useState("");
  const [categoryId, setCategoryId] = useState<string>("");
  const [manufacturerId, setManufacturerId] = useState<string>("");
  const [barcode, setBarcode] = useState("");
  const [defaultUnit, setDefaultUnit] = useState("");
  const [error, setError] = useState<string | null>(null);

  function reset() {
    setName("");
    setCategoryId("");
    setManufacturerId("");
    setBarcode("");
    setDefaultUnit("");
    setError(null);
  }

  async function handleSubmit(e: FormEvent) {
    e.preventDefault();
    setError(null);
    try {
      await createProduct.mutateAsync({
        name,
        categoryId: categoryId || null,
        manufacturerId: manufacturerId || null,
        barcode: barcode.trim() || null,
        defaultUnit: defaultUnit.trim() || null,
      });
      reset();
      onOpenChange(false);
    } catch (err) {
      const message =
        (err as { response?: { data?: { message?: string } } })?.response?.data?.message ?? "Could not create product.";
      setError(message);
    }
  }

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent>
        <form onSubmit={handleSubmit}>
          <DialogHeader>
            <DialogTitle>Add Product</DialogTitle>
            <DialogDescription>Registers real product master data used across inspections.</DialogDescription>
          </DialogHeader>
          <DialogBody>
            <div className="space-y-1.5">
              <Label htmlFor="product-name">Name</Label>
              <Input id="product-name" required value={name} onChange={(e) => setName(e.target.value)} placeholder="Tata Salt 1kg" />
            </div>

            <SelectOrCreate
              label="Category"
              placeholder="None"
              items={categories}
              value={categoryId}
              onChange={setCategoryId}
              onCreate={(name) => createCategory.mutateAsync(name)}
              isCreating={createCategory.isPending}
            />

            <SelectOrCreate
              label="Manufacturer"
              placeholder="None"
              items={manufacturers}
              value={manufacturerId}
              onChange={setManufacturerId}
              onCreate={(name) => createManufacturer.mutateAsync(name)}
              isCreating={createManufacturer.isPending}
            />

            <div className="grid grid-cols-2 gap-3">
              <div className="space-y-1.5">
                <Label htmlFor="product-barcode">Barcode</Label>
                <Input id="product-barcode" value={barcode} onChange={(e) => setBarcode(e.target.value)} placeholder="8901058854488" />
              </div>
              <div className="space-y-1.5">
                <Label htmlFor="product-unit">Default Unit</Label>
                <Input id="product-unit" value={defaultUnit} onChange={(e) => setDefaultUnit(e.target.value)} placeholder="kg" />
              </div>
            </div>

            {error && <p className="text-xs text-critical">{error}</p>}
          </DialogBody>
          <DialogFooter>
            <Button type="button" variant="ghost" size="sm" onClick={() => onOpenChange(false)}>Cancel</Button>
            <Button type="submit" variant="ai" size="sm" disabled={createProduct.isPending || !name.trim()}>
              {createProduct.isPending && <Loader2 className="size-3.5 animate-spin" />}
              Create
            </Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  );
}
