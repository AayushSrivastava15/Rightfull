import { useState } from 'react'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import {
  Users,
  Search,
  Plus,
  Building,
  Mail,
  Hash,
  Archive,
  Edit2,
  Eye,
  Info,
  CheckCircle2,
  AlertTriangle,
  ChevronLeft,
  ChevronRight,
  Code2,
} from 'lucide-react'
import { apiClient } from '@/services/apiClient'
import type { Customer, CreateCustomerInput, UpdateCustomerInput } from '@/types'
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

interface CustomerDirectoryPanelProps {
  workspaceId: string
  canManage: boolean
}

export function CustomerDirectoryPanel({
  workspaceId,
  canManage,
}: CustomerDirectoryPanelProps) {
  const queryClient = useQueryClient()
  const [searchQuery, setSearchQuery] = useState('')
  const [statusFilter, setStatusFilter] = useState<'ALL' | 'ACTIVE' | 'ARCHIVED'>('ALL')
  const [page, setPage] = useState(0)
  const pageSize = 10

  const [isCreateOpen, setIsCreateOpen] = useState(false)
  const [editingCustomer, setEditingCustomer] = useState<Customer | null>(null)
  const [viewingCustomer, setViewingCustomer] = useState<Customer | null>(null)

  // Form State
  const [name, setName] = useState('')
  const [email, setEmail] = useState('')
  const [company, setCompany] = useState('')
  const [externalReference, setExternalReference] = useState('')
  const [metadataText, setMetadataText] = useState('{}')
  const [formError, setFormError] = useState<string | null>(null)

  const {
    data: pageData,
    isLoading,
    error,
  } = useQuery({
    queryKey: ['customers', workspaceId, searchQuery, statusFilter, page],
    queryFn: () =>
      apiClient.getCustomers(
        workspaceId,
        searchQuery || undefined,
        statusFilter === 'ALL' ? undefined : statusFilter,
        page,
        pageSize
      ),
    enabled: !!workspaceId,
  })

  const createMutation = useMutation({
    mutationFn: (data: CreateCustomerInput) => apiClient.createCustomer(workspaceId, data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['customers', workspaceId] })
      setIsCreateOpen(false)
      resetForm()
    },
    onError: (err: any) => {
      setFormError(err.message || 'Failed to create customer')
    },
  })

  const updateMutation = useMutation({
    mutationFn: ({ id, data }: { id: string; data: UpdateCustomerInput }) =>
      apiClient.updateCustomer(workspaceId, id, data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['customers', workspaceId] })
      setEditingCustomer(null)
      resetForm()
    },
    onError: (err: any) => {
      setFormError(err.message || 'Failed to update customer')
    },
  })

  const archiveMutation = useMutation({
    mutationFn: (id: string) => apiClient.archiveCustomer(workspaceId, id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['customers', workspaceId] })
    },
  })

  function resetForm() {
    setName('')
    setEmail('')
    setCompany('')
    setExternalReference('')
    setMetadataText('{}')
    setFormError(null)
  }

  function openCreate() {
    resetForm()
    setIsCreateOpen(true)
  }

  function openEdit(customer: Customer) {
    resetForm()
    setEditingCustomer(customer)
    setName(customer.name)
    setEmail(customer.email)
    setCompany(customer.company || '')
    setExternalReference(customer.externalReference || '')
    setMetadataText(JSON.stringify(customer.metadata || {}, null, 2))
  }

  function handleSave() {
    setFormError(null)
    if (!name.trim()) {
      setFormError('Customer name is required')
      return
    }
    if (!editingCustomer && !email.trim()) {
      setFormError('Customer email is required')
      return
    }

    let parsedMetadata = {}
    try {
      parsedMetadata = JSON.parse(metadataText || '{}')
      if (typeof parsedMetadata !== 'object' || Array.isArray(parsedMetadata)) {
        throw new Error('Metadata must be a JSON object')
      }
    } catch (e: any) {
      setFormError(`Invalid Metadata JSON: ${e.message}`)
      return
    }

    if (editingCustomer) {
      const updateData: UpdateCustomerInput = {
        name: name.trim(),
        company: company.trim() || undefined,
        externalReference: externalReference.trim() || undefined,
        metadata: parsedMetadata,
      }
      updateMutation.mutate({ id: editingCustomer.id, data: updateData })
    } else {
      const createData: CreateCustomerInput = {
        name: name.trim(),
        email: email.trim(),
        company: company.trim() || undefined,
        externalReference: externalReference.trim() || undefined,
        metadata: parsedMetadata,
      }
      createMutation.mutate(createData)
    }
  }

  const customers = pageData?.content || []
  const totalPages = pageData?.totalPages || 0

  return (
    <div className="space-y-6">
      {/* Principle & Optionality Callout */}
      <div className="flex items-start gap-4 rounded-xl border border-zinc-800 bg-zinc-900/50 p-4 text-zinc-300">
        <div className="mt-0.5 rounded-lg bg-indigo-500/10 p-2 text-indigo-400 ring-1 ring-indigo-500/20">
          <Info className="h-5 w-5" />
        </div>
        <div className="space-y-1 text-sm">
          <p className="font-semibold text-white">Optional Customer Management</p>
          <p className="text-xs text-zinc-400">
            Rightful supports named customer assignment for B2B contracts, as well as anonymous,
            device-bound, or standalone license keys without a customer record. Customer records are
            never a mandatory prerequisite for licensing.
          </p>
        </div>
      </div>

      {/* Filter and Action Bar */}
      <div className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
        <div className="flex flex-1 items-center gap-3">
          <div className="relative flex-1 max-w-sm">
            <Search className="absolute left-3 top-2.5 h-4 w-4 text-zinc-500" />
            <Input
              value={searchQuery}
              onChange={(e) => {
                setSearchQuery(e.target.value)
                setPage(0)
              }}
              placeholder="Search customers by name or email..."
              className="pl-9 border-zinc-800 bg-zinc-900 text-white placeholder:text-zinc-500 text-sm"
            />
          </div>

          <select
            value={statusFilter}
            onChange={(e) => {
              setStatusFilter(e.target.value as any)
              setPage(0)
            }}
            className="rounded-lg border border-zinc-800 bg-zinc-900 px-3 py-2 text-xs font-medium text-zinc-300 focus:outline-none focus:ring-1 focus:ring-blue-500"
          >
            <option value="ALL">All Statuses</option>
            <option value="ACTIVE">Active Only</option>
            <option value="ARCHIVED">Archived Only</option>
          </select>
        </div>

        {canManage && (
          <Button
            onClick={openCreate}
            size="sm"
            className="gap-2 bg-indigo-600 hover:bg-indigo-500 text-white"
          >
            <Plus className="h-4 w-4" />
            Register Customer
          </Button>
        )}
      </div>

      {/* Customers Table / Grid */}
      {isLoading ? (
        <div className="flex justify-center p-12 text-zinc-400">
          <div className="h-6 w-6 animate-spin rounded-full border-2 border-indigo-500 border-t-transparent" />
        </div>
      ) : error ? (
        <div className="rounded-xl border border-red-500/20 bg-red-500/10 p-6 text-sm text-red-400">
          Failed to load customers directory.
        </div>
      ) : customers.length === 0 ? (
        <Card className="border-dashed border-zinc-800 bg-zinc-900/20 text-center py-12">
          <CardContent className="space-y-3">
            <Users className="mx-auto h-10 w-10 text-zinc-600" />
            <div className="space-y-1">
              <h3 className="font-semibold text-white">No Customers Found</h3>
              <p className="text-xs text-zinc-500 max-w-sm mx-auto">
                {searchQuery
                  ? `No matching customer records for "${searchQuery}".`
                  : 'Register client organizations or individual licensees to associate with issued license keys.'}
              </p>
            </div>
            {canManage && !searchQuery && (
              <Button onClick={openCreate} size="sm" variant="outline" className="border-zinc-700 text-zinc-300">
                <Plus className="h-4 w-4 mr-1.5" />
                Add First Customer
              </Button>
            )}
          </CardContent>
        </Card>
      ) : (
        <div className="space-y-4">
          <div className="overflow-hidden rounded-xl border border-zinc-800 bg-zinc-900/60 shadow-lg">
            <table className="w-full text-left text-sm text-zinc-300">
              <thead className="border-b border-zinc-800 bg-zinc-950/50 text-[11px] font-semibold uppercase tracking-wider text-zinc-400">
                <tr>
                  <th className="px-4 py-3">Customer / Email</th>
                  <th className="px-4 py-3">Company</th>
                  <th className="px-4 py-3">External Ref</th>
                  <th className="px-4 py-3">Status</th>
                  <th className="px-4 py-3 text-right">Actions</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-zinc-800/60 text-xs">
                {customers.map((c) => (
                  <tr
                    key={c.id}
                    className={`hover:bg-zinc-800/30 transition-colors ${
                      c.status === 'ARCHIVED' ? 'opacity-60' : ''
                    }`}
                  >
                    <td className="px-4 py-3">
                      <div className="font-semibold text-white">{c.name}</div>
                      <div className="flex items-center gap-1 text-zinc-400 font-mono text-[11px]">
                        <Mail className="h-3 w-3 text-zinc-500" />
                        {c.email}
                      </div>
                    </td>
                    <td className="px-4 py-3">
                      {c.company ? (
                        <div className="flex items-center gap-1.5 text-zinc-300">
                          <Building className="h-3.5 w-3.5 text-zinc-500" />
                          <span>{c.company}</span>
                        </div>
                      ) : (
                        <span className="text-zinc-600">—</span>
                      )}
                    </td>
                    <td className="px-4 py-3 font-mono text-[11px]">
                      {c.externalReference ? (
                        <Badge variant="outline" className="border-zinc-800 text-zinc-400 font-mono text-[10px]">
                          {c.externalReference}
                        </Badge>
                      ) : (
                        <span className="text-zinc-600">—</span>
                      )}
                    </td>
                    <td className="px-4 py-3">
                      <Badge
                        variant={c.status === 'ACTIVE' ? 'default' : 'secondary'}
                        className={
                          c.status === 'ACTIVE'
                            ? 'bg-emerald-500/10 text-emerald-400 border border-emerald-500/20 text-[10px]'
                            : 'bg-zinc-800 text-zinc-400 text-[10px]'
                        }
                      >
                        {c.status}
                      </Badge>
                    </td>
                    <td className="px-4 py-3 text-right">
                      <div className="flex items-center justify-end gap-1.5">
                        <Button
                          variant="ghost"
                          size="sm"
                          className="h-7 w-7 p-0 text-zinc-400 hover:text-white"
                          onClick={() => setViewingCustomer(c)}
                          title="View Profile & Metadata"
                        >
                          <Eye className="h-3.5 w-3.5" />
                        </Button>

                        {canManage && c.status === 'ACTIVE' && (
                          <>
                            <Button
                              variant="ghost"
                              size="sm"
                              className="h-7 w-7 p-0 text-zinc-400 hover:text-white"
                              onClick={() => openEdit(c)}
                              title="Edit Customer"
                            >
                              <Edit2 className="h-3.5 w-3.5" />
                            </Button>
                            <Button
                              variant="ghost"
                              size="sm"
                              className="h-7 w-7 p-0 text-zinc-400 hover:text-red-400"
                              onClick={() => archiveMutation.mutate(c.id)}
                              disabled={archiveMutation.isPending}
                              title="Archive Customer"
                            >
                              <Archive className="h-3.5 w-3.5" />
                            </Button>
                          </>
                        )}
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>

          {/* Pagination */}
          {totalPages > 1 && (
            <div className="flex items-center justify-between px-2 text-xs text-zinc-400">
              <span>
                Page {page + 1} of {totalPages} ({pageData?.totalElements} total customers)
              </span>
              <div className="flex items-center gap-2">
                <Button
                  variant="outline"
                  size="sm"
                  onClick={() => setPage((p) => Math.max(0, p - 1))}
                  disabled={page === 0}
                  className="h-7 border-zinc-800 text-zinc-400"
                >
                  <ChevronLeft className="h-3.5 w-3.5" />
                </Button>
                <Button
                  variant="outline"
                  size="sm"
                  onClick={() => setPage((p) => Math.min(totalPages - 1, p + 1))}
                  disabled={page >= totalPages - 1}
                  className="h-7 border-zinc-800 text-zinc-400"
                >
                  <ChevronRight className="h-3.5 w-3.5" />
                </Button>
              </div>
            </div>
          )}
        </div>
      )}

      {/* Create / Edit Dialog */}
      <Dialog
        open={isCreateOpen || !!editingCustomer}
        onOpenChange={(open) => {
          if (!open) {
            setIsCreateOpen(false)
            setEditingCustomer(null)
          }
        }}
      >
        <DialogContent className="max-w-md border-zinc-800 bg-zinc-950 text-zinc-100">
          <DialogHeader>
            <DialogTitle className="text-lg font-bold text-white flex items-center gap-2">
              <Users className="h-5 w-5 text-indigo-400" />
              {editingCustomer ? 'Edit Customer' : 'Register Customer'}
            </DialogTitle>
            <DialogDescription className="text-xs text-zinc-400">
              {editingCustomer
                ? 'Update customer details and custom metadata attributes.'
                : 'Customer directory entries can be linked to issued software licenses.'}
            </DialogDescription>
          </DialogHeader>

          {formError && (
            <div className="rounded-lg border border-red-500/30 bg-red-500/10 p-3 text-xs text-red-400 flex items-center gap-2">
              <AlertTriangle className="h-4 w-4 flex-shrink-0" />
              <span>{formError}</span>
            </div>
          )}

          <div className="space-y-3 py-2 text-sm">
            <div className="space-y-1">
              <Label htmlFor="cust-name">Full Name or Account Name *</Label>
              <Input
                id="cust-name"
                value={name}
                onChange={(e) => setName(e.target.value)}
                placeholder="e.g. Jane Doe, Acme Security Inc."
                className="border-zinc-800 bg-zinc-900 text-white"
              />
            </div>

            {!editingCustomer && (
              <div className="space-y-1">
                <Label htmlFor="cust-email">Contact Email *</Label>
                <Input
                  id="cust-email"
                  type="email"
                  value={email}
                  onChange={(e) => setEmail(e.target.value)}
                  placeholder="billing@acme.com"
                  className="border-zinc-800 bg-zinc-900 text-white"
                />
              </div>
            )}

            <div className="grid grid-cols-2 gap-3">
              <div className="space-y-1">
                <Label htmlFor="cust-company">Company</Label>
                <Input
                  id="cust-company"
                  value={company}
                  onChange={(e) => setCompany(e.target.value)}
                  placeholder="Acme Corp"
                  className="border-zinc-800 bg-zinc-900 text-white"
                />
              </div>
              <div className="space-y-1">
                <Label htmlFor="cust-extref">External Reference</Label>
                <Input
                  id="cust-extref"
                  value={externalReference}
                  onChange={(e) => setExternalReference(e.target.value)}
                  placeholder="CRM-98765"
                  className="border-zinc-800 bg-zinc-900 text-white"
                />
              </div>
            </div>

            <div className="space-y-1">
              <Label className="text-xs font-semibold text-white flex items-center gap-1.5">
                <Code2 className="h-3.5 w-3.5 text-indigo-400" />
                Custom Metadata (JSON)
              </Label>
              <Textarea
                rows={4}
                value={metadataText}
                onChange={(e) => setMetadataText(e.target.value)}
                placeholder='{\n  "vat_number": "GB123456789",\n  "tier": "enterprise"\n}'
                className="font-mono text-xs border-zinc-800 bg-zinc-900 text-zinc-200"
              />
            </div>
          </div>

          <DialogFooter className="pt-2">
            <Button
              variant="outline"
              size="sm"
              onClick={() => {
                setIsCreateOpen(false)
                setEditingCustomer(null)
              }}
              className="border-zinc-800 text-zinc-400"
            >
              Cancel
            </Button>
            <Button
              size="sm"
              onClick={handleSave}
              disabled={createMutation.isPending || updateMutation.isPending}
              className="bg-indigo-600 hover:bg-indigo-500 text-white"
            >
              {createMutation.isPending || updateMutation.isPending ? 'Saving...' : 'Save Customer'}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      {/* Customer Profile & Metadata Sheet / Dialog */}
      <Dialog
        open={!!viewingCustomer}
        onOpenChange={(open) => !open && setViewingCustomer(null)}
      >
        <DialogContent className="border-zinc-800 bg-zinc-950 text-zinc-100 max-w-lg">
          <DialogHeader>
            <DialogTitle className="text-base font-bold text-white flex items-center gap-2">
              <Users className="h-4 w-4 text-indigo-400" />
              Customer Profile
            </DialogTitle>
            <DialogDescription className="text-xs text-zinc-400">
              Workspace-scoped client directory record and external metadata.
            </DialogDescription>
          </DialogHeader>

          {viewingCustomer && (
            <div className="space-y-4 text-xs">
              <div className="grid grid-cols-2 gap-3 rounded-lg border border-zinc-800 bg-zinc-900/40 p-3">
                <div>
                  <span className="text-zinc-500 block">Name</span>
                  <span className="font-semibold text-white">{viewingCustomer.name}</span>
                </div>
                <div>
                  <span className="text-zinc-500 block">Email</span>
                  <span className="font-mono text-zinc-300">{viewingCustomer.email}</span>
                </div>
                <div>
                  <span className="text-zinc-500 block">Company</span>
                  <span className="text-zinc-300">{viewingCustomer.company || '—'}</span>
                </div>
                <div>
                  <span className="text-zinc-500 block">External Ref</span>
                  <span className="font-mono text-zinc-300">
                    {viewingCustomer.externalReference || '—'}
                  </span>
                </div>
                <div>
                  <span className="text-zinc-500 block">Status</span>
                  <Badge
                    variant="outline"
                    className="border-zinc-700 text-zinc-300 text-[10px] mt-0.5"
                  >
                    {viewingCustomer.status}
                  </Badge>
                </div>
                <div>
                  <span className="text-zinc-500 block">Registered</span>
                  <span className="text-zinc-400">
                    {new Date(viewingCustomer.createdAt).toLocaleDateString()}
                  </span>
                </div>
              </div>

              <div className="space-y-1.5">
                <span className="font-semibold text-zinc-300 block">Metadata JSON</span>
                <pre className="p-3 rounded-lg bg-zinc-900 text-emerald-400 font-mono text-[11px] overflow-x-auto max-h-48 border border-zinc-800">
                  {JSON.stringify(viewingCustomer.metadata || {}, null, 2)}
                </pre>
              </div>

              <div className="rounded-lg border border-zinc-800 bg-zinc-900/20 p-2.5 text-zinc-400 text-[11px]">
                <p>
                  <strong>Associated Licenses:</strong> In Phase 4.2+, issued licenses will appear here when assigned to this customer account.
                </p>
              </div>
            </div>
          )}

          <DialogFooter>
            <Button
              size="sm"
              variant="outline"
              onClick={() => setViewingCustomer(null)}
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

