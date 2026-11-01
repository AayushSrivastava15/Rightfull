import { render, screen, fireEvent } from '@testing-library/react'
import { describe, expect, it, vi, beforeEach } from 'vitest'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { LicenseConfigurationPanel } from '@/components/licensing/LicenseConfigurationPanel'
import { apiClient } from '@/services/apiClient'
import type { Product, LicenseConfiguration } from '@/types'

vi.mock('@/services/apiClient', () => ({
  apiClient: {
    getConfigurations: vi.fn(),
    createConfiguration: vi.fn(),
    updateConfiguration: vi.fn(),
    archiveConfiguration: vi.fn(),
  },
}))

describe('LicenseConfigurationPanel Component', () => {
  let queryClient: QueryClient

  const mockProducts: Product[] = [
    {
      id: 'prod-001',
      workspaceId: 'ws-001',
      name: 'CloudSync Pro',
      code: 'CLOUDSYNC',
      description: 'Enterprise sync engine',
      status: 'ACTIVE',
      createdAt: '2026-10-31T00:00:00Z',
      updatedAt: '2026-10-31T00:00:00Z',
    },
  ]

  const mockConfigurations: LicenseConfiguration[] = [
    {
      id: 'cfg-001',
      workspaceId: 'ws-001',
      productId: 'prod-001',
      name: 'Standard Developer Preset',
      description: 'Preset for developer licenses',
      durationDays: 365,
      maximumActivations: 2,
      seatLimit: 1,
      offlineGracePeriodSeconds: 25200,
      revalidationIntervalSeconds: 300,
      entitlements: {
        features: { pdf_export: true },
        limits: { max_projects: 50 },
      },
      status: 'ACTIVE',
      version: 1,
      createdAt: '2026-11-01T00:00:00Z',
      updatedAt: '2026-11-01T00:00:00Z',
    },
    {
      id: 'cfg-002',
      workspaceId: 'ws-001',
      productId: 'prod-001',
      name: 'Perpetual Core Preset',
      description: 'Lifetime license without expiry',
      durationDays: null,
      maximumActivations: 5,
      seatLimit: 5,
      offlineGracePeriodSeconds: 86400,
      revalidationIntervalSeconds: 600,
      entitlements: {
        features: { enterprise_sso: true },
      },
      status: 'ACTIVE',
      version: 2,
      createdAt: '2026-11-01T01:00:00Z',
      updatedAt: '2026-11-01T01:00:00Z',
    },
  ]

  beforeEach(() => {
    queryClient = new QueryClient({
      defaultOptions: {
        queries: { retry: false },
      },
    })
    vi.mocked(apiClient.getConfigurations).mockResolvedValue(mockConfigurations)
  })

  function renderComponent(canManage = true) {
    return render(
      <QueryClientProvider client={queryClient}>
        <LicenseConfigurationPanel
          workspaceId="ws-001"
          products={mockProducts}
          canManage={canManage}
        />
      </QueryClientProvider>
    )
  }

  it('renders architectural callout and product selector', async () => {
    renderComponent()

    expect(
      screen.getByText(/Licensing Infrastructure Presets/i)
    ).toBeInTheDocument()
    expect(
      screen.getByText(/These configurations are reusable presets/i)
    ).toBeInTheDocument()
    expect(screen.getByRole('combobox', { name: /Product:/i })).toBeInTheDocument()
  })

  it('renders configurations list with validity and activations', async () => {
    renderComponent()

    expect(await screen.findByText('Standard Developer Preset')).toBeInTheDocument()
    expect(screen.getByText('365 Days')).toBeInTheDocument()
    expect(screen.getByText('2 Nodes')).toBeInTheDocument()
    expect(screen.getByText('v1')).toBeInTheDocument()

    expect(screen.getByText('Perpetual Core Preset')).toBeInTheDocument()
    expect(screen.getByText('Perpetual (Lifetime)')).toBeInTheDocument()
    expect(screen.getByText('5 Nodes')).toBeInTheDocument()
    expect(screen.getByText('v2')).toBeInTheDocument()
  })

  it('opens create modal with default values and perpetual toggle', async () => {
    renderComponent()

    const newBtn = await screen.findByRole('button', {
      name: /New Configuration Preset/i,
    })
    fireEvent.click(newBtn)

    expect(
      screen.getByText('Create License Configuration Preset')
    ).toBeInTheDocument()

    const perpetualCheckbox = screen.getByLabelText(/Perpetual \/ Lifetime/i)
    expect(perpetualCheckbox).toBeInTheDocument()
    expect(perpetualCheckbox).not.toBeChecked()

    fireEvent.click(perpetualCheckbox)
    expect(perpetualCheckbox).toBeChecked()
    expect(
      screen.getByText(/License will be issued without an expiration timestamp/i)
    ).toBeInTheDocument()
  })

  it('inspects entitlements JSON payload in modal', async () => {
    renderComponent()

    const inspectBtns = await screen.findAllByRole('button', {
      name: /Inspect Entitlements/i,
    })
    fireEvent.click(inspectBtns[0]!)

    expect(screen.getByText('Entitlements Payload')).toBeInTheDocument()
    expect(screen.getByText(/"pdf_export": true/i)).toBeInTheDocument()
  })
})

