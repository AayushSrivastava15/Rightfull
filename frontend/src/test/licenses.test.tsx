import { describe, it, expect, vi, beforeEach } from 'vitest'
import { render, screen, fireEvent, waitFor } from '@testing-library/react'
import { IssuedLicensesPanel } from '@/components/licensing/IssuedLicensesPanel'
import { OneTimeKeyModal } from '@/components/licensing/OneTimeKeyModal'
import { LicenseDetailsDialog } from '@/components/licensing/LicenseDetailsDialog'
import { apiClient } from '@/services/apiClient'
import type { Product, License, LicenseSummary } from '@/types'

vi.mock('@/services/apiClient', () => ({
  apiClient: {
    getLicenses: vi.fn(),
    getLicenseSummary: vi.fn(),
    getCustomers: vi.fn(),
    getConfigurations: vi.fn(),
    getLicenseActivations: vi.fn(),
    issueLicense: vi.fn(),
    suspendLicense: vi.fn(),
    resumeLicense: vi.fn(),
    revokeLicense: vi.fn(),
    renewLicense: vi.fn(),
    deactivateDevice: vi.fn(),
  },
}))

describe('License Lifecycle Engine Frontend Components', () => {
  const mockProducts: Product[] = [
    {
      id: 'prod-001',
      workspaceId: 'ws-001',
      name: 'VectorDB Engine',
      slug: 'vectordb-engine',
      version: 'v2.0',
      status: 'ACTIVE',
      createdAt: '2026-11-01T10:00:00Z',
      updatedAt: '2026-11-01T10:00:00Z',
    },
  ]

  const mockLicenses: License[] = [
    {
      id: 'lic-001',
      workspaceId: 'ws-001',
      productId: 'prod-001',
      customerId: undefined,
      licenseKeyPrefix: 'RF-7K2M',
      licenseKeyMasked: 'RF-7K2M-••••-••••-••••-W9A1',
      licenseKeyLastFour: 'W9A1',
      status: 'ACTIVE',
      startsAt: '2026-11-01T10:00:00Z',
      expiresAt: '2027-11-01T10:00:00Z',
      isPerpetual: false,
      maxActivations: 3,
      currentActivations: 1,
      seatLimit: 10,
      offlineGracePeriodSeconds: 25200,
      revalidationIntervalSeconds: 300,
      entitlements: { max_nodes: 3 },
      signingKeyId: 'kid_test_1',
      createdAt: '2026-11-01T10:00:00Z',
      updatedAt: '2026-11-01T10:00:00Z',
    },
  ]

  const mockSummary: LicenseSummary = {
    totalLicenses: 1,
    activeLicenses: 1,
    expiredLicenses: 0,
    suspendedLicenses: 0,
    revokedLicenses: 0,
  }

  beforeEach(() => {
    vi.clearAllMocks()
    vi.mocked(apiClient.getLicenses).mockResolvedValue({
      content: mockLicenses,
      totalElements: 1,
      totalPages: 1,
      number: 0,
      size: 50,
    })
    vi.mocked(apiClient.getLicenseSummary).mockResolvedValue(mockSummary)
    vi.mocked(apiClient.getCustomers).mockResolvedValue({
      content: [],
      totalElements: 0,
      totalPages: 0,
      number: 0,
      size: 100,
    })
    vi.mocked(apiClient.getConfigurations).mockResolvedValue([])
    vi.mocked(apiClient.getLicenseActivations).mockResolvedValue({
      content: [],
      totalElements: 0,
      totalPages: 0,
      number: 0,
      size: 50,
    })
  })

  it('renders IssuedLicensesPanel with metrics summary and data table', async () => {
    render(<IssuedLicensesPanel workspaceId="ws-001" products={mockProducts} />)

    expect(screen.getByText('Issued Licenses')).toBeInTheDocument()
    await waitFor(() => {
      expect(screen.getByText('RF-7K2M-••••-••••-••••-W9A1')).toBeInTheDocument()
    })
    expect(screen.getByText('VectorDB Engine')).toBeInTheDocument()
    expect(screen.getByText('1 / 3')).toBeInTheDocument()
  })

  it('renders OneTimeKeyModal with raw plaintext key and copy capability', () => {
    const onOpenChange = vi.fn()
    render(
      <OneTimeKeyModal
        open={true}
        onOpenChange={onOpenChange}
        licenseKey="RF-7K2M-9X42-M7PQ-K28D-W9A1"
        productName="VectorDB Engine"
        isPerpetual={false}
        expiresAt="2027-11-01T10:00:00Z"
      />
    )

    expect(screen.getByText('License Key Issued Successfully')).toBeInTheDocument()
    expect(screen.getByText('RF-7K2M-9X42-M7PQ-K28D-W9A1')).toBeInTheDocument()
    expect(screen.getByText(/Save this key now\. It will never be shown again/i)).toBeInTheDocument()

    const copyBtn = screen.getByText('Copy Key')
    expect(copyBtn).toBeInTheDocument()

    const doneBtn = screen.getByText('I Have Securely Saved This Key')
    fireEvent.click(doneBtn)
    expect(onOpenChange).toHaveBeenCalledWith(false)
  })

  it('renders LicenseDetailsDialog with overview, entitlements, and lifecycle buttons', () => {
    const onOpenChange = vi.fn()
    const onUpdate = vi.fn()

    render(
      <LicenseDetailsDialog
        open={true}
        onOpenChange={onOpenChange}
        license={mockLicenses[0]}
        productName="VectorDB Engine"
        workspaceId="ws-001"
        onUpdate={onUpdate}
      />
    )

    expect(screen.getByText('Overview & Limits')).toBeInTheDocument()
    expect(screen.getByText('Entitlements Snapshot')).toBeInTheDocument()
    expect(screen.getByText(/Device Nodes/i)).toBeInTheDocument()
    expect(screen.getByText('Suspend License')).toBeInTheDocument()
    expect(screen.getByText('Revoke License')).toBeInTheDocument()
    expect(screen.getByText('Renew / Extend Expiry')).toBeInTheDocument()
  })
})

