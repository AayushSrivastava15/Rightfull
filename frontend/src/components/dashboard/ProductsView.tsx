import React, { useState } from 'react'
import {
  Package,
  Plus,
  Search,
  KeyRound,
  RotateCw,
  Archive,
  ExternalLink,
  ShieldCheck,
  CheckCircle2,
  Clock,
  MoreVertical,
} from 'lucide-react'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Badge } from '@/components/ui/badge'
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogDescription,
  DialogFooter,
} from '@/components/ui/dialog'
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuTrigger,
} from '@/components/ui/dropdown-menu'
import { apiClient } from '@/services/apiClient'
import { type Product } from '@/types'

interface ProductsViewProps {
  workspaceId: string
  products: Product[]
  canManage: boolean
  isLoading: boolean
  onRefresh: () => void
  isCreateOpen: boolean
  setIsCreateOpen: (open: boolean) => void
}

export function ProductsView({
  workspaceId,
  products,
  canManage,
  isLoading,
  onRefresh,
  isCreateOpen,
  setIsCreateOpen,
}: ProductsViewProps) {
  const [search, setSearch] = useState('')
  const [statusFilter, setStatusFilter] = useState<'ALL' | 'ACTIVE' | 'ARCHIVED'>('ALL')
  const [selectedProduct, setSelectedProduct] = useState<Product | null>(null)
  const [isRotating, setIsRotating] = useState(false)
  const [rotationSuccess, setRotationSuccess] = useState(false)

  // Form state for creating a product
  const [newProductName, setNewProductName] = useState('')
  const [newProductSlug, setNewProductSlug] = useState('')
  const [newProductDesc, setNewProductDesc] = useState('')
  const [isSubmitting, setIsSubmitting] = useState(false)
  const [formError, setFormError] = useState<string | null>(null)

  const handleNameChange = (name: string) => {
    setNewProductName(name)
    // Auto-generate slug from name if not manually edited
    if (!newProductSlug || newProductSlug === newProductName.toLowerCase().replace(/[^a-z0-9]/g, '-')) {
      setNewProductSlug(name.toLowerCase().trim().replace(/[^a-z0-9]+/g, '-').replace(/^-|-$/g, ''))
    }
  }

  const handleCreate = async (e: React.FormEvent) => {
    e.preventDefault()
    if (!newProductName.trim() || !newProductSlug.trim()) {
      setFormError('Product name and slug are required.')
      return
    }

    setIsSubmitting(true)
    setFormError(null)

    try {
      await apiClient.createProduct(workspaceId, {
        name: newProductName.trim(),
        slug: newProductSlug.trim(),
        ...(newProductDesc.trim() ? { description: newProductDesc.trim() } : {}),
      })
      setIsCreateOpen(false)
      setNewProductName('')
      setNewProductSlug('')
      setNewProductDesc('')
      onRefresh()
    } catch (err: any) {
      setFormError(err.message || 'Failed to register product.')
    } finally {
      setIsSubmitting(false)
    }
  }

  const handleRotateKey = async (productId: string) => {
    setIsRotating(true)
    try {
      await apiClient.rotateSigningKey(workspaceId, productId)
      setRotationSuccess(true)
      setTimeout(() => setRotationSuccess(false), 3000)
      onRefresh()
    } catch (err: any) {
      alert(`Key rotation failed: ${err.message}`)
    } finally {
      setIsRotating(false)
    }
  }

  const handleArchive = async (productId: string) => {
    if (!confirm('Are you sure you want to archive this product? License plans will also be archived.')) return
    try {
      await apiClient.archiveProduct(workspaceId, productId)
      onRefresh()
      if (selectedProduct?.id === productId) {
        setSelectedProduct(null)
      }
    } catch (err: any) {
      alert(`Failed to archive product: ${err.message}`)
    }
  }

  // Filter products
  const filtered = products.filter((p) => {
    const matchesSearch =
      p.name.toLowerCase().includes(search.toLowerCase()) ||
      (p.slug || '').toLowerCase().includes(search.toLowerCase()) ||
      (p.description || '').toLowerCase().includes(search.toLowerCase())

    const matchesStatus =
      statusFilter === 'ALL' ||
      (statusFilter === 'ACTIVE' && p.status === 'ACTIVE') ||
      (statusFilter === 'ARCHIVED' && p.status === 'ARCHIVED')

    return matchesSearch && matchesStatus
  })

  return (
    <div className="space-y-6">
      {/* Top Action Bar */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <h2 className="text-xl font-bold tracking-tight text-foreground">
            Product Catalog & Signing Keys
          </h2>
          <p className="text-xs text-muted-foreground mt-0.5">
            Register client software applications and configure Ed25519 asymmetric signing keys.
          </p>
        </div>

        {canManage && (
          <Button onClick={() => setIsCreateOpen(true)} size="sm" className="gap-1.5 shadow-sm">
            <Plus className="h-4 w-4" />
            Register Product
          </Button>
        )}
      </div>

      {/* Filter and Search Bar */}
      <div className="flex flex-col sm:flex-row items-stretch sm:items-center gap-3">
        <div className="relative flex-1">
          <Search className="absolute left-3 top-2.5 h-4 w-4 text-muted-foreground" />
          <Input
            placeholder="Search products by name, slug, or description..."
            value={search}
            onChange={(e) => setSearch(e.target.value)}
            className="pl-9 text-xs h-9"
          />
        </div>

        <div className="flex items-center gap-1 border border-border/70 rounded-lg p-0.5 bg-card">
          {(['ALL', 'ACTIVE', 'ARCHIVED'] as const).map((filter) => (
            <button
              key={filter}
              onClick={() => setStatusFilter(filter)}
              className={`rounded-md px-3 py-1 text-xs font-medium transition-colors ${
                statusFilter === filter
                  ? 'bg-primary text-primary-foreground font-semibold shadow-xs'
                  : 'text-muted-foreground hover:text-foreground'
              }`}
            >
              {filter === 'ALL' ? 'All' : filter === 'ACTIVE' ? 'Active' : 'Archived'}
            </button>
          ))}
        </div>
      </div>

      {/* Product List */}
      {isLoading ? (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
          {[1, 2, 3].map((i) => (
            <div key={i} className="h-44 rounded-xl border border-border/60 bg-card p-5 animate-pulse" />
          ))}
        </div>
      ) : filtered.length === 0 ? (
        <div className="flex flex-col items-center justify-center rounded-xl border border-dashed border-border/80 bg-card/50 py-16 text-center">
          <Package className="h-10 w-10 text-muted-foreground/50 mb-3" />
          <h3 className="text-sm font-semibold text-foreground">
            {search ? 'No matching products found' : 'No software products registered'}
          </h3>
          <p className="text-xs text-muted-foreground max-w-sm mt-1">
            {search
              ? 'Try adjusting your search criteria or clear your status filters.'
              : 'Register your first software application to provision an Ed25519 signing key pair and start defining license rules.'}
          </p>
          {canManage && !search && (
            <Button size="sm" onClick={() => setIsCreateOpen(true)} className="mt-4 gap-1.5">
              <Plus className="h-4 w-4" />
              Register Product
            </Button>
          )}
        </div>
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
          {filtered.map((product) => (
            <div
              key={product.id}
              className="group relative flex flex-col justify-between rounded-xl border border-border/60 bg-card p-5 shadow-sm transition-all duration-200 hover:border-border hover:shadow-md"
            >
              <div>
                <div className="flex items-start justify-between gap-2">
                  <div className="flex items-center gap-2.5">
                    <div className="flex h-9 w-9 items-center justify-center rounded-lg bg-blue-500/10 text-blue-500 font-mono font-bold text-xs ring-1 ring-blue-500/20">
                      {product.name.substring(0, 2).toUpperCase()}
                    </div>
                    <div>
                      <h4 className="text-sm font-semibold text-foreground group-hover:text-primary transition-colors">
                        {product.name}
                      </h4>
                      <span className="font-mono text-[11px] text-muted-foreground">
                        {product.slug || product.code}
                      </span>
                    </div>
                  </div>

                  <Badge
                    variant={product.status === 'ACTIVE' ? 'secondary' : 'outline'}
                    className="text-[10px] font-normal"
                  >
                    {product.status}
                  </Badge>
                </div>

                <p className="mt-3 text-xs text-muted-foreground line-clamp-2 leading-relaxed">
                  {product.description || 'No description provided for this product.'}
                </p>
              </div>

              <div className="mt-5 pt-3 border-t border-border/50 flex items-center justify-between">
                <div className="flex items-center gap-1.5 text-[11px] text-emerald-500">
                  <KeyRound className="h-3.5 w-3.5" />
                  <span className="font-mono text-[10px]">Ed25519 Active</span>
                </div>

                <div className="flex items-center gap-1">
                  <Button
                    variant="ghost"
                    size="sm"
                    onClick={() => setSelectedProduct(product)}
                    className="h-7 px-2 text-xs text-muted-foreground hover:text-foreground"
                  >
                    Details
                  </Button>

                  {canManage && product.status === 'ACTIVE' && (
                    <DropdownMenu>
                      <DropdownMenuTrigger asChild>
                        <Button variant="ghost" size="icon" className="h-7 w-7 text-muted-foreground">
                          <MoreVertical className="h-3.5 w-3.5" />
                        </Button>
                      </DropdownMenuTrigger>
                      <DropdownMenuContent align="end" className="text-xs">
                        <DropdownMenuItem
                          onClick={() => handleRotateKey(product.id)}
                          className="gap-2 cursor-pointer"
                        >
                          <RotateCw className="h-3.5 w-3.5 text-amber-500" />
                          <span>Rotate Signing Key</span>
                        </DropdownMenuItem>
                        <DropdownMenuItem
                          onClick={() => handleArchive(product.id)}
                          className="gap-2 text-destructive cursor-pointer"
                        >
                          <Archive className="h-3.5 w-3.5" />
                          <span>Archive Product</span>
                        </DropdownMenuItem>
                      </DropdownMenuContent>
                    </DropdownMenu>
                  )}
                </div>
              </div>
            </div>
          ))}
        </div>
      )}

      {/* Create Product Dialog */}
      <Dialog open={isCreateOpen} onOpenChange={setIsCreateOpen}>
        <DialogContent className="sm:max-w-md">
          <DialogHeader>
            <DialogTitle className="text-base font-semibold">Register Software Product</DialogTitle>
            <DialogDescription className="text-xs">
              Add an application to this workspace. An asymmetric Ed25519 signing key pair will be automatically generated.
            </DialogDescription>
          </DialogHeader>

          <form onSubmit={handleCreate} className="space-y-4 py-2">
            {formError && (
              <div className="rounded-lg border border-destructive/20 bg-destructive/10 p-2.5 text-xs text-destructive">
                {formError}
              </div>
            )}

            <div className="space-y-1.5">
              <label className="text-xs font-medium text-foreground">Product Name *</label>
              <Input
                placeholder="e.g. Acme Studio Pro"
                value={newProductName}
                onChange={(e) => handleNameChange(e.target.value)}
                required
                className="text-xs"
              />
            </div>

            <div className="space-y-1.5">
              <label className="text-xs font-medium text-foreground">Product Slug *</label>
              <Input
                placeholder="e.g. acme-studio-pro"
                value={newProductSlug}
                onChange={(e) => setNewProductSlug(e.target.value)}
                required
                className="text-xs font-mono"
              />
              <p className="text-[10px] text-muted-foreground">
                Unique identifier used in API routes and license validation endpoints.
              </p>
            </div>

            <div className="space-y-1.5">
              <label className="text-xs font-medium text-foreground">Description (Optional)</label>
              <Input
                placeholder="Brief description of the product and target platforms..."
                value={newProductDesc}
                onChange={(e) => setNewProductDesc(e.target.value)}
                className="text-xs"
              />
            </div>

            <DialogFooter className="pt-2">
              <Button
                type="button"
                variant="outline"
                size="sm"
                onClick={() => setIsCreateOpen(false)}
                disabled={isSubmitting}
              >
                Cancel
              </Button>
              <Button type="submit" size="sm" disabled={isSubmitting}>
                {isSubmitting ? 'Registering...' : 'Register Product'}
              </Button>
            </DialogFooter>
          </form>
        </DialogContent>
      </Dialog>

      {/* Product Details & Cryptographic Keys Modal */}
      {selectedProduct && (
        <Dialog open={!!selectedProduct} onOpenChange={(open) => !open && setSelectedProduct(null)}>
          <DialogContent className="sm:max-w-lg">
            <DialogHeader>
              <div className="flex items-center gap-2 mb-1">
                <Badge variant="outline" className="text-[10px] font-mono">
                  {selectedProduct.status}
                </Badge>
                <span className="text-xs text-muted-foreground font-mono">
                  ID: {selectedProduct.id}
                </span>
              </div>
              <DialogTitle className="text-lg font-bold">{selectedProduct.name}</DialogTitle>
              <DialogDescription className="text-xs">
                {selectedProduct.description || 'No description recorded.'}
              </DialogDescription>
            </DialogHeader>

            <div className="space-y-4 py-2">
              {/* Key Management Info Box */}
              <div className="rounded-xl border border-border/70 bg-muted/20 p-4 space-y-3">
                <div className="flex items-center justify-between">
                  <div className="flex items-center gap-2">
                    <KeyRound className="h-4 w-4 text-primary" />
                    <span className="text-xs font-semibold text-foreground">
                      Ed25519 Cryptographic Vault
                    </span>
                  </div>
                  <Badge variant="secondary" className="text-[10px] text-emerald-500 font-mono">
                    ACTIVE KEY
                  </Badge>
                </div>

                <p className="text-xs text-muted-foreground leading-relaxed">
                  Licenses issued under this product are signed with its active private key. The public key is bundled with your client SDK for offline signature verification.
                </p>

                <div className="rounded-lg bg-background/80 border border-border/60 p-2.5 font-mono text-[11px] text-muted-foreground break-all">
                  algorithm: "Ed25519" • status: "ACTIVE" • encryption: "AES-256-GCM"
                </div>

                {rotationSuccess && (
                  <div className="flex items-center gap-1.5 text-xs text-emerald-500 font-medium">
                    <CheckCircle2 className="h-4 w-4" />
                    Key rotation succeeded! New version active.
                  </div>
                )}
              </div>
            </div>

            <DialogFooter className="flex items-center justify-between sm:justify-between w-full">
              {canManage && selectedProduct.status === 'ACTIVE' && (
                <Button
                  variant="outline"
                  size="sm"
                  onClick={() => handleRotateKey(selectedProduct.id)}
                  disabled={isRotating}
                  className="gap-1.5 text-xs"
                >
                  <RotateCw className={`h-3.5 w-3.5 ${isRotating ? 'animate-spin' : ''}`} />
                  {isRotating ? 'Rotating Key...' : 'Rotate Ed25519 Key'}
                </Button>
              )}
              <Button size="sm" onClick={() => setSelectedProduct(null)}>
                Close
              </Button>
            </DialogFooter>
          </DialogContent>
        </Dialog>
      )}
    </div>
  )
}
