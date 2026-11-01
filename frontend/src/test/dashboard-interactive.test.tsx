import React from 'react'
import { render, screen, fireEvent, waitFor } from '@testing-library/react'
import { describe, it, expect, vi, beforeEach } from 'vitest'
import { OverviewView } from '@/components/dashboard/OverviewView'
import { ProductsView } from '@/components/dashboard/ProductsView'
import { OnboardingChecklist } from '@/components/dashboard/OnboardingChecklist'
import { CommandMenu } from '@/components/dashboard/CommandMenu'
import { TerminalBlock } from '@/components/ui/terminal-block'
import { ThemeProvider } from '@/contexts/ThemeContext'
import { AuthContext } from '@/contexts/AuthContext'
import { type Product, type Customer, type LicenseConfiguration } from '@/types'

const mockProducts: Product[] = [
  {
    id: 'prod-1',
    workspaceId: 'ws-1',
    name: 'Rightful Core Engine',
    slug: 'rightful-core',
    description: 'High-performance licensing daemon',
    status: 'ACTIVE',
    createdAt: '2026-10-30T10:00:00Z',
    updatedAt: '2026-10-30T10:00:00Z',
  },
  {
    id: 'prod-2',
    workspaceId: 'ws-1',
    name: 'Acme Legacy CLI',
    slug: 'acme-cli',
    description: 'Old command line utility',
    status: 'ARCHIVED',
    createdAt: '2026-09-15T10:00:00Z',
    updatedAt: '2026-09-15T10:00:00Z',
  },
]

const mockConfigurations: LicenseConfiguration[] = [
  {
    id: 'conf-1',
    workspaceId: 'ws-1',
    productId: 'prod-1',
    name: 'Enterprise Plan',
    description: 'All features enabled',
    durationDays: 365,
    maximumActivations: 5,
    seatLimit: 25,
    offlineGracePeriodSeconds: 25200,
    revalidationIntervalSeconds: 300,
    entitlements: { max_nodes: 50 },
    status: 'ACTIVE',
    version: 2,
    createdAt: '2026-10-31T12:00:00Z',
    updatedAt: '2026-11-01T10:00:00Z',
  },
]

const mockCustomers: Customer[] = [
  {
    id: 'cust-1',
    workspaceId: 'ws-1',
    name: 'Starlight Tech',
    email: 'ops@starlight.io',
    company: 'Starlight Inc',
    status: 'ACTIVE',
    metadata: { tier: 'Strategic' },
    createdAt: '2026-11-01T15:00:00Z',
    updatedAt: '2026-11-01T15:00:00Z',
  },
]

const mockAuthValue: any = {
  user: { id: 'user-1', email: 'alex@company.com' },
  profile: {
    id: 'user-1',
    email: 'alex@company.com',
    fullName: 'Alex Vance',
    defaultWorkspace: { id: 'ws-1', name: 'Acme Software', slug: 'acme-sw', role: 'ADMIN', tier: 'PRO' },
    memberships: [],
  },
  session: { access_token: 'dummy-token' },
  loading: false,
  error: null,
  signOut: vi.fn(),
}

