import { useState } from 'react'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import {
  Sliders,
  Plus,
  Layers,
  Clock,
  Laptop,
  Users,
  Archive,
  Edit2,
  Info,
  CheckCircle2,
  AlertTriangle,
  Code2,
  Sparkles,
} from 'lucide-react'
import { apiClient } from '@/services/apiClient'
import type {
  LicenseConfiguration,
  Product,
  CreateConfigurationInput,
  UpdateConfigurationInput,
} from '@/types'
import { Button } from '@/components/ui/button'
import { Badge } from '@/components/ui/badge'
import {
  Card,
  CardHeader,
  CardTitle,
  CardDescription,
  CardContent,
} from '@/components/ui/card'
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogBody,
  DialogTitle,
  DialogDescription,
  DialogFooter,
} from '@/components/ui/dialog'
import { cn } from '@/lib/utils'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Textarea } from '@/components/ui/textarea'

interface LicenseConfigurationPanelProps {
  workspaceId: string
  products: Product[]
  canManage: boolean
}

const SAMPLE_ENTITLEMENTS = {
  features: {
    pdf_export: true,
    team_collaboration: true,
    advanced_analytics: false,
  },
  limits: {
    max_projects: 50,
    max_users: 5,
    api_requests_per_day: 10000,
  },
  modules: {
    reporting: true,
    automation: false,
  },
}

