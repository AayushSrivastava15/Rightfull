import React, { useState, useEffect } from 'react'
import {
  Laptop,
  Cpu,
  Search,
  Check,
  Copy,
  AlertCircle,
  Clock,
  Shield,
  XCircle,
} from 'lucide-react'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Badge } from '@/components/ui/badge'
import { apiClient } from '@/services/apiClient'
import type { License, LicenseActivation, Product } from '@/types'

interface DeviceActivationsPanelProps {
  workspaceId: string
  products: Product[]
}

export function DeviceActivationsPanel({
  workspaceId,
  products,
}: DeviceActivationsPanelProps) {
  const [activations, setActivations] = useState<LicenseActivation[]>([])
  const [licenses, setLicenses] = useState<License[]>([])
  const [loading, setLoading] = useState(true)
  const [search, setSearch] = useState('')
  const [copiedId, setCopiedId] = useState<string | null>(null)

  const loadActivations = async () => {
    if (!workspaceId) return
    setLoading(true)
    try {
      // 1. Fetch licenses in workspace
      const licRes = await apiClient.getLicenses(workspaceId, undefined, undefined, undefined, undefined, 0, 100)
      const fetchedLicenses = licRes?.content || []
      setLicenses(fetchedLicenses)

      // 2. Fetch activations for each license
      const allActs: LicenseActivation[] = []
      for (const lic of fetchedLicenses) {
        if (lic.currentActivations > 0) {
          try {
            const actsRes = await apiClient.getLicenseActivations(workspaceId, lic.id, 0, 50)
            if (actsRes?.content) {
              allActs.push(...actsRes.content)
            }
          } catch (e) {
            console.warn(`Failed to fetch activations for license ${lic.id}:`, e)
          }
        }
      }

      setActivations(allActs)
    } catch (e) {
      console.warn('Failed to load device activations:', e)
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    loadActivations()
  }, [workspaceId])

  const handleCopy = async (text: string, id: string) => {
    try {
      await navigator.clipboard.writeText(text)
      setCopiedId(id)
      setTimeout(() => setCopiedId(null), 2000)
    } catch (e) {
      console.warn('Copy failed:', e)
    }
  }

  const handleDeactivate = async (act: LicenseActivation) => {
    if (!window.confirm(`Deactivate node '${act.deviceName || act.deviceIdentifierHash}'?`)) return
    try {
      await apiClient.deactivateDevice(workspaceId, act.licenseId, act.id)
      loadActivations()
    } catch (err: any) {
      alert(err.message || 'Failed to deactivate node')
    }
  }

  const getProductName = (prodId: string) => {
    const p = products.find((prod) => prod.id === prodId)
    return p ? p.name : 'Unknown Product'
  }

  const getLicenseMasked = (licId: string) => {
    const l = licenses.find((lic) => lic.id === licId)
    return l ? l.licenseKeyMasked : '••••••••'
  }

  const filtered = activations.filter((a) => {
    if (!search) return true
    const term = search.toLowerCase()
    return (
      (a.deviceName && a.deviceName.toLowerCase().includes(term)) ||
      a.deviceIdentifierHash.toLowerCase().includes(term) ||
      (a.devicePlatform && a.devicePlatform.toLowerCase().includes(term))
    )
  })

  return (
    <div className="space-y-6">
      {/* Top Header */}
      <div>
        <h2 className="text-xl font-bold tracking-tight text-foreground flex items-center gap-2">
          <Cpu className="h-5 w-5 text-primary" />
          Device & Node Activations
        </h2>
        <p className="text-xs text-muted-foreground mt-0.5">
          Monitor and manage authenticated client machines, server instances, and hardware nodes running your software.
        </p>
      </div>

      {/* Filter and Search Bar */}
      <div className="flex items-center justify-between gap-3 rounded-xl border border-border/70 bg-card p-3.5 shadow-sm">
        <div className="relative flex-1 max-w-sm">
          <Search className="absolute left-2.5 top-2.5 h-3.5 w-3.5 text-muted-foreground" />
          <Input
            type="text"
            placeholder="Search by device label, hash, platform..."
            value={search}
            onChange={(e) => setSearch(e.target.value)}
            className="pl-8 h-8 text-xs bg-background"
          />
        </div>

        <div className="text-xs text-muted-foreground">
          Showing <span className="font-semibold text-foreground">{filtered.length}</span> active device(s)
        </div>
      </div>

      {/* Activations Table */}
      <div className="rounded-xl border border-border/70 bg-card overflow-hidden shadow-sm">
        {loading ? (
          <div className="p-12 text-center text-xs text-muted-foreground">Loading active device nodes...</div>
        ) : filtered.length === 0 ? (
          <div className="p-12 text-center space-y-3">
            <div className="mx-auto flex h-12 w-12 items-center justify-center rounded-2xl bg-primary/10 text-primary">
              <Laptop className="h-6 w-6" />
            </div>
            <div>
              <h3 className="text-sm font-semibold text-foreground">No Active Devices</h3>
              <p className="text-xs text-muted-foreground max-w-sm mx-auto mt-0.5">
                {search
                  ? 'No activations match your search query.'
                  : 'Client applications will appear here once they authenticate using POST /api/v1/licenses/activate.'}
              </p>
            </div>
          </div>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-xs text-left">
              <thead className="bg-muted/40 text-[11px] uppercase tracking-wider text-muted-foreground border-b border-border/60">
                <tr>
                  <th className="px-4 py-3 font-semibold">Device Node</th>
                  <th className="px-4 py-3 font-semibold">Product</th>
                  <th className="px-4 py-3 font-semibold">Bound License</th>
                  <th className="px-4 py-3 font-semibold">Platform & Version</th>
                  <th className="px-4 py-3 font-semibold">Status</th>
                  <th className="px-4 py-3 font-semibold">Last Heartbeat</th>
                  <th className="px-4 py-3 font-semibold text-right">Actions</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-border/60">
                {filtered.map((act) => {
                  return (
                    <tr key={act.id} className="hover:bg-muted/20 transition-colors">
                      {/* Device Node & Hash */}
                      <td className="px-4 py-3 font-medium text-foreground">
                        <div className="flex items-center gap-2">
                          <Laptop className="h-4 w-4 text-muted-foreground" />
                          <span>{act.deviceName || 'Unnamed Node'}</span>
                        </div>
                        <div className="font-mono text-[10px] text-muted-foreground flex items-center gap-1.5 mt-0.5">
                          <span>{act.deviceIdentifierHash.substring(0, 16)}...</span>
                          <button
                            type="button"
                            onClick={() => handleCopy(act.deviceIdentifierHash, act.id)}
                            className="text-muted-foreground hover:text-foreground"
                          >
                            {copiedId === act.id ? (
                              <Check className="h-3 w-3 text-emerald-500" />
                            ) : (
                              <Copy className="h-3 w-3" />
                            )}
                          </button>
                        </div>
                      </td>

                      {/* Product */}
                      <td className="px-4 py-3 text-foreground font-medium">
                        {getProductName(act.productId)}
                      </td>

                      {/* Bound License */}
                      <td className="px-4 py-3 font-mono text-muted-foreground">
                        {getLicenseMasked(act.licenseId)}
                      </td>

                      {/* Platform & Version */}
                      <td className="px-4 py-3 text-muted-foreground">
                        <div className="text-foreground">{act.devicePlatform || 'unknown'}</div>
                        <div className="text-[10px]">v{act.appVersion || '1.0'}</div>
                      </td>

                      {/* Status */}
                      <td className="px-4 py-3">
                        <Badge
                          variant={act.status === 'ACTIVE' ? 'secondary' : 'outline'}
                          className="text-[10px] font-semibold"
                        >
                          {act.status}
                        </Badge>
                      </td>

                      {/* Last Heartbeat */}
                      <td className="px-4 py-3 text-muted-foreground">
                        {new Date(act.lastValidatedAt).toLocaleDateString()}
                      </td>

                      {/* Actions */}
                      <td className="px-4 py-3 text-right">
                        {act.status === 'ACTIVE' && (
                          <Button
                            variant="ghost"
                            size="sm"
                            onClick={() => handleDeactivate(act)}
                            className="h-7 text-xs text-destructive hover:bg-destructive/10"
                          >
                            Deactivate
                          </Button>
                        )}
                      </td>
                    </tr>
                  )
                })}
              </tbody>
            </table>
          </div>
        )}
      </div>
    </div>
  )
}

