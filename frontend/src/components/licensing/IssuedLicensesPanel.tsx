import React, { useState, useEffect } from 'react'
import {
  KeyRound,
  Shield,
  Search,
  Filter,
  Plus,
  Copy,
  Check,
  MoreVertical,
  Laptop,
  Users,
  Clock,
  Sparkles,
  AlertCircle,
  PauseCircle,
  PlayCircle,
  XCircle,
  RotateCcw,
} from 'lucide-react'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Badge } from '@/components/ui/badge'
import { apiClient } from '@/services/apiClient'
import { IssueLicenseDialog } from '@/components/licensing/IssueLicenseDialog'
import { OneTimeKeyModal } from '@/components/licensing/OneTimeKeyModal'
import { LicenseDetailsDialog } from '@/components/licensing/LicenseDetailsDialog'
import type {
  Product,
  Customer,
  License,
  LicenseSummary,
  IssueLicenseResponse,
  LicenseStatus,
} from '@/types'

interface IssuedLicensesPanelProps {
  workspaceId: string
  products: Product[]
}

export function IssuedLicensesPanel({ workspaceId, products }: IssuedLicensesPanelProps) {
  const [licenses, setLicenses] = useState<License[]>([])
  const [summary, setSummary] = useState<LicenseSummary | null>(null)
  const [customers, setCustomers] = useState<Customer[]>([])
  const [loading, setLoading] = useState(true)

  // Filter states
  const [search, setSearch] = useState('')
  const [selectedProductId, setSelectedProductId] = useState<string>('')
  const [selectedCustomerId, setSelectedCustomerId] = useState<string>('')
  const [statusFilter, setStatusFilter] = useState<LicenseStatus | 'ALL'>('ALL')

  // Modals state
  const [issueOpen, setIssueOpen] = useState(false)
  const [oneTimeOpen, setOneTimeOpen] = useState(false)
  const [newlyIssuedResponse, setNewlyIssuedResponse] = useState<IssueLicenseResponse | null>(null)

  const [detailsOpen, setDetailsOpen] = useState(false)
  const [selectedLicense, setSelectedLicense] = useState<License | null>(null)

  const [copiedKeyId, setCopiedKeyId] = useState<string | null>(null)

  const loadData = async () => {
    if (!workspaceId) return
    setLoading(true)
    try {
      const [licensesRes, summaryRes, customersRes] = await Promise.all([
        apiClient.getLicenses(
          workspaceId,
          selectedProductId || undefined,
          selectedCustomerId || undefined,
          statusFilter !== 'ALL' ? statusFilter : undefined,
          search || undefined,
          0,
          50
        ),
        apiClient.getLicenseSummary(workspaceId).catch(() => null),
        apiClient.getCustomers(workspaceId, '', 'ACTIVE', 0, 100).catch(() => ({ content: [] })),
      ])

      setLicenses(licensesRes?.content || [])
      setSummary(summaryRes)
      setCustomers(customersRes?.content || [])
    } catch (e) {
      console.warn('Failed to load licenses:', e)
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    loadData()
  }, [workspaceId, selectedProductId, selectedCustomerId, statusFilter, search])

  const handleCopyMasked = async (text: string, id: string) => {
    try {
      await navigator.clipboard.writeText(text)
      setCopiedKeyId(id)
      setTimeout(() => setCopiedKeyId(null), 2000)
    } catch (e) {
      console.warn('Copy failed:', e)
    }
  }

  const handleIssueSuccess = (response: IssueLicenseResponse) => {
    setNewlyIssuedResponse(response)
    setOneTimeOpen(true)
    loadData()
  }

  const openDetails = (lic: License) => {
    setSelectedLicense(lic)
    setDetailsOpen(true)
  }

  const getProductName = (id: string) => {
    const p = products.find((prod) => prod.id === id)
    return p ? p.name : 'Unknown Product'
  }

  const getCustomerName = (id?: string) => {
    if (!id) return null
    const c = customers.find((cust) => cust.id === id)
    return c ? c.name : null
  }

  return (
    <div className="space-y-6">
      {/* Top Header & Issue CTA */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div>
          <h2 className="text-xl font-bold tracking-tight text-foreground flex items-center gap-2">
            <KeyRound className="h-5 w-5 text-primary" />
            Issued Licenses
          </h2>
          <p className="text-xs text-muted-foreground mt-0.5">
            Manage software product licenses, activation caps, offline entitlements, and lifecycle states.
          </p>
        </div>

        <Button
          onClick={() => setIssueOpen(true)}
          className="gap-2 text-xs font-semibold shadow-sm h-9"
          disabled={products.length === 0}
        >
          <Plus className="h-4 w-4" />
          Issue New License
        </Button>
      </div>

      {/* Metrics Summary Strip */}
      <div className="grid grid-cols-2 sm:grid-cols-5 gap-3.5">
        <div className="rounded-xl border border-border/80 bg-card p-3.5 shadow-sm">
          <div className="text-[11px] text-muted-foreground font-medium">Total Licenses</div>
          <div className="text-xl font-bold text-foreground mt-0.5">
            {summary?.totalLicenses ?? licenses.length}
          </div>
        </div>

        <div className="rounded-xl border border-border/80 bg-card p-3.5 shadow-sm">
          <div className="text-[11px] text-muted-foreground font-medium flex items-center gap-1.5">
            <span className="h-2 w-2 rounded-full bg-emerald-500" />
            Active
          </div>
          <div className="text-xl font-bold text-emerald-500 mt-0.5">
            {summary?.activeLicenses ?? licenses.filter((l) => l.status === 'ACTIVE').length}
          </div>
        </div>

        <div className="rounded-xl border border-border/80 bg-card p-3.5 shadow-sm">
          <div className="text-[11px] text-muted-foreground font-medium flex items-center gap-1.5">
            <span className="h-2 w-2 rounded-full bg-amber-500" />
            Expired
          </div>
          <div className="text-xl font-bold text-amber-500 mt-0.5">
            {summary?.expiredLicenses ?? licenses.filter((l) => l.status === 'EXPIRED').length}
          </div>
        </div>

        <div className="rounded-xl border border-border/80 bg-card p-3.5 shadow-sm">
          <div className="text-[11px] text-muted-foreground font-medium flex items-center gap-1.5">
            <span className="h-2 w-2 rounded-full bg-orange-500" />
            Suspended
          </div>
          <div className="text-xl font-bold text-orange-500 mt-0.5">
            {summary?.suspendedLicenses ?? licenses.filter((l) => l.status === 'SUSPENDED').length}
          </div>
        </div>

        <div className="rounded-xl border border-border/80 bg-card p-3.5 shadow-sm col-span-2 sm:col-span-1">
          <div className="text-[11px] text-muted-foreground font-medium flex items-center gap-1.5">
            <span className="h-2 w-2 rounded-full bg-destructive" />
            Revoked
          </div>
          <div className="text-xl font-bold text-destructive mt-0.5">
            {summary?.revokedLicenses ?? licenses.filter((l) => l.status === 'REVOKED').length}
          </div>
        </div>
      </div>

      {/* Filter and Search Bar */}
      <div className="flex flex-col md:flex-row md:items-center justify-between gap-3 rounded-xl border border-border/70 bg-card p-3.5 shadow-sm">
        <div className="flex flex-1 items-center gap-2">
          <div className="relative flex-1 max-w-sm">
            <Search className="absolute left-2.5 top-2.5 h-3.5 w-3.5 text-muted-foreground" />
            <Input
              type="text"
              placeholder="Search by key prefix or reference..."
              value={search}
              onChange={(e) => setSearch(e.target.value)}
              className="pl-8 h-8 text-xs bg-background"
            />
          </div>

          {/* Product Filter Dropdown */}
          <select
            value={selectedProductId}
            onChange={(e) => setSelectedProductId(e.target.value)}
            className="h-8 rounded-md border border-input bg-background px-2.5 text-xs text-foreground focus-visible:outline-none focus-visible:ring-1 focus-visible:ring-ring"
          >
            <option value="">All Products</option>
            {products.map((p) => (
              <option key={p.id} value={p.id}>
                {p.name}
              </option>
            ))}
          </select>

          {/* Customer Filter Dropdown */}
          <select
            value={selectedCustomerId}
            onChange={(e) => setSelectedCustomerId(e.target.value)}
            className="h-8 rounded-md border border-input bg-background px-2.5 text-xs text-foreground focus-visible:outline-none focus-visible:ring-1 focus-visible:ring-ring"
          >
            <option value="">All Customers</option>
            {customers.map((c) => (
              <option key={c.id} value={c.id}>
                {c.name}
              </option>
            ))}
          </select>
        </div>

        {/* Status Pills */}
        <div className="flex items-center gap-1 bg-muted/40 p-1 rounded-lg border border-border/60 text-xs">
          {(['ALL', 'ACTIVE', 'SUSPENDED', 'EXPIRED', 'REVOKED'] as const).map((st) => (
            <button
              key={st}
              onClick={() => setStatusFilter(st)}
              className={`px-2.5 py-1 rounded text-xs font-medium transition-colors ${
                statusFilter === st
                  ? 'bg-primary text-primary-foreground shadow-sm'
                  : 'text-muted-foreground hover:text-foreground'
              }`}
            >
              {st}
            </button>
          ))}
        </div>
      </div>

      {/* Main Licenses Data Table */}
      <div className="rounded-xl border border-border/70 bg-card overflow-hidden shadow-sm">
        {loading ? (
          <div className="p-12 text-center text-xs text-muted-foreground">Loading licenses...</div>
        ) : licenses.length === 0 ? (
          <div className="p-12 text-center space-y-3">
            <div className="mx-auto flex h-12 w-12 items-center justify-center rounded-2xl bg-primary/10 text-primary">
              <KeyRound className="h-6 w-6" />
            </div>
            <div>
              <h3 className="text-sm font-semibold text-foreground">No Licenses Found</h3>
              <p className="text-xs text-muted-foreground max-w-sm mx-auto mt-0.5">
                {search || selectedProductId || statusFilter !== 'ALL'
                  ? 'No licenses matched the active filter criteria.'
                  : 'You have not issued any software product licenses yet.'}
              </p>
            </div>
            {products.length > 0 && (
              <Button
                variant="outline"
                size="sm"
                onClick={() => setIssueOpen(true)}
                className="gap-2 text-xs mt-2"
              >
                <Plus className="h-3.5 w-3.5" />
                Issue Your First License
              </Button>
            )}
          </div>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-xs text-left">
              <thead className="bg-muted/40 text-[11px] uppercase tracking-wider text-muted-foreground border-b border-border/60">
                <tr>
                  <th className="px-4 py-3 font-semibold">License Key</th>
                  <th className="px-4 py-3 font-semibold">Product</th>
                  <th className="px-4 py-3 font-semibold">Customer</th>
                  <th className="px-4 py-3 font-semibold">Status</th>
                  <th className="px-4 py-3 font-semibold">Activations</th>
                  <th className="px-4 py-3 font-semibold">Expiry</th>
                  <th className="px-4 py-3 font-semibold text-right">Actions</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-border/60">
                {licenses.map((lic) => {
                  const custName = getCustomerName(lic.customerId)
                  const prodName = getProductName(lic.productId)

                  return (
                    <tr key={lic.id} className="hover:bg-muted/20 transition-colors">
                      {/* Key & Copy */}
                      <td className="px-4 py-3 font-mono font-medium text-foreground">
                        <div className="flex items-center gap-1.5">
                          <span>{lic.licenseKeyMasked}</span>
                          <button
                            type="button"
                            onClick={() => handleCopyMasked(lic.licenseKeyPrefix, lic.id)}
                            title="Copy key prefix"
                            className="text-muted-foreground hover:text-foreground"
                          >
                            {copiedKeyId === lic.id ? (
                              <Check className="h-3.5 w-3.5 text-emerald-500" />
                            ) : (
                              <Copy className="h-3.5 w-3.5" />
                            )}
                          </button>
                        </div>
                        {lic.externalReference && (
                          <div className="text-[10px] font-sans text-muted-foreground">
                            Ref: {lic.externalReference}
                          </div>
                        )}
                      </td>

                      {/* Product */}
                      <td className="px-4 py-3 text-foreground font-medium">
                        {prodName}
                      </td>

                      {/* Customer */}
                      <td className="px-4 py-3 text-muted-foreground">
                        {custName ? (
                          <span className="text-foreground font-medium">{custName}</span>
                        ) : (
                          <Badge variant="outline" className="text-[10px] font-normal text-muted-foreground">
                            Standalone
                          </Badge>
                        )}
                      </td>

                      {/* Status */}
                      <td className="px-4 py-3">
                        <Badge
                          variant={
                            lic.status === 'ACTIVE'
                              ? 'secondary'
                              : lic.status === 'SUSPENDED'
                              ? 'destructive'
                              : lic.status === 'REVOKED'
                              ? 'destructive'
                              : 'outline'
                          }
                          className="text-[10px] uppercase font-semibold"
                        >
                          {lic.status}
                        </Badge>
                      </td>

                      {/* Activations Meter */}
                      <td className="px-4 py-3 text-foreground">
                        <div className="flex items-center gap-2">
                          <Laptop className="h-3.5 w-3.5 text-muted-foreground" />
                          <span className="font-semibold">
                            {lic.currentActivations} / {lic.maxActivations}
                          </span>
                        </div>
                      </td>

                      {/* Expiry */}
                      <td className="px-4 py-3 text-muted-foreground">
                        {lic.isPerpetual ? (
                          <Badge variant="outline" className="text-[10px] text-emerald-500 border-emerald-500/30">
                            Perpetual
                          </Badge>
                        ) : lic.expiresAt ? (
                          <span>{new Date(lic.expiresAt).toLocaleDateString()}</span>
                        ) : (
                          <span>No expiry</span>
                        )}
                      </td>

                      {/* Actions */}
                      <td className="px-4 py-3 text-right">
                        <Button
                          variant="outline"
                          size="sm"
                          onClick={() => openDetails(lic)}
                          className="h-7 text-xs font-medium"
                        >
                          Manage
                        </Button>
                      </td>
                    </tr>
                  )
                })}
              </tbody>
            </table>
          </div>
        )}
      </div>

      {/* Issue License Dialog */}
      <IssueLicenseDialog
        open={issueOpen}
        onOpenChange={setIssueOpen}
        workspaceId={workspaceId}
        products={products}
        onSuccess={handleIssueSuccess}
      />

      {/* One-Time Plaintext Key Modal */}
      {newlyIssuedResponse && (
        <OneTimeKeyModal
          open={oneTimeOpen}
          onOpenChange={setOneTimeOpen}
          licenseKey={newlyIssuedResponse.licenseKey}
          productName={getProductName(newlyIssuedResponse.productId)}
          isPerpetual={newlyIssuedResponse.isPerpetual}
          expiresAt={newlyIssuedResponse.expiresAt}
        />
      )}

      {/* License Details & Activations Dialog */}
      {selectedLicense && (
        <LicenseDetailsDialog
          open={detailsOpen}
          onOpenChange={setDetailsOpen}
          license={selectedLicense}
          productName={getProductName(selectedLicense.productId)}
          customerName={getCustomerName(selectedLicense.customerId) || undefined}
          workspaceId={workspaceId}
          onUpdate={loadData}
        />
      )}
    </div>
  )
}