export function LicenseConfigurationPanel({
  workspaceId,
  products,
  canManage,
}: LicenseConfigurationPanelProps) {
  const queryClient = useQueryClient()
  const [selectedProductId, setSelectedProductId] = useState<string>(
    products[0]?.id || ''
  )
  const [isCreateOpen, setIsCreateOpen] = useState(false)
  const [editingConfig, setEditingConfig] = useState<LicenseConfiguration | null>(null)
  const [viewingEntitlements, setViewingEntitlements] = useState<Record<string, any> | null>(null)

  // Form State
  const [name, setName] = useState('')
  const [description, setDescription] = useState('')
  const [isPerpetual, setIsPerpetual] = useState(false)
  const [durationDays, setDurationDays] = useState(365)
  const [maxActivations, setMaxActivations] = useState(1)
  const [seatLimit, setSeatLimit] = useState(1)
  const [gracePeriodSec, setGracePeriodSec] = useState(25200) // 7 hours
  const [revalidationSec, setRevalidationSec] = useState(300) // 5 minutes
  const [entitlementsText, setEntitlementsText] = useState('{}')
  const [formError, setFormError] = useState<string | null>(null)

  const activeProduct = products.find((p) => p.id === selectedProductId)

  const {
    data: configurations = [],
    isLoading,
    error,
  } = useQuery({
    queryKey: ['configurations', workspaceId, selectedProductId],
    queryFn: () => apiClient.getConfigurations(workspaceId, selectedProductId),
    enabled: !!selectedProductId && !!workspaceId,
  })

  const createMutation = useMutation({
    mutationFn: (data: CreateConfigurationInput) =>
      apiClient.createConfiguration(workspaceId, selectedProductId, data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['configurations', workspaceId, selectedProductId] })
      setIsCreateOpen(false)
      resetForm()
    },
    onError: (err: any) => {
      setFormError(err.message || 'Failed to create license configuration')
    },
  })

  const updateMutation = useMutation({
    mutationFn: ({ id, data }: { id: string; data: UpdateConfigurationInput }) =>
      apiClient.updateConfiguration(workspaceId, selectedProductId, id, data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['configurations', workspaceId, selectedProductId] })
      setEditingConfig(null)
      resetForm()
    },
    onError: (err: any) => {
      setFormError(err.message || 'Failed to update license configuration')
    },
  })

  const archiveMutation = useMutation({
    mutationFn: (id: string) =>
      apiClient.archiveConfiguration(workspaceId, selectedProductId, id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['configurations', workspaceId, selectedProductId] })
    },
  })

  function resetForm() {
    setName('')
    setDescription('')
    setIsPerpetual(false)
    setDurationDays(365)
    setMaxActivations(1)
    setSeatLimit(1)
    setGracePeriodSec(25200)
    setRevalidationSec(300)
    setEntitlementsText('{}')
    setFormError(null)
  }

  function openCreate() {
    resetForm()
    setIsCreateOpen(true)
  }

  function openEdit(cfg: LicenseConfiguration) {
    resetForm()
    setEditingConfig(cfg)
    setName(cfg.name)
    setDescription(cfg.description || '')
    setIsPerpetual(cfg.durationDays === null || cfg.durationDays === undefined)
    setDurationDays(cfg.durationDays || 365)
    setMaxActivations(cfg.maximumActivations)
    setSeatLimit(cfg.seatLimit)
    setGracePeriodSec(cfg.offlineGracePeriodSeconds)
    setRevalidationSec(cfg.revalidationIntervalSeconds)
    setEntitlementsText(JSON.stringify(cfg.entitlements, null, 2))
  }

  function handleSave() {
    setFormError(null)
    if (!name.trim()) {
      setFormError('Configuration name is required')
      return
    }

    let parsedEntitlements = {}
    try {
      parsedEntitlements = JSON.parse(entitlementsText || '{}')
      if (typeof parsedEntitlements !== 'object' || Array.isArray(parsedEntitlements)) {
        throw new Error('Entitlements must be a JSON object')
      }
    } catch (e: any) {
      setFormError(`Invalid Entitlements JSON: ${e.message}`)
      return
    }

    const payload: CreateConfigurationInput = {
      name: name.trim(),
      ...(description.trim() ? { description: description.trim() } : {}),
      durationDays: isPerpetual ? null : Number(durationDays),
      maximumActivations: Number(maxActivations),
      seatLimit: Number(seatLimit),
      offlineGracePeriodSeconds: Number(gracePeriodSec),
      revalidationIntervalSeconds: Number(revalidationSec),
      entitlements: parsedEntitlements,
    }

    if (editingConfig) {
      updateMutation.mutate({
        id: editingConfig.id,
        data: payload as UpdateConfigurationInput,
      })
    } else {
      createMutation.mutate(payload)
    }
  }

  return (
    <div className="space-y-6">
      {/* Principle & Architectural Clarification Callout */}
      <div className="flex items-start gap-4 rounded-xl border border-primary/20 bg-primary/5 p-4 text-foreground">
        <div className="mt-0.5 rounded-lg bg-primary/10 p-2 text-primary ring-1 ring-primary/20">
          <Info className="h-5 w-5" />
        </div>
        <div className="space-y-1 text-sm">
          <p className="font-semibold text-foreground">
            Licensing Infrastructure Presets
          </p>
          <p className="text-xs text-muted-foreground">
            Rightful delivers cryptographically signed permissions and restrictions defined by you.
            These configurations are reusable presets to streamline license issuance. They are not
            rigid billing plans, pricing models, or forced feature flags.
          </p>
        </div>
      </div>

      {/* Header & Product Selector */}
      <div className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
        <div className="flex items-center gap-3">
          <Label htmlFor="product-select" className="text-xs font-semibold uppercase tracking-wider text-muted-foreground">
            Product:
          </Label>
          <select
            id="product-select"
            value={selectedProductId}
            onChange={(e) => setSelectedProductId(e.target.value)}
            className="rounded-lg border border-border/80 bg-background px-3 py-1.5 text-sm font-medium text-foreground focus:outline-none focus:ring-2 focus:ring-primary"
          >
            {products.map((p) => (
              <option key={p.id} value={p.id}>
                {p.name} ({p.code})
              </option>
            ))}
          </select>
        </div>

        {canManage && (
          <Button
            onClick={openCreate}
            size="sm"
            className="gap-2"
            disabled={!selectedProductId || activeProduct?.status === 'ARCHIVED'}
          >
            <Plus className="h-4 w-4" />
            New Configuration Preset
          </Button>
        )}
      </div>

      {/* Configurations List */}
      {isLoading ? (
        <div className="flex justify-center p-12 text-muted-foreground">
          <div className="h-6 w-6 animate-spin rounded-full border-2 border-primary border-t-transparent" />
        </div>
      ) : error ? (
        <div className="rounded-xl border border-destructive/20 bg-destructive/10 p-6 text-sm text-destructive">
          Failed to load license configurations. Please verify your workspace permissions.
        </div>
      ) : configurations.length === 0 ? (
        <Card className="border-dashed border-border/80 bg-muted/20 text-center py-12">
          <CardContent className="space-y-3">
            <Sliders className="mx-auto h-10 w-10 text-muted-foreground" />
            <div className="space-y-1">
              <h3 className="font-semibold text-foreground">No License Configurations Yet</h3>
              <p className="text-xs text-muted-foreground max-w-sm mx-auto">
                Create reusable restriction templates (durations, max activations, grace periods, and custom JSON entitlements) for {activeProduct?.name || 'this product'}.
              </p>
            </div>
            {canManage && (
              <Button onClick={openCreate} size="sm" variant="outline">
                <Plus className="h-4 w-4 mr-1.5" />
                Add First Configuration
              </Button>
            )}
          </CardContent>
        </Card>
      ) : (
        <div className="grid gap-4 md:grid-cols-2">
          {configurations.map((cfg) => (
            <Card
              key={cfg.id}
              className={cn(
                "border-border/70 bg-card shadow-xs hover:border-border transition-all duration-200",
                cfg.status === 'ARCHIVED' && 'opacity-60'
              )}
            >
              <CardHeader className="flex flex-row items-start justify-between pb-2">
                <div className="space-y-1">
                  <div className="flex items-center gap-2">
                    <CardTitle className="text-base font-bold text-foreground">
                      {cfg.name}
                    </CardTitle>
                    <Badge variant="outline" className="font-mono text-[10px] border-border/70 text-muted-foreground">
                      v{cfg.version}
                    </Badge>
                  </div>
                  {cfg.description && (
                    <CardDescription className="text-xs text-muted-foreground line-clamp-1">
                      {cfg.description}
                    </CardDescription>
                  )}
                </div>
                <Badge
                  variant={cfg.status === 'ACTIVE' ? 'default' : 'secondary'}
                  className={
                    cfg.status === 'ACTIVE'
                      ? 'bg-emerald-500/10 text-emerald-500 border border-emerald-500/20 text-[10px]'
                      : 'bg-muted text-muted-foreground text-[10px]'
                  }
                >
                  {cfg.status}
                </Badge>
              </CardHeader>

              <CardContent className="space-y-4 pt-2">
                {/* Parameter Pills */}
                <div className="grid grid-cols-2 gap-2 text-xs">
                  <div className="flex items-center gap-2 rounded-lg border border-border/60 bg-muted/30 p-2.5">
                    <Clock className="h-4 w-4 text-primary" />
                    <div>
                      <span className="text-[10px] text-muted-foreground block">Validity</span>
                      <span className="font-medium text-foreground">
                        {cfg.durationDays === null || cfg.durationDays === undefined
                          ? 'Perpetual (Lifetime)'
                          : `${cfg.durationDays} Days`}
                      </span>
                    </div>
                  </div>

                  <div className="flex items-center gap-2 rounded-lg border border-border/60 bg-muted/30 p-2.5">
                    <Laptop className="h-4 w-4 text-primary" />
                    <div>
                      <span className="text-[10px] text-muted-foreground block">Activations</span>
                      <span className="font-medium text-foreground">
                        {cfg.maximumActivations} Node{cfg.maximumActivations > 1 ? 's' : ''}
                      </span>
                    </div>
                  </div>

                  <div className="flex items-center gap-2 rounded-lg border border-border/60 bg-muted/30 p-2.5">
                    <Users className="h-4 w-4 text-primary" />
                    <div>
                      <span className="text-[10px] text-muted-foreground block">Seats</span>
                      <span className="font-medium text-foreground">
                        {cfg.seatLimit} Seat{cfg.seatLimit > 1 ? 's' : ''}
                      </span>
                    </div>
                  </div>

                  <div className="flex items-center gap-2 rounded-lg border border-border/60 bg-muted/30 p-2.5">
                    <Layers className="h-4 w-4 text-primary" />
                    <div>
                      <span className="text-[10px] text-muted-foreground block">Grace / Heartbeat</span>
                      <span className="font-medium text-foreground font-mono text-[11px]">
                        {Math.round(cfg.offlineGracePeriodSeconds / 3600)}h / {Math.round(cfg.revalidationIntervalSeconds / 60)}m
                      </span>
                    </div>
                  </div>
                </div>

                {/* Entitlements Summary & Actions */}
                <div className="flex items-center justify-between border-t border-border/60 pt-3">
                  <Button
                    variant="ghost"
                    size="sm"
                    className="h-8 gap-1.5 text-xs text-muted-foreground hover:text-foreground"
                    onClick={() => setViewingEntitlements(cfg.entitlements)}
                  >
                    <Code2 className="h-3.5 w-3.5" />
                    Inspect Entitlements ({Object.keys(cfg.entitlements || {}).length} keys)
                  </Button>

                  {canManage && cfg.status === 'ACTIVE' && (
                    <div className="flex items-center gap-1.5">
                      <Button
                        variant="ghost"
                        size="sm"
                        className="h-8 w-8 p-0 text-muted-foreground hover:text-foreground"
                        onClick={() => openEdit(cfg)}
                        title="Edit Configuration"
                      >
                        <Edit2 className="h-3.5 w-3.5" />
                      </Button>
                      <Button
                        variant="ghost"
                        size="sm"
                        className="h-8 w-8 p-0 text-muted-foreground hover:text-destructive"
                        onClick={() => archiveMutation.mutate(cfg.id)}
                        disabled={archiveMutation.isPending}
                        title="Archive Configuration"
                      >
                        <Archive className="h-3.5 w-3.5" />
                      </Button>
                    </div>
                  )}
                </div>
              </CardContent>
            </Card>
          ))}
        </div>
      )}

      {/* Create / Edit Dialog */}
      <Dialog
        open={isCreateOpen || !!editingConfig}
        onOpenChange={(open) => {
          if (!open) {
            setIsCreateOpen(false)
            setEditingConfig(null)
          }
        }}
      >
        <DialogContent className="max-w-2xl">
          <DialogHeader>
            <DialogTitle className="text-lg font-bold text-foreground flex items-center gap-2">
              <Sliders className="h-5 w-5 text-primary" />
              {editingConfig ? 'Edit License Configuration' : 'Create License Configuration Preset'}
            </DialogTitle>
            <DialogDescription className="text-xs text-muted-foreground">
              {editingConfig
                ? 'Editing parameters increments the configuration version. Previously issued licenses remain immutable.'
                : 'Define reusable default restrictions, node activations, seat limits, and structured permissions.'}
            </DialogDescription>
          </DialogHeader>

          <DialogBody className="space-y-4 py-4 text-sm">
            {formError && (
              <div className="rounded-lg border border-destructive/20 bg-destructive/10 p-3 text-xs text-destructive flex items-center gap-2">
                <AlertTriangle className="h-4 w-4 shrink-0" />
                <span>{formError}</span>
              </div>
            )}

            <div className="grid gap-2">
              <Label htmlFor="cfg-name" className="text-foreground">Configuration Name *</Label>
              <Input
                id="cfg-name"
                value={name}
                onChange={(e) => setName(e.target.value)}
                placeholder="e.g. Standard Developer, Team Pro, On-Premise Core"
              />
            </div>

            <div className="grid gap-2">
              <Label htmlFor="cfg-desc" className="text-foreground">Description (Optional)</Label>
              <Input
                id="cfg-desc"
                value={description}
                onChange={(e) => setDescription(e.target.value)}
                placeholder="Internal notes or reference for this licensing preset"
              />
            </div>

            {/* Validity Duration */}
            <div className="rounded-xl border border-border/70 bg-muted/20 p-4 space-y-3">
              <div className="flex items-center justify-between">
                <Label className="font-semibold text-foreground">License Duration & Expiry</Label>
                <div className="flex items-center gap-2">
                  <input
                    type="checkbox"
                    id="perpetual-toggle"
                    checked={isPerpetual}
                    onChange={(e) => setIsPerpetual(e.target.checked)}
                    className="h-4 w-4 rounded border-border text-primary focus:ring-primary cursor-pointer"
                  />
                  <Label htmlFor="perpetual-toggle" className="text-xs text-muted-foreground cursor-pointer">
                    Perpetual / Lifetime (No Expiry)
                  </Label>
                </div>
              </div>

              {!isPerpetual ? (
                <div className="grid grid-cols-2 gap-3">
                  <div>
                    <Label htmlFor="duration-days" className="text-xs text-muted-foreground">Duration in Days</Label>
                    <Input
                      id="duration-days"
                      type="number"
                      min={1}
                      max={36500}
                      value={durationDays}
                      onChange={(e) => setDurationDays(Number(e.target.value))}
                      className="mt-1"
                    />
                  </div>
                  <div className="flex items-end">
                    <span className="text-xs text-muted-foreground pb-2">
                      = ~{(durationDays / 365).toFixed(1)} Year(s) / {Math.round(durationDays / 30)} Month(s)
                    </span>
                  </div>
                </div>
              ) : (
                <p className="text-xs text-emerald-500 font-medium">
                  License will be issued without an expiration timestamp.
                </p>
              )}
            </div>

            {/* Activations & Seats */}
            <div className="grid grid-cols-2 gap-4">
              <div className="space-y-1.5">
                <Label htmlFor="max-act" className="text-xs text-foreground">Max Node Activations</Label>
                <Input
                  id="max-act"
                  type="number"
                  min={1}
                  value={maxActivations}
                  onChange={(e) => setMaxActivations(Number(e.target.value))}
                />
                <span className="text-[10px] text-muted-foreground">Hardware machines allowed</span>
              </div>
              <div className="space-y-1.5">
                <Label htmlFor="seat-lim" className="text-xs text-foreground">Seat Limit</Label>
                <Input
                  id="seat-lim"
                  type="number"
                  min={1}
                  value={seatLimit}
                  onChange={(e) => setSeatLimit(Number(e.target.value))}
                />
                <span className="text-[10px] text-muted-foreground">User accounts allowed</span>
              </div>
            </div>

            {/* Grace Period & Revalidation */}
            <div className="grid grid-cols-2 gap-4">
              <div className="space-y-1.5">
                <Label htmlFor="grace-period" className="text-xs text-foreground">Offline Grace Period (Seconds)</Label>
                <Input
                  id="grace-period"
                  type="number"
                  min={300}
                  max={2592000}
                  value={gracePeriodSec}
                  onChange={(e) => setGracePeriodSec(Number(e.target.value))}
                  className="font-mono text-xs"
                />
                <span className="text-[10px] text-muted-foreground">
                  Default 25,200s (7 hours). Max 30 days.
                </span>
              </div>

              <div className="space-y-1.5">
                <Label htmlFor="revalidation" className="text-xs text-foreground">Revalidation Interval (Seconds)</Label>
                <Input
                  id="revalidation"
                  type="number"
                  min={60}
                  max={86400}
                  value={revalidationSec}
                  onChange={(e) => setRevalidationSec(Number(e.target.value))}
                  className="font-mono text-xs"
                />
                <span className="text-[10px] text-muted-foreground">
                  Default 300s (5 minutes). Max 24 hours.
                </span>
              </div>
            </div>

            {/* Entitlements JSON Editor */}
            <div className="space-y-2">
              <div className="flex items-center justify-between">
                <Label className="text-xs font-semibold text-foreground flex items-center gap-1.5">
                  <Code2 className="h-4 w-4 text-primary" />
                  Structured Entitlements (JSON)
                </Label>
                <Button
                  type="button"
                  variant="ghost"
                  size="sm"
                  className="h-7 text-xs text-primary hover:text-primary/80 gap-1"
                  onClick={() =>
                    setEntitlementsText(JSON.stringify(SAMPLE_ENTITLEMENTS, null, 2))
                  }
                >
                  <Sparkles className="h-3 w-3" />
                  Load Structured Preset
                </Button>
              </div>
              <Textarea
                rows={6}
                value={entitlementsText}
                onChange={(e) => setEntitlementsText(e.target.value)}
                placeholder='{\n  "features": { "pdf_export": true },\n  "limits": { "max_projects": 50 }\n}'
                className="font-mono text-xs"
              />
              <p className="text-[10px] text-muted-foreground">
                Supports typed boolean flags, numeric quotas, string configurations, and nested sections up to 3 levels deep.
              </p>
            </div>
          </DialogBody>

          <DialogFooter>
            <Button
              variant="outline"
              size="sm"
              onClick={() => {
                setIsCreateOpen(false)
                setEditingConfig(null)
              }}
            >
              Cancel
            </Button>
            <Button
              size="sm"
              onClick={handleSave}
              loading={createMutation.isPending || updateMutation.isPending}
            >
              Save Configuration
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      {/* Inspect Entitlements Dialog */}
      <Dialog
        open={!!viewingEntitlements}
        onOpenChange={(open) => !open && setViewingEntitlements(null)}
      >
        <DialogContent className="max-w-lg">
          <DialogHeader>
            <DialogTitle className="text-base font-bold text-foreground flex items-center gap-2">
              <Code2 className="h-4 w-4 text-primary" />
              Entitlements Payload
            </DialogTitle>
            <DialogDescription className="text-xs text-muted-foreground">
              Developer-defined restrictions, limits, and feature permissions delivered in signed license tokens.
            </DialogDescription>
          </DialogHeader>
          <DialogBody>
            <pre className="p-4 rounded-xl bg-muted/40 text-foreground font-mono text-xs overflow-x-auto max-h-80 border border-border/60 custom-scrollbar">
              {JSON.stringify(viewingEntitlements, null, 2)}
            </pre>
          </DialogBody>
          <DialogFooter>
            <Button
              size="sm"
              variant="outline"
              onClick={() => setViewingEntitlements(null)}
            >
              Close
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </div>
  )
}

