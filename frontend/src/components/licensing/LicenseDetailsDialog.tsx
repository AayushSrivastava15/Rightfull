import React, { useState, useEffect } from 'react'
import {
  KeyRound,
  Shield,
  Clock,
  Cpu,
  Layers,
  Users,
  Copy,
  Check,
  PauseCircle,
  PlayCircle,
  XCircle,
  RotateCcw,
  AlertTriangle,
  Laptop,
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
import { Badge } from '@/components/ui/badge'
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/components/ui/tabs'
import { apiClient } from '@/services/apiClient'
import type { License, LicenseActivation } from '@/types'

interface LicenseDetailsDialogProps {
  open: boolean
  onOpenChange: (open: boolean) => void
  license: License | null
  productName: string
  customerName?: string
  workspaceId: string
  onUpdate: () => void
}

export function LicenseDetailsDialog({
  open,
  onOpenChange,
  license,
  productName,
  customerName,
  workspaceId,
  onUpdate,
}: LicenseDetailsDialogProps) {
  const [activeTab, setActiveTab] = useState<'overview' | 'entitlements' | 'activations'>('overview')
  const [activations, setActivations] = useState<LicenseActivation[]>([])
  const [loadingActivations, setLoadingActivations] = useState(false)
  const [copiedKey, setCopiedKey] = useState(false)
  const [copiedId, setCopiedId] = useState(false)
  const [actionLoading, setActionLoading] = useState(false)
  const [actionError, setActionError] = useState<string | null>(null)

  // Fetch activations when modal opens or tab changes to activations
  useEffect(() => {
    if (!open || !license || !workspaceId) return
    setLoadingActivations(true)
    apiClient
      .getLicenseActivations(workspaceId, license.id, 0, 50)
      .then((res) => setActivations(res?.content || []))
      .catch((e) => console.warn('Failed to fetch activations:', e))
      .finally(() => setLoadingActivations(false))
  }, [open, license, workspaceId, activeTab])

  if (!license) return null

  const copyToClipboard = async (text: string, type: 'key' | 'id') => {
    try {
      await navigator.clipboard.writeText(text)
      if (type === 'key') {
        setCopiedKey(true)
        setTimeout(() => setCopiedKey(false), 2000)
      } else {
        setCopiedId(true)
        setTimeout(() => setCopiedId(false), 2000)
      }
    } catch (e) {
      console.warn('Copy failed:', e)
    }
  }

  const handleSuspend = async () => {
    const reason = window.prompt('Enter reason for license suspension:', 'Administrative review')
    if (reason === null) return
    setActionLoading(true)
    setActionError(null)
    try {
      await apiClient.suspendLicense(workspaceId, license.id, reason)
      onUpdate()
      onOpenChange(false)
    } catch (err: any) {
      setActionError(err.message || 'Failed to suspend license')
    } finally {
      setActionLoading(false)
    }
  }

  const handleResume = async () => {
    if (!window.confirm('Are you sure you want to resume this license?')) return
    setActionLoading(true)
    setActionError(null)
    try {
      await apiClient.resumeLicense(workspaceId, license.id)
      onUpdate()
      onOpenChange(false)
    } catch (err: any) {
      setActionError(err.message || 'Failed to resume license')
    } finally {
      setActionLoading(false)
    }
  }

  const handleRevoke = async () => {
    const reason = window.prompt(
      'WARNING: Revocation is permanent and cannot be undone. Enter revocation reason:',
      'License terms violation'
    )
    if (reason === null) return
    setActionLoading(true)
    setActionError(null)
    try {
      await apiClient.revokeLicense(workspaceId, license.id, reason)
      onUpdate()
      onOpenChange(false)
    } catch (err: any) {
      setActionError(err.message || 'Failed to revoke license')
    } finally {
      setActionLoading(false)
    }
  }

  const handleRenew = async () => {
    const daysStr = window.prompt('Enter number of days to extend this license by:', '365')
    if (daysStr === null) return
    const days = parseInt(daysStr, 10)
    if (isNaN(days) || days <= 0) {
      alert('Please enter a valid positive number of days.')
      return
    }
    setActionLoading(true)
    setActionError(null)
    try {
      await apiClient.renewLicense(workspaceId, license.id, days)
      onUpdate()
      onOpenChange(false)
    } catch (err: any) {
      setActionError(err.message || 'Failed to renew license')
    } finally {
      setActionLoading(false)
    }
  }

  const handleDeactivateDevice = async (activationId: string) => {
    if (!window.confirm('Are you sure you want to deactivate this client device?')) return
    setActionLoading(true)
    try {
      await apiClient.deactivateDevice(workspaceId, license.id, activationId)
      // Refresh activations
      const res = await apiClient.getLicenseActivations(workspaceId, license.id, 0, 50)
      setActivations(res?.content || [])
      onUpdate()
    } catch (err: any) {
      alert(err.message || 'Failed to deactivate device')
    } finally {
      setActionLoading(false)
    }
  }

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="sm:max-w-3xl max-h-[90vh] overflow-y-auto border-border bg-card shadow-2xl">
        <DialogHeader>
          <div className="flex items-center justify-between">
            <div className="flex items-center gap-2">
              <div className="flex h-9 w-9 items-center justify-center rounded-xl bg-primary/10 text-primary">
                <KeyRound className="h-5 w-5" />
              </div>
              <div>
                <DialogTitle className="text-base font-bold text-foreground flex items-center gap-2 font-mono">
                  {license.licenseKeyMasked}
                  <Badge
                    variant={
                      license.status === 'ACTIVE'
                        ? 'secondary'
                        : license.status === 'SUSPENDED'
                        ? 'destructive'
                        : license.status === 'REVOKED'
                        ? 'destructive'
                        : 'outline'
                    }
                    className="font-sans text-[10px] uppercase font-semibold"
                  >
                    {license.status}
                  </Badge>
                </DialogTitle>
                <DialogDescription className="text-xs text-muted-foreground">
                  Product: {productName} {customerName ? `· Customer: ${customerName}` : ''}
                </DialogDescription>
              </div>
            </div>

            <Button
              variant="outline"
              size="sm"
              onClick={() => copyToClipboard(license.licenseKeyPrefix, 'key')}
              className="h-8 gap-1.5 text-xs font-mono"
            >
              {copiedKey ? <Check className="h-3.5 w-3.5 text-emerald-500" /> : <Copy className="h-3.5 w-3.5" />}
              <span>{copiedKey ? 'Copied' : 'Copy Prefix'}</span>
            </Button>
          </div>
        </DialogHeader>

        {actionError && (
          <div className="rounded-xl border border-destructive/30 bg-destructive/10 p-3 text-xs text-destructive flex items-center gap-2">
            <AlertTriangle className="h-4 w-4 shrink-0" />
            <span>{actionError}</span>
          </div>
        )}

        <Tabs value={activeTab} onValueChange={(v: any) => setActiveTab(v)} className="w-full mt-2">
          <TabsList className="grid w-full grid-cols-3">
            <TabsTrigger value="overview" className="text-xs">Overview & Limits</TabsTrigger>
            <TabsTrigger value="entitlements" className="text-xs">Entitlements Snapshot</TabsTrigger>
            <TabsTrigger value="activations" className="text-xs">
              Device Nodes ({license.currentActivations}/{license.maxActivations})
            </TabsTrigger>
          </TabsList>

          {/* Tab 1: Overview */}
          <TabsContent value="overview" className="space-y-4 pt-4">
            <div className="grid grid-cols-2 sm:grid-cols-3 gap-3.5 rounded-xl border border-border/70 bg-muted/20 p-4">
              <div>
                <span className="text-[11px] text-muted-foreground">License ID</span>
                <div className="flex items-center gap-1.5 mt-0.5">
                  <span className="font-mono text-xs text-foreground truncate max-w-[140px]">{license.id}</span>
                  <button
                    type="button"
                    onClick={() => copyToClipboard(license.id, 'id')}
                    className="text-muted-foreground hover:text-foreground"
                  >
                    {copiedId ? <Check className="h-3 w-3 text-emerald-500" /> : <Copy className="h-3 w-3" />}
                  </button>
                </div>
              </div>

              <div>
                <span className="text-[11px] text-muted-foreground">Validity</span>
                <div className="text-xs font-semibold text-foreground mt-0.5">
                  {license.isPerpetual ? (
                    <span className="text-emerald-500 font-medium">Perpetual (No Expiry)</span>
                  ) : license.expiresAt ? (
                    `Expires ${new Date(license.expiresAt).toLocaleDateString()}`
                  ) : (
                    'Indefinite'
                  )}
                </div>
              </div>

              <div>
                <span className="text-[11px] text-muted-foreground">Device Activations</span>
                <div className="text-xs font-semibold text-foreground mt-0.5">
                  {license.currentActivations} / {license.maxActivations} registered
                </div>
              </div>

              <div>
                <span className="text-[11px] text-muted-foreground">Seat Limit</span>
                <div className="text-xs font-semibold text-foreground mt-0.5">{license.seatLimit} seat(s)</div>
              </div>

              <div>
                <span className="text-[11px] text-muted-foreground">Offline Grace</span>
                <div className="text-xs font-semibold text-foreground mt-0.5">
                  {Math.round(license.offlineGracePeriodSeconds / 3600)} hour(s)
                </div>
              </div>

              <div>
                <span className="text-[11px] text-muted-foreground">Revalidation</span>
                <div className="text-xs font-semibold text-foreground mt-0.5">
                  Every {Math.round(license.revalidationIntervalSeconds / 60)} min(s)
                </div>
              </div>

              <div>
                <span className="text-[11px] text-muted-foreground">Signing Key ID (kid)</span>
                <div className="font-mono text-[11px] text-foreground mt-0.5 truncate">{license.signingKeyId}</div>
              </div>

              <div>
                <span className="text-[11px] text-muted-foreground">Issued At</span>
                <div className="text-xs text-foreground mt-0.5">
                  {new Date(license.createdAt).toLocaleDateString()}
                </div>
              </div>

              <div>
                <span className="text-[11px] text-muted-foreground">External Reference</span>
                <div className="text-xs text-foreground mt-0.5">{license.externalReference || 'None'}</div>
              </div>
            </div>

            {/* Lifecycle Action Bar */}
            <div className="rounded-xl border border-border/70 bg-card p-4 space-y-2">
              <h4 className="text-xs font-semibold text-foreground">Lifecycle Operations</h4>
              <div className="flex flex-wrap items-center gap-2 pt-1">
                {license.status === 'ACTIVE' && (
                  <Button
                    variant="outline"
                    size="sm"
                    onClick={handleSuspend}
                    disabled={actionLoading}
                    className="text-xs gap-1.5 text-amber-600 dark:text-amber-400 border-amber-500/30 hover:bg-amber-500/10"
                  >
                    <PauseCircle className="h-3.5 w-3.5" />
                    Suspend License
                  </Button>
                )}

                {license.status === 'SUSPENDED' && (
                  <Button
                    variant="outline"
                    size="sm"
                    onClick={handleResume}
                    disabled={actionLoading}
                    className="text-xs gap-1.5 text-emerald-600 dark:text-emerald-400 border-emerald-500/30 hover:bg-emerald-500/10"
                  >
                    <PlayCircle className="h-3.5 w-3.5" />
                    Resume License
                  </Button>
                )}

                {license.status !== 'REVOKED' && (
                  <Button
                    variant="outline"
                    size="sm"
                    onClick={handleRenew}
                    disabled={actionLoading}
                    className="text-xs gap-1.5"
                  >
                    <RotateCcw className="h-3.5 w-3.5" />
                    Renew / Extend Expiry
                  </Button>
                )}

                {license.status !== 'REVOKED' && (
                  <Button
                    variant="destructive"
                    size="sm"
                    onClick={handleRevoke}
                    disabled={actionLoading}
                    className="text-xs gap-1.5 ml-auto"
                  >
                    <XCircle className="h-3.5 w-3.5" />
                    Revoke License
                  </Button>
                )}

                {license.status === 'REVOKED' && (
                  <div className="text-xs text-destructive font-medium flex items-center gap-1.5">
                    <XCircle className="h-4 w-4" />
                    This license is permanently revoked.
                  </div>
                )}
              </div>
            </div>
          </TabsContent>

          {/* Tab 2: Entitlements JSON */}
          <TabsContent value="entitlements" className="pt-4">
            <div className="space-y-2">
              <p className="text-xs text-muted-foreground">
                Cryptographically signed snapshot delivered to validating client SDKs.
              </p>
              <pre className="rounded-xl border border-border bg-muted/40 p-4 font-mono text-xs text-foreground overflow-x-auto max-h-[300px]">
                {JSON.stringify(license.entitlements, null, 2)}
              </pre>
            </div>
          </TabsContent>

          {/* Tab 3: Device Activations */}
          <TabsContent value="activations" className="pt-4 space-y-3">
            <div className="flex items-center justify-between text-xs text-muted-foreground">
              <span>Registered Node Hardware</span>
              <span>
                {license.currentActivations} of {license.maxActivations} slots active
              </span>
            </div>

            {loadingActivations ? (
              <div className="p-8 text-center text-xs text-muted-foreground">Loading activations...</div>
            ) : activations.length === 0 ? (
              <div className="p-8 text-center rounded-xl border border-dashed border-border bg-muted/10 text-xs text-muted-foreground">
                No client devices or hardware nodes have been activated for this license yet.
              </div>
            ) : (
              <div className="divide-y divide-border/60 rounded-xl border border-border/70 overflow-hidden">
                {activations.map((a) => (
                  <div key={a.id} className="p-3.5 flex items-center justify-between bg-card text-xs">
                    <div className="flex items-center gap-3">
                      <div className="flex h-8 w-8 items-center justify-center rounded-lg bg-primary/10 text-primary">
                        <Laptop className="h-4 w-4" />
                      </div>
                      <div>
                        <div className="font-semibold text-foreground flex items-center gap-2">
                          <span>{a.deviceName || 'Unnamed Node'}</span>
                          <Badge
                            variant={a.status === 'ACTIVE' ? 'secondary' : 'outline'}
                            className="text-[10px] font-normal"
                          >
                            {a.status}
                          </Badge>
                        </div>
                        <div className="font-mono text-[11px] text-muted-foreground">
                          Hash: {a.deviceIdentifierHash.substring(0, 16)}... &middot;{' '}
                          {a.devicePlatform || 'unknown platform'} &middot; v{a.appVersion || '1.0'}
                        </div>
                      </div>
                    </div>

                    <div className="flex items-center gap-3">
                      <span className="text-[11px] text-muted-foreground">
                        Last seen: {new Date(a.lastValidatedAt).toLocaleDateString()}
                      </span>
                      {a.status === 'ACTIVE' && (
                        <Button
                          variant="ghost"
                          size="sm"
                          onClick={() => handleDeactivateDevice(a.id)}
                          className="h-7 text-xs text-destructive hover:bg-destructive/10"
                        >
                          Deactivate
                        </Button>
                      )}
                    </div>
                  </div>
                ))}
              </div>
            )}
          </TabsContent>
        </Tabs>

        <DialogFooter className="mt-4">
          <Button variant="outline" onClick={() => onOpenChange(false)} className="text-xs">
            Close
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  )
}

