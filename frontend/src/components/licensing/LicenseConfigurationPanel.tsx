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
  DialogTitle,
  DialogDescription,
  DialogFooter,
} from '@/components/ui/dialog'
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
      description: description.trim() || undefined,
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
      <div className="flex items-start gap-4 rounded-xl border border-zinc-800 bg-zinc-900/50 p-4 text-zinc-300">
        <div className="mt-0.5 rounded-lg bg-blue-500/10 p-2 text-blue-400 ring-1 ring-blue-500/20">
          <Info className="h-5 w-5" />
        </div>
        <div className="space-y-1 text-sm">
          <p className="font-semibold text-white">
            Licensing Infrastructure Presets
          </p>
          <p className="text-xs text-zinc-400">
            Rightful delivers cryptographically signed permissions and restrictions defined by you.
            These configurations are reusable presets to streamline license issuance. They are not
            rigid billing plans, pricing models, or forced feature flags.
          </p>
        </div>
      </div>

      {/* Header & Product Selector */}
      <div className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
        <div className="flex items-center gap-3">
          <Label htmlFor="product-select" className="text-xs font-semibold uppercase tracking-wider text-zinc-400">
            Product:
          </Label>
          <select
            id="product-select"
            value={selectedProductId}
            onChange={(e) => setSelectedProductId(e.target.value)}
            className="rounded-lg border border-zinc-800 bg-zinc-900 px-3 py-1.5 text-sm font-medium text-white focus:outline-none focus:ring-1 focus:ring-blue-500"
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
            className="gap-2 bg-blue-600 hover:bg-blue-500 text-white"
            disabled={!selectedProductId || activeProduct?.status === 'ARCHIVED'}
          >
            <Plus className="h-4 w-4" />
            New Configuration Preset
          </Button>
        )}
      </div>

      {/* Configurations List */}
      {isLoading ? (
        <div className="flex justify-center p-12 text-zinc-400">
          <div className="h-6 w-6 animate-spin rounded-full border-2 border-blue-500 border-t-transparent" />
        </div>
      ) : error ? (
        <div className="rounded-xl border border-red-500/20 bg-red-500/10 p-6 text-sm text-red-400">
          Failed to load license configurations. Please verify your workspace permissions.
        </div>
      ) : configurations.length === 0 ? (
        <Card className="border-dashed border-zinc-800 bg-zinc-900/20 text-center py-12">
          <CardContent className="space-y-3">
            <Sliders className="mx-auto h-10 w-10 text-zinc-600" />
            <div className="space-y-1">
              <h3 className="font-semibold text-white">No License Configurations Yet</h3>
              <p className="text-xs text-zinc-500 max-w-sm mx-auto">
                Create reusable restriction templates (durations, max activations, grace periods, and custom JSON entitlements) for {activeProduct?.name || 'this product'}.
              </p>
            </div>
            {canManage && (
              <Button onClick={openCreate} size="sm" variant="outline" className="border-zinc-700 text-zinc-300">
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
              className={`border-zinc-800 bg-zinc-900/60 shadow-md ${
                cfg.status === 'ARCHIVED' ? 'opacity-60' : ''
              }`}
            >
              <CardHeader className="flex flex-row items-start justify-between pb-2">
                <div className="space-y-1">
                  <div className="flex items-center gap-2">
                    <CardTitle className="text-base font-bold text-white">
                      {cfg.name}
                    </CardTitle>
                    <Badge variant="outline" className="font-mono text-[10px] border-zinc-700 text-zinc-400">
                      v{cfg.version}
                    </Badge>
                  </div>
                  {cfg.description && (
                    <CardDescription className="text-xs text-zinc-400 line-clamp-1">
                      {cfg.description}
                    </CardDescription>
                  )}
                </div>
                <Badge
                  variant={cfg.status === 'ACTIVE' ? 'default' : 'secondary'}
                  className={
                    cfg.status === 'ACTIVE'
                      ? 'bg-emerald-500/10 text-emerald-400 border border-emerald-500/20 text-[10px]'
                      : 'bg-zinc-800 text-zinc-400 text-[10px]'
                  }
                >
                  {cfg.status}
                </Badge>
              </CardHeader>

              <CardContent className="space-y-4 pt-2">
                {/* Parameter Pills */}
                <div className="grid grid-cols-2 gap-2 text-xs">
                  <div className="flex items-center gap-2 rounded-lg border border-zinc-800/80 bg-zinc-950/40 p-2">
                    <Clock className="h-4 w-4 text-blue-400" />
                    <div>
                      <span className="text-[10px] text-zinc-500 block">Validity</span>
                      <span className="font-medium text-zinc-200">
                        {cfg.durationDays === null || cfg.durationDays === undefined
                          ? 'Perpetual (Lifetime)'
                          : `${cfg.durationDays} Days`}
                      </span>
                    </div>
                  </div>

                  <div className="flex items-center gap-2 rounded-lg border border-zinc-800/80 bg-zinc-950/40 p-2">
                    <Laptop className="h-4 w-4 text-indigo-400" />
                    <div>
                      <span className="text-[10px] text-zinc-500 block">Activations</span>
                      <span className="font-medium text-zinc-200">
                        {cfg.maximumActivations} Node{cfg.maximumActivations > 1 ? 's' : ''}
                      </span>
                    </div>
                  </div>

                  <div className="flex items-center gap-2 rounded-lg border border-zinc-800/80 bg-zinc-950/40 p-2">
                    <Users className="h-4 w-4 text-amber-400" />
                    <div>
                      <span className="text-[10px] text-zinc-500 block">Seats</span>
                      <span className="font-medium text-zinc-200">
                        {cfg.seatLimit} Seat{cfg.seatLimit > 1 ? 's' : ''}
                      </span>
                    </div>
                  </div>

                  <div className="flex items-center gap-2 rounded-lg border border-zinc-800/80 bg-zinc-950/40 p-2">
                    <Layers className="h-4 w-4 text-emerald-400" />
                    <div>
                      <span className="text-[10px] text-zinc-500 block">Grace / Heartbeat</span>
                      <span className="font-medium text-zinc-200 font-mono text-[11px]">
                        {Math.round(cfg.offlineGracePeriodSeconds / 3600)}h / {Math.round(cfg.revalidationIntervalSeconds / 60)}m
                      </span>
                    </div>
                  </div>
                </div>

                {/* Entitlements Summary & Actions */}
                <div className="flex items-center justify-between border-t border-zinc-800/80 pt-3">
                  <Button
                    variant="ghost"
                    size="sm"
                    className="h-8 gap-1.5 text-xs text-zinc-400 hover:text-white"
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
                        className="h-8 w-8 p-0 text-zinc-400 hover:text-white"
                        onClick={() => openEdit(cfg)}
                        title="Edit Configuration"
                      >
                        <Edit2 className="h-3.5 w-3.5" />
                      </Button>
                      <Button
                        variant="ghost"
                        size="sm"
                        className="h-8 w-8 p-0 text-zinc-400 hover:text-red-400"
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
        <DialogContent className="max-w-2xl border-zinc-800 bg-zinc-950 text-zinc-100 max-h-[90vh] overflow-y-auto">
          <DialogHeader>
            <DialogTitle className="text-lg font-bold text-white flex items-center gap-2">
              <Sliders className="h-5 w-5 text-blue-500" />
              {editingConfig ? 'Edit License Configuration' : 'Create License Configuration Preset'}
            </DialogTitle>
            <DialogDescription className="text-xs text-zinc-400">
              {editingConfig
                ? 'Editing parameters increments the configuration version. Previously issued licenses remain immutable.'
                : 'Define reusable default restrictions, node activations, seat limits, and structured permissions.'}
            </DialogDescription>
          </DialogHeader>

          {formError && (
            <div className="rounded-lg border border-red-500/30 bg-red-500/10 p-3 text-xs text-red-400 flex items-center gap-2">
              <AlertTriangle className="h-4 w-4 flex-shrink-0" />
              <span>{formError}</span>
            </div>
          )}

          <div className="space-y-4 py-2 text-sm">
            <div className="grid gap-2">
              <Label htmlFor="cfg-name">Configuration Name *</Label>
              <Input
                id="cfg-name"
                value={name}
                onChange={(e) => setName(e.target.value)}
                placeholder="e.g. Standard Developer, Team Pro, On-Premise Core"
                className="border-zinc-800 bg-zinc-900 text-white"
              />
            </div>

            <div className="grid gap-2">
              <Label htmlFor="cfg-desc">Description (Optional)</Label>
              <Input
                id="cfg-desc"
                value={description}
                onChange={(e) => setDescription(e.target.value)}
                placeholder="Internal notes or reference for this licensing preset"
                className="border-zinc-800 bg-zinc-900 text-white"
              />
            </div>

            {/* Validity Duration */}
            <div className="rounded-xl border border-zinc-800/80 bg-zinc-900/30 p-4 space-y-3">
              <div className="flex items-center justify-between">
                <Label className="font-semibold text-white">License Duration & Expiry</Label>
                <div className="flex items-center gap-2">
                  <input
                    type="checkbox"
                    id="perpetual-toggle"
                    checked={isPerpetual}
                    onChange={(e) => setIsPerpetual(e.target.checked)}
                    className="rounded border-zinc-700 bg-zinc-800 text-blue-600 focus:ring-0"
                  />
                  <Label htmlFor="perpetual-toggle" className="text-xs text-zinc-300 cursor-pointer">
                    Perpetual / Lifetime (No Expiry)
                  </Label>
                </div>
              </div>

              {!isPerpetual ? (
                <div className="grid grid-cols-2 gap-3">
                  <div>
                    <Label htmlFor="duration-days" className="text-xs text-zinc-400">Duration in Days</Label>
                    <Input
                      id="duration-days"
                      type="number"
                      min={1}
                      max={36500}
                      value={durationDays}
                      onChange={(e) => setDurationDays(Number(e.target.value))}
                      className="border-zinc-800 bg-zinc-900 text-white mt-1"
                    />
                  </div>
                  <div className="flex items-end">
                    <span className="text-xs text-zinc-500 pb-2">
                      = ~{(durationDays / 365).toFixed(1)} Year(s) / {Math.round(durationDays / 30)} Month(s)
                    </span>
                  </div>
                </div>
              ) : (
                <p className="text-xs text-emerald-400">
                  License will be issued without an expiration timestamp.
                </p>
              )}
            </div>

            {/* Activations & Seats */}
            <div className="grid grid-cols-2 gap-4">
              <div className="space-y-1.5">
                <Label htmlFor="max-act" className="text-xs">Max Node Activations</Label>
                <Input
                  id="max-act"
                  type="number"
                  min={1}
                  value={maxActivations}
                  onChange={(e) => setMaxActivations(Number(e.target.value))}
                  className="border-zinc-800 bg-zinc-900 text-white"
                />
                <span className="text-[10px] text-zinc-500">Hardware machines allowed</span>
              </div>
              <div className="space-y-1.5">
                <Label htmlFor="seat-lim" className="text-xs">Seat Limit</Label>
                <Input
                  id="seat-lim"
                  type="number"
                  min={1}
                  value={seatLimit}
                  onChange={(e) => setSeatLimit(Number(e.target.value))}
                  className="border-zinc-800 bg-zinc-900 text-white"
                />
                <span className="text-[10px] text-zinc-500">User accounts allowed</span>
              </div>
            </div>

            {/* Grace Period & Revalidation */}
            <div className="grid grid-cols-2 gap-4">
              <div className="space-y-1.5">
                <Label htmlFor="grace-period" className="text-xs">Offline Grace Period (Seconds)</Label>
                <Input
                  id="grace-period"
                  type="number"
                  min={300}
                  max={2592000}
                  value={gracePeriodSec}
                  onChange={(e) => setGracePeriodSec(Number(e.target.value))}
                  className="border-zinc-800 bg-zinc-900 text-white font-mono text-xs"
                />
                <span className="text-[10px] text-zinc-500">
                  Default 25,200s (7 hours). Max 30 days.
                </span>
              </div>

              <div className="space-y-1.5">
                <Label htmlFor="revalidation" className="text-xs">Revalidation Interval (Seconds)</Label>
                <Input
                  id="revalidation"
                  type="number"
                  min={60}
                  max={86400}
                  value={revalidationSec}
                  onChange={(e) => setRevalidationSec(Number(e.target.value))}
                  className="border-zinc-800 bg-zinc-900 text-white font-mono text-xs"
                />
                <span className="text-[10px] text-zinc-500">
                  Default 300s (5 minutes). Max 24 hours.
                </span>
              </div>
            </div>

            {/* Entitlements JSON Editor */}
            <div className="space-y-2">
              <div className="flex items-center justify-between">
                <Label className="text-xs font-semibold text-white flex items-center gap-1.5">
                  <Code2 className="h-4 w-4 text-blue-400" />
                  Structured Entitlements (JSON)
                </Label>
                <Button
                  type="button"
                  variant="ghost"
                  size="sm"
                  className="h-7 text-xs text-blue-400 hover:text-blue-300 gap-1"
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
                className="font-mono text-xs border-zinc-800 bg-zinc-900 text-zinc-200"
              />
              <p className="text-[10px] text-zinc-500">
                Supports typed boolean flags, numeric quotas, string configurations, and nested sections up to 3 levels deep.
              </p>
            </div>
          </div>

          <DialogFooter className="pt-2">
            <Button
              variant="outline"
              size="sm"
              onClick={() => {
                setIsCreateOpen(false)
                setEditingConfig(null)
              }}
              className="border-zinc-800 text-zinc-400"
            >
              Cancel
            </Button>
            <Button
              size="sm"
              onClick={handleSave}
              disabled={createMutation.isPending || updateMutation.isPending}
              className="bg-blue-600 hover:bg-blue-500 text-white"
            >
              {createMutation.isPending || updateMutation.isPending ? 'Saving...' : 'Save Configuration'}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      {/* Inspect Entitlements Dialog */}
      <Dialog
        open={!!viewingEntitlements}
        onOpenChange={(open) => !open && setViewingEntitlements(null)}
      >
        <DialogContent className="border-zinc-800 bg-zinc-950 text-zinc-100 max-w-lg">
          <DialogHeader>
            <DialogTitle className="text-base font-bold text-white flex items-center gap-2">
              <Code2 className="h-4 w-4 text-blue-400" />
              Entitlements Payload
            </DialogTitle>
            <DialogDescription className="text-xs text-zinc-400">
              Developer-defined restrictions, limits, and feature permissions delivered in signed license tokens.
            </DialogDescription>
          </DialogHeader>
          <pre className="p-4 rounded-lg bg-zinc-900 text-emerald-400 font-mono text-xs overflow-x-auto max-h-80 border border-zinc-800">
            {JSON.stringify(viewingEntitlements, null, 2)}
          </pre>
          <DialogFooter>
            <Button
              size="sm"
              variant="outline"
              onClick={() => setViewingEntitlements(null)}
              className="border-zinc-800 text-zinc-300"
            >
              Close
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </div>
  )
}