describe('Dashboard Interactive UI & Components', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('renders Overview with personalized greeting, workspace name, and real metric counts', () => {
    const onNavigate = vi.fn()
    const onOpenCreate = vi.fn()

    render(
      <ThemeProvider>
        <OverviewView
          displayName="Alex"
          workspaceName="Acme Software"
          products={mockProducts}
          configurations={mockConfigurations}
          customers={mockCustomers}
          onNavigate={onNavigate}
          onOpenCreateProduct={onOpenCreate}
        />
      </ThemeProvider>
    )

    // Greeting contains Alex
    expect(screen.getByRole('heading', { level: 1 })).toHaveTextContent(/Alex/i)
    expect(screen.getByText(/Acme Software/i)).toBeInTheDocument()

    // Metric titles
    expect(screen.getByText('Total Products')).toBeInTheDocument()
    expect(screen.getAllByText('License Plans').length).toBeGreaterThan(0)
    expect(screen.getByText('Customers')).toBeInTheDocument()
    expect(screen.getByText('Cryptography')).toBeInTheDocument()

    // Recent activity feed contains products and customers
    expect(screen.getByText(/Product "Rightful Core Engine" registered/i)).toBeInTheDocument()
    expect(screen.getByText(/Customer "Starlight Tech" added/i)).toBeInTheDocument()
  })

  it('computes OnboardingChecklist completion progress from real state', () => {
    const onNavigate = vi.fn()

    render(
      <ThemeProvider>
        <OnboardingChecklist
          hasProducts={true}
          hasSigningKeys={true}
          hasConfigurations={true}
          hasCustomers={true}
          onNavigate={onNavigate}
        />
      </ThemeProvider>
    )

    // 4 out of 5 complete (docs reviewed is stored in localStorage)
    expect(screen.getByText(/4 of 5 Complete/i)).toBeInTheDocument()
    expect(screen.getByText(/Register your first product/i)).toBeInTheDocument()
    expect(screen.getByText(/Generate Ed25519 signing keys/i)).toBeInTheDocument()
  })

  it('renders ProductsView, filters by search query and status', async () => {
    const onRefresh = vi.fn()
    const setIsCreateOpen = vi.fn()

    render(
      <ThemeProvider>
        <ProductsView
          workspaceId="ws-1"
          products={mockProducts}
          canManage={true}
          isLoading={false}
          onRefresh={onRefresh}
          isCreateOpen={false}
          setIsCreateOpen={setIsCreateOpen}
        />
      </ThemeProvider>
    )

    // Both products visible initially
    expect(screen.getByText('Rightful Core Engine')).toBeInTheDocument()
    expect(screen.getByText('Acme Legacy CLI')).toBeInTheDocument()

    // Search for "Core"
    const searchInput = screen.getByPlaceholderText(/Search products/i)
    fireEvent.change(searchInput, { target: { value: 'Core' } })

    expect(screen.getByText('Rightful Core Engine')).toBeInTheDocument()
    expect(screen.queryByText('Acme Legacy CLI')).not.toBeInTheDocument()

    // Filter by Active status
    fireEvent.change(searchInput, { target: { value: '' } })
    fireEvent.click(screen.getByRole('button', { name: 'Active' }))

    expect(screen.getByText('Rightful Core Engine')).toBeInTheDocument()
    expect(screen.queryByText('Acme Legacy CLI')).not.toBeInTheDocument()
  })

  it('renders TerminalBlock and executes copy action with feedback', async () => {
    // Mock navigator.clipboard
    const writeTextMock = vi.fn().mockResolvedValue(undefined)
    Object.assign(navigator, {
      clipboard: { writeText: writeTextMock },
    })

    render(
      <TerminalBlock
        title="curl test"
        code='curl -X GET "http://localhost:8080/api/v1/health"'
        language="bash"
      />
    )

    expect(screen.getByText(/curl -X GET/i)).toBeInTheDocument()
    const copyBtn = screen.getByRole('button', { name: /Copy code/i })
    fireEvent.click(copyBtn)

    expect(writeTextMock).toHaveBeenCalledWith('curl -X GET "http://localhost:8080/api/v1/health"')
    expect(await screen.findByText('Copied')).toBeInTheDocument()
  })

  it('opens and navigates through CommandMenu', () => {
    const onNavigate = vi.fn()
    const onOpenChange = vi.fn()

    render(
      <ThemeProvider>
        <AuthContext.Provider value={mockAuthValue}>
          <CommandMenu open={true} onOpenChange={onOpenChange} onNavigate={onNavigate} />
        </AuthContext.Provider>
      </ThemeProvider>
    )

    expect(screen.getByPlaceholderText(/Type a command or search/i)).toBeInTheDocument()
    expect(screen.getByText('Products & Signing Keys')).toBeInTheDocument()
    expect(screen.getByText('License Configurations')).toBeInTheDocument()

    // Select Products
    fireEvent.click(screen.getByText('Products & Signing Keys'))
    expect(onNavigate).toHaveBeenCalledWith('products')
    expect(onOpenChange).toHaveBeenCalledWith(false)
  })
})
