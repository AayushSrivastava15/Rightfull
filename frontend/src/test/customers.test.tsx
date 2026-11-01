import { render, screen, fireEvent } from '@testing-library/react'
import { describe, expect, it, vi, beforeEach } from 'vitest'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { CustomerDirectoryPanel } from '@/components/licensing/CustomerDirectoryPanel'
import { apiClient } from '@/services/apiClient'
import type { Customer, PageResponse } from '@/types'

vi.mock('@/services/apiClient', () => ({
  apiClient: {
    getCustomers: vi.fn(),
    createCustomer: vi.fn(),
    updateCustomer: vi.fn(),
    archiveCustomer: vi.fn(),
  },
}))

describe('CustomerDirectoryPanel Component', () => {
  let queryClient: QueryClient

  const mockCustomersPage: PageResponse<Customer> = {
    content: [
      {
        id: 'cust-001',
        workspaceId: 'ws-001',
        name: 'Jane Doe',
        email: 'jane@enterprise.com',
        company: 'Enterprise Corp',
        externalReference: 'CRM-1001',
        metadata: { tier: 'vip' },
        status: 'ACTIVE',
        createdAt: '2026-11-01T00:00:00Z',
        updatedAt: '2026-11-01T00:00:00Z',
      },
      {
        id: 'cust-002',
        workspaceId: 'ws-001',
        name: 'John Smith',
        email: 'john@startup.io',
        company: 'Startup IO',
        externalReference: 'CRM-1002',
        metadata: {},
        status: 'ARCHIVED',
        createdAt: '2026-11-01T01:00:00Z',
        updatedAt: '2026-11-01T01:00:00Z',
        archivedAt: '2026-11-01T02:00:00Z',
      },
    ],
    totalElements: 2,
    totalPages: 1,
    number: 0,
    size: 10,
  }

  beforeEach(() => {
    queryClient = new QueryClient({
      defaultOptions: {
        queries: { retry: false },
      },
    })
    vi.mocked(apiClient.getCustomers).mockResolvedValue(mockCustomersPage)
  })

  function renderComponent(canManage = true) {
    return render(
      <QueryClientProvider client={queryClient}>
        <CustomerDirectoryPanel workspaceId="ws-001" canManage={canManage} />
      </QueryClientProvider>
    )
  }

  it('renders optional customer management notice and search field', async () => {
    renderComponent()

    expect(
      screen.getByText(/Optional Customer Management/i)
    ).toBeInTheDocument()
    expect(
      screen.getByText(/Customer records are never a mandatory prerequisite for licensing/i)
    ).toBeInTheDocument()
    expect(
      screen.getByPlaceholderText(/Search customers by name or email/i)
    ).toBeInTheDocument()
  })

  it('renders customer directory table with records', async () => {
    renderComponent()

    expect(await screen.findByText('Jane Doe')).toBeInTheDocument()
    expect(screen.getByText('jane@enterprise.com')).toBeInTheDocument()
    expect(screen.getByText('Enterprise Corp')).toBeInTheDocument()
    expect(screen.getByText('CRM-1001')).toBeInTheDocument()

    expect(screen.getByText('John Smith')).toBeInTheDocument()
    expect(screen.getByText('john@startup.io')).toBeInTheDocument()
  })

  it('opens register customer dialog with fields', async () => {
    renderComponent()

    const registerBtn = await screen.findByRole('button', {
      name: /Register Customer/i,
    })
    fireEvent.click(registerBtn)

    expect(screen.getByRole('heading', { name: 'Register Customer' })).toBeInTheDocument()
    expect(screen.getByLabelText(/Full Name or Account Name/i)).toBeInTheDocument()
    expect(screen.getByLabelText(/Contact Email/i)).toBeInTheDocument()
    expect(screen.getByLabelText(/Company/i)).toBeInTheDocument()
    expect(screen.getByLabelText(/External Reference/i)).toBeInTheDocument()
  })

  it('views customer profile and metadata', async () => {
    renderComponent()

    const viewBtns = await screen.findAllByTitle('View Profile & Metadata')
    fireEvent.click(viewBtns[0]!)

    expect(screen.getByRole('heading', { name: 'Customer Profile' })).toBeInTheDocument()
    expect(screen.getByText(/"tier": "vip"/i)).toBeInTheDocument()
    expect(
      screen.getByText(/In Phase 4.2\+, issued licenses will appear here/i)
    ).toBeInTheDocument()
  })
})

