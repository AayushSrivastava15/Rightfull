import React, { useState, useEffect } from 'react'
import {
  KeyRound,
  Shield,
  Layers,
  Users,
  Clock,
  Cpu,
  Sparkles,
  AlertCircle,
  FileCode2,
} from 'lucide-react'
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from '@/components/ui/dialog'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Badge } from '@/components/ui/badge'
import { apiClient } from '@/services/apiClient'
import type {
  Product,
  Customer,
  LicenseConfiguration,
  IssueLicenseInput,
  IssueLicenseResponse,
} from '@/types'

interface IssueLicenseDialogProps {
  open: boolean
  onOpenChange: (open: boolean) => void
  workspaceId: string
  products: Product[]
  defaultProductId?: string
  onSuccess: (response: IssueLicenseResponse) => void
}

export function IssueLicenseDialog({
  open,
  onOpenChange,
  workspaceId,
  products,
  defaultProductId,
  onSuccess,
}: IssueLicenseDialogProps) {
  const [selectedProductId, setSelectedProductId] = useState<string>(
    defaultProductId || products[0]?.id || ''
  )
  const [customers, setCustomers] = useState<Customer[]>([])
  const [selectedCustomerId, setSelectedCustomerId] = useState<string>('')

  const [usePreset, setUsePreset] = useState<boolean>(true)
  const [presets, setPresets] = useState<LicenseConfiguration[]>([])
  const [selectedPresetId, setSelectedPresetId] = useState<string>('')

  // Direct configuration state
  const [isPerpetual, setIsPerpetual] = useState<boolean>(false)
  const [durationDays, setDurationDays] = useState<number>(365)
  const [maxActivations, setMaxActivations] = useState<number>(1)
  const [seatLimit, setSeatLimit] = useState<number>(1)
  const [gracePeriodHours, setGracePeriodHours] = useState<number>(7)
  const [revalIntervalMinutes, setRevalIntervalMinutes] = useState<number>(5)
  const [entitlementsJson, setEntitlementsJson] = useState<string>(
    JSON.stringify(
      {
        features: {
          pro_export: true,
          cloud_sync: true,
        },
        quotas: {
          max_projects: 50,
          api_rate_limit: 1000,
        },
      },
      null,
      2
    )
  )
  const [jsonError, setJsonError] = useState<string | null>(null)
  const [externalReference, setExternalReference] = useState<string>('')
  const [loading, setLoading] = useState(false)
  const [formError, setFormError] = useState<string | null>(null)

  useEffect(() => {
    if (defaultProductId) {
      setSelectedProductId(defaultProductId)
    } else if (products.length > 0 && !selectedProductId) {
      setSelectedProductId(products[0].id)
    }
  }, [defaultProductId, products])

  // Fetch customers in workspace
  useEffect(() => {
    if (!open || !workspaceId) return
    apiClient
      .getCustomers(workspaceId, '', 'ACTIVE', 0, 100)
      .then((res) => setCustomers(res?.content || []))
      .catch((e) => console.warn('Failed to load customers for issuance:', e))
  }, [open, workspaceId])

  // Fetch presets for selected product
  useEffect(() => {
    if (!open || !workspaceId || !selectedProductId) return
    apiClient
      .getConfigurations(workspaceId, selectedProductId)
      .then((configs) => {
        const activeConfigs = (configs || []).filter((c) => c.status === 'ACTIVE')
        setPresets(activeConfigs)
        if (activeConfigs.length > 0) {
          setSelectedPresetId(activeConfigs[0].id)
        } else {
          setSelectedPresetId('')
          setUsePreset(false)
        }
      })
      .catch((e) => {
        console.warn('Failed to load presets:', e)
        setPresets([])
      })
  }, [open, workspaceId, selectedProductId])

  const validateJson = (text: string) => {
    setEntitlementsJson(text)
    try {
      JSON.parse(text)
      setJsonError(null)
    } catch (e: any) {
      setJsonError(e.message)
    }
  }

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    setFormError(null)

    if (!selectedProductId) {
      setFormError('Please select a target product')
      return
    }

    let parsedEntitlements: Record<string, any> = {}
    if (!usePreset && entitlementsJson.trim()) {
      try {
        parsedEntitlements = JSON.parse(entitlementsJson)
      } catch (err: any) {
        setFormError('Invalid entitlements JSON: ' + err.message)
        return
      }
    }

    const payload: IssueLicenseInput = {
      customerId: selectedCustomerId || undefined,
      externalReference: externalReference.trim() || undefined,
    }

    if (usePreset && selectedPresetId) {
      payload.planId = selectedPresetId
    } else {
      payload.durationDays = isPerpetual ? undefined : durationDays
      payload.maximumActivations = maxActivations
      payload.seatLimit = seatLimit
      payload.offlineGracePeriodSeconds = gracePeriodHours * 3600
      payload.revalidationIntervalSeconds = revalIntervalMinutes * 60
      payload.entitlements = parsedEntitlements
    }

    setLoading(true)
    try {
      const response = await apiClient.issueLicense(workspaceId, selectedProductId, payload)
      onSuccess(response)
      onOpenChange(false)
    } catch (err: any) {
      setFormError(err.message || 'Failed to issue software license')
    } finally {
      setLoading(false)
    }
  }

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="sm:max-w-2xl max-h-[90vh] overflow-y-auto border-border bg-card shadow-2xl">
        <DialogHeader>
          <div className="flex items-center gap-2 mb-1">
            <div className="flex h-9 w-9 items-center justify-center rounded-xl bg-primary/10 text-primary">
              <KeyRound className="h-5 w-5" />
            </div>
            <div>
              <DialogTitle className="text-lg font-bold text-foreground">
                Issue New Software License
              </DialogTitle>
              <DialogDescription className="text-xs text-muted-foreground">
                Mint a tamper-proof license key with immutable entitlement snapshot and Ed25519 digital signature
              </DialogDescription>
            </div>
          </div>
        </DialogHeader>

        {formError && (
          <div className="rounded-xl border border-destructive/30 bg-destructive/10 p-3 text-xs text-destructive flex items-center gap-2">
            <AlertCircle className="h-4 w-4 shrink-0" />
            <span>{formError}</span>
          </div>
        )}

        <form onSubmit={handleSubmit} className="space-y-6 py-2">
          {/* Section 1: Product & Customer */}
          <div className="space-y-3 rounded-xl border border-border/70 bg-muted/20 p-4">
            <h4 className="text-xs font-semibold text-foreground flex items-center gap-2">
              <Layers className="h-4 w-4 text-primary" />
              1. Target Product & Customer
            </h4>

            <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
              <div className="space-y-1.5">
                <Label className="text-xs font-medium text-foreground">Product *</Label>
                <select
                  value={selectedProductId}
                  onChange={(e) => setSelectedProductId(e.target.value)}
                  className="w-full h-9 rounded-md border border-input bg-background px-3 text-xs text-foreground focus-visible:outline-none focus-visible:ring-1 focus-visible:ring-ring"
                  required
                >
                  {products.map((p) => (
                    <option key={p.id} value={p.id}>
                      {p.name} ({p.slug})
                    </option>
                  ))}
                </select>
              </div>

              <div className="space-y-1.5">
                <Label className="text-xs font-medium text-foreground">Customer (Optional)</Label>
                <select
                  value={selectedCustomerId}
                  onChange={(e) => setSelectedCustomerId(e.target.value)}
                  className="w-full h-9 rounded-md border border-input bg-background px-3 text-xs text-foreground focus-visible:outline-none focus-visible:ring-1 focus-visible:ring-ring"
                >
                  <option value="">No Customer Assigned (Standalone)</option>
                  {customers.map((c) => (
                    <option key={c.id} value={c.id}>
                      {c.name} {c.company ? `(${c.company})` : `(${c.email})`}
                    </option>
                  ))}
                </select>
              </div>
            </div>
          </div>

          {/* Section 2: Reusable Preset vs Direct Configuration */}
          <div className="space-y-4 rounded-xl border border-border/70 bg-muted/20 p-4">
            <div className="flex items-center justify-between">
              <h4 className="text-xs font-semibold text-foreground flex items-center gap-2">
                <Shield className="h-4 w-4 text-primary" />
                2. Configuration & Entitlements
              </h4>

              <div className="flex items-center gap-1.5 bg-background p-1 rounded-lg border border-border text-xs">
                <button
                  type="button"
                  onClick={() => setUsePreset(true)}
                  disabled={presets.length === 0}
                  className={`px-2.5 py-1 rounded text-xs font-medium transition-colors ${
                    usePreset
                      ? 'bg-primary text-primary-foreground shadow-sm'
                      : 'text-muted-foreground hover:text-foreground'
                  } ${presets.length === 0 ? 'opacity-40 cursor-not-allowed' : ''}`}
                >
                  Configuration Preset ({presets.length})
                </button>
                <button
                  type="button"
                  onClick={() => setUsePreset(false)}
                  className={`px-2.5 py-1 rounded text-xs font-medium transition-colors ${
                    !usePreset
                      ? 'bg-primary text-primary-foreground shadow-sm'
                      : 'text-muted-foreground hover:text-foreground'
                  }`}
                >
                  Direct Custom
                </button>
              </div>
            </div>

            {usePreset ? (
              <div className="space-y-3">
                <div className="space-y-1.5">
                  <Label className="text-xs font-medium text-foreground">Select Preset</Label>
                  <select
                    value={selectedPresetId}
                    onChange={(e) => setSelectedPresetId(e.target.value)}
                    className="w-full h-9 rounded-md border border-input bg-background px-3 text-xs text-foreground focus-visible:outline-none focus-visible:ring-1 focus-visible:ring-ring"
                  >
                    {presets.map((cfg) => (
                      <option key={cfg.id} value={cfg.id}>
                        {cfg.name} (v{cfg.version}) &mdash;{' '}
                        {cfg.durationDays ? `${cfg.durationDays} days` : 'Perpetual'},{' '}
                        {cfg.maximumActivations} nodes, {cfg.seatLimit} seats
                      </option>
                    ))}
                  </select>
                </div>

                <p className="text-[11px] text-muted-foreground">
                  The issued license captures an immutable snapshot of this preset. Subsequent modifications to the preset will not alter this license.
                </p>
              </div>
            ) : (
              <div className="space-y-4">
                {/* Duration */}
                <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                  <div className="space-y-1.5">
                    <div className="flex items-center justify-between">
                      <Label className="text-xs font-medium text-foreground">Duration (Days)</Label>
                      <label className="flex items-center gap-1.5 text-[11px] text-muted-foreground cursor-pointer">
                        <input
                          type="checkbox"
                          checked={isPerpetual}
                          onChange={(e) => setIsPerpetual(e.target.checked)}
                          className="rounded border-input text-primary focus:ring-primary h-3.5 w-3.5"
                        />
                        <span>Perpetual (No Expiry)</span>
                      </label>
                    </div>
                    <Input
                      type="number"
                      min={1}
                      disabled={isPerpetual}
                      value={durationDays}
                      onChange={(e) => setDurationDays(Number(e.target.value))}
                      className="h-9 text-xs"
                    />
                  </div>

                  <div className="space-y-1.5">
                    <Label className="text-xs font-medium text-foreground">Max Activations (Devices/Nodes)</Label>
                    <Input
                      type="number"
                      min={1}
                      value={maxActivations}
                      onChange={(e) => setMaxActivations(Number(e.target.value))}
                      className="h-9 text-xs"
                      required
                    />
                  </div>
                </div>

                {/* Seats and Grace Period */}
                <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
                  <div className="space-y-1.5">
                    <Label className="text-xs font-medium text-foreground">Seat Limit</Label>
                    <Input
                      type="number"
                      min={1}
                      value={seatLimit}
                      onChange={(e) => setSeatLimit(Number(e.target.value))}
                      className="h-9 text-xs"
                      required
                    />
                  </div>

                  <div className="space-y-1.5">
                    <Label className="text-xs font-medium text-foreground">Offline Grace (Hours)</Label>
                    <Input
                      type="number"
                      min={1}
                      max={720}
                      value={gracePeriodHours}
                      onChange={(e) => setGracePeriodHours(Number(e.target.value))}
                      className="h-9 text-xs"
                      required
                    />
                  </div>

                  <div className="space-y-1.5">
                    <Label className="text-xs font-medium text-foreground">Revalidation (Minutes)</Label>
                    <Input
                      type="number"
                      min={1}
                      max={1440}
                      value={revalIntervalMinutes}
                      onChange={(e) => setRevalIntervalMinutes(Number(e.target.value))}
                      className="h-9 text-xs"
                      required
                    />
                  </div>
                </div>

                {/* Entitlements JSON */}
                <div className="space-y-1.5">
                  <div className="flex items-center justify-between">
                    <Label className="text-xs font-medium text-foreground">Custom Entitlements (JSON)</Label>
                    {jsonError && <span className="text-[10px] text-destructive">Invalid JSON syntax</span>}
                  </div>
                  <textarea
                    rows={5}
                    value={entitlementsJson}
                    onChange={(e) => validateJson(e.target.value)}
                    className="w-full rounded-md border border-input bg-background p-2.5 font-mono text-[11px] text-foreground focus-visible:outline-none focus-visible:ring-1 focus-visible:ring-ring"
                  />
                </div>
              </div>
            )}
          </div>

          {/* Section 3: Reference & Order info */}
          <div className="space-y-1.5">
            <Label className="text-xs font-medium text-foreground">External Reference / Order ID (Optional)</Label>
            <Input
              type="text"
              placeholder="e.g. INV-2026-9042 or stripe_sub_xxxx"
              value={externalReference}
              onChange={(e) => setExternalReference(e.target.value)}
              className="h-9 text-xs"
            />
          </div>

          <DialogFooter className="gap-2 sm:gap-0 mt-4">
            <Button
              type="button"
              variant="outline"
              onClick={() => onOpenChange(false)}
              className="text-xs"
              disabled={loading}
            >
              Cancel
            </Button>
            <Button type="submit" disabled={loading || (!!jsonError && !usePreset)} className="gap-2 text-xs">
              <KeyRound className="h-3.5 w-3.5" />
              {loading ? 'Minting License...' : 'Issue License Key'}
            </Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  )
}

