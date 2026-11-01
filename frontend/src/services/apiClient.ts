import type {
  ApiResponse,
  SystemHealth,
  UserProfile,
  Product,
  LicenseConfiguration,
  CreateConfigurationInput,
  UpdateConfigurationInput,
  Customer,
  CreateCustomerInput,
  UpdateCustomerInput,
  PageResponse,
  License,
  IssueLicenseInput,
  IssueLicenseResponse,
  LicenseSummary,
  LicenseActivation,
  LicenseStatus,
} from '@/types'
import { supabase } from '@/lib/supabaseClient'

const API_BASE_URL = import.meta.env['VITE_API_BASE_URL'] || 'http://localhost:8080/api/v1'

export class ApiError extends Error {
  constructor(
    public status: number,
    public code: string,
    message: string
  ) {
    super(message)
    this.name = 'ApiError'
  }
}

async function request<T>(endpoint: string, options: RequestInit = {}): Promise<T> {
  let token: string | undefined

  if (typeof window !== 'undefined') {
    try {
      const { data } = await supabase.auth.getSession()
      token = data.session?.access_token
    } catch (e) {
      console.warn('Failed to retrieve Supabase session for API request:', e)
    }
    if (!token) {
      token = localStorage.getItem('rightful_token') || undefined
    }
  }

  const headers: Record<string, string> = {
    'Content-Type': 'application/json',
    ...(options.headers as Record<string, string>),
  }

  if (token) {
    headers['Authorization'] = `Bearer ${token}`
  }

  const response = await fetch(`${API_BASE_URL}${endpoint}`, {
    ...options,
    headers,
  })

  if (!response.ok) {
    const errorBody = await response.json().catch(() => null)
    throw new ApiError(
      response.status,
      errorBody?.error?.code || 'UNKNOWN_ERROR',
      errorBody?.error?.message || response.statusText
    )
  }

  const json: ApiResponse<T> = await response.json()
  return json.data as T
}

export const apiClient = {
  getHealth: async (): Promise<SystemHealth> => {
    try {
      const response = await fetch(`${API_BASE_URL}/health`)
      if (!response.ok) {
        return { status: 'DOWN', version: '1.0.0', product: 'rightfull', timestamp: new Date().toISOString() }
      }
      const json = await response.json()
      if (json?.data?.status) {
        return json.data as SystemHealth
      }
      if (json?.status) {
        return {
          status: json.status,
          version: json.version || '1.0.0',
          product: json.application || 'rightfull',
          timestamp: new Date().toISOString()
        }
      }
      return { status: 'DOWN', version: '1.0.0', product: 'rightfull', timestamp: new Date().toISOString() }
    } catch {
      return { status: 'DOWN', version: '1.0.0', product: 'rightfull', timestamp: new Date().toISOString() }
    }
  },
  getUserProfile: async (): Promise<UserProfile> => {
    return request<UserProfile>('/users/me')
  },
  get: <T>(endpoint: string) => request<T>(endpoint, { method: 'GET' }),
  post: <T>(endpoint: string, body: unknown) =>
    request<T>(endpoint, { method: 'POST', body: JSON.stringify(body) }),
  put: <T>(endpoint: string, body: unknown) =>
    request<T>(endpoint, { method: 'PUT', body: JSON.stringify(body) }),
  delete: <T>(endpoint: string) => request<T>(endpoint, { method: 'DELETE' }),

  // Products
  getProducts: async (workspaceId: string): Promise<Product[]> => {
    const res = await request<any>(`/products?workspaceId=${workspaceId}`)
    // Handle both Page<Product> and Product[] responses gracefully
    if (res && Array.isArray(res.content)) {
      return res.content as Product[]
    }
    if (Array.isArray(res)) {
      return res as Product[]
    }
    return []
  },

  createProduct: async (
    workspaceId: string,
    data: { name: string; slug: string; description?: string }
  ): Promise<Product> => {
    return request<Product>(`/products?workspaceId=${workspaceId}`, {
      method: 'POST',
      body: JSON.stringify(data),
    })
  },

  archiveProduct: async (
    workspaceId: string,
    productId: string
  ): Promise<Product> => {
    return request<Product>(
      `/products/${productId}/archive?workspaceId=${workspaceId}`,
      { method: 'POST' }
    )
  },

  rotateSigningKey: async (
    workspaceId: string,
    productId: string
  ): Promise<any> => {
    return request<any>(
      `/products/${productId}/signing-keys/rotate?workspaceId=${workspaceId}`,
      { method: 'POST' }
    )
  },

  // License Configurations
  getConfigurations: async (
    workspaceId: string,
    productId: string
  ): Promise<LicenseConfiguration[]> => {
    return request<LicenseConfiguration[]>(
      `/products/${productId}/license-configurations/all?workspaceId=${workspaceId}`
    )
  },

  getConfigurationsPaged: async (
    workspaceId: string,
    productId: string,
    page = 0,
    size = 20
  ): Promise<PageResponse<LicenseConfiguration>> => {
    return request<PageResponse<LicenseConfiguration>>(
      `/products/${productId}/license-configurations?workspaceId=${workspaceId}&page=${page}&size=${size}`
    )
  },

  createConfiguration: async (
    workspaceId: string,
    productId: string,
    data: CreateConfigurationInput
  ): Promise<LicenseConfiguration> => {
    return request<LicenseConfiguration>(
      `/products/${productId}/license-configurations?workspaceId=${workspaceId}`,
      { method: 'POST', body: JSON.stringify(data) }
    )
  },

  updateConfiguration: async (
    workspaceId: string,
    productId: string,
    configurationId: string,
    data: UpdateConfigurationInput
  ): Promise<LicenseConfiguration> => {
    return request<LicenseConfiguration>(
      `/products/${productId}/license-configurations/${configurationId}?workspaceId=${workspaceId}`,
      { method: 'PUT', body: JSON.stringify(data) }
    )
  },

  archiveConfiguration: async (
    workspaceId: string,
    productId: string,
    configurationId: string
  ): Promise<LicenseConfiguration> => {
    return request<LicenseConfiguration>(
      `/products/${productId}/license-configurations/${configurationId}/archive?workspaceId=${workspaceId}`,
      { method: 'POST' }
    )
  },

  // Customers
  getCustomers: async (
    workspaceId: string,
    query?: string,
    status?: string,
    page = 0,
    size = 20
  ): Promise<PageResponse<Customer>> => {
    const params = new URLSearchParams({
      workspaceId,
      page: page.toString(),
      size: size.toString(),
    })
    if (query) params.append('query', query)
    if (status) params.append('status', status)
    return request<PageResponse<Customer>>(`/customers?${params.toString()}`)
  },

  getCustomer: async (workspaceId: string, customerId: string): Promise<Customer> => {
    return request<Customer>(`/customers/${customerId}?workspaceId=${workspaceId}`)
  },

  createCustomer: async (
    workspaceId: string,
    data: CreateCustomerInput
  ): Promise<Customer> => {
    return request<Customer>(`/customers?workspaceId=${workspaceId}`, {
      method: 'POST',
      body: JSON.stringify(data),
    })
  },

  updateCustomer: async (
    workspaceId: string,
    customerId: string,
    data: UpdateCustomerInput
  ): Promise<Customer> => {
    return request<Customer>(`/customers/${customerId}?workspaceId=${workspaceId}`, {
      method: 'PUT',
      body: JSON.stringify(data),
    })
  },

  archiveCustomer: async (workspaceId: string, customerId: string): Promise<Customer> => {
    return request<Customer>(`/customers/${customerId}/archive?workspaceId=${workspaceId}`, {
      method: 'POST',
    })
  },

  // Licenses
  getLicenses: async (
    workspaceId: string,
    productId?: string,
    customerId?: string,
    status?: LicenseStatus,
    search?: string,
    page = 0,
    size = 20
  ): Promise<PageResponse<License>> => {
    const params = new URLSearchParams({
      page: page.toString(),
      size: size.toString(),
    })
    if (productId) params.append('productId', productId)
    if (customerId) params.append('customerId', customerId)
    if (status) params.append('status', status)
    if (search) params.append('search', search)

    return request<PageResponse<License>>(`/workspaces/${workspaceId}/licenses?${params.toString()}`)
  },

  getLicenseSummary: async (workspaceId: string): Promise<LicenseSummary> => {
    return request<LicenseSummary>(`/workspaces/${workspaceId}/licenses/summary`)
  },

  getLicense: async (workspaceId: string, licenseId: string): Promise<License> => {
    return request<License>(`/licenses/${licenseId}?workspaceId=${workspaceId}`)
  },

  issueLicense: async (
    workspaceId: string,
    productId: string,
    data: IssueLicenseInput
  ): Promise<IssueLicenseResponse> => {
    return request<IssueLicenseResponse>(`/products/${productId}/licenses?workspaceId=${workspaceId}`, {
      method: 'POST',
      body: JSON.stringify(data),
    })
  },

  suspendLicense: async (
    workspaceId: string,
    licenseId: string,
    reason?: string
  ): Promise<License> => {
    return request<License>(`/licenses/${licenseId}/suspend?workspaceId=${workspaceId}`, {
      method: 'POST',
      body: JSON.stringify({ reason: reason || 'Administrative suspension' }),
    })
  },

  resumeLicense: async (workspaceId: string, licenseId: string): Promise<License> => {
    return request<License>(`/licenses/${licenseId}/resume?workspaceId=${workspaceId}`, {
      method: 'POST',
    })
  },

  revokeLicense: async (
    workspaceId: string,
    licenseId: string,
    reason?: string
  ): Promise<License> => {
    return request<License>(`/licenses/${licenseId}/revoke?workspaceId=${workspaceId}`, {
      method: 'POST',
      body: JSON.stringify({ reason: reason || 'Administrative revocation' }),
    })
  },

  renewLicense: async (
    workspaceId: string,
    licenseId: string,
    extensionDays?: number,
    newExpiresAt?: string
  ): Promise<License> => {
    return request<License>(`/licenses/${licenseId}/renew?workspaceId=${workspaceId}`, {
      method: 'POST',
      body: JSON.stringify({ extensionDays, newExpiresAt }),
    })
  },

  getLicenseActivations: async (
    workspaceId: string,
    licenseId: string,
    page = 0,
    size = 20
  ): Promise<PageResponse<LicenseActivation>> => {
    const params = new URLSearchParams({
      workspaceId,
      page: page.toString(),
      size: size.toString(),
    })
    return request<PageResponse<LicenseActivation>>(`/licenses/${licenseId}/activations?${params.toString()}`)
  },

  deactivateDevice: async (
    workspaceId: string,
    licenseId: string,
    activationId: string
  ): Promise<LicenseActivation> => {
    return request<LicenseActivation>(
      `/licenses/${licenseId}/activations/${activationId}/deactivate?workspaceId=${workspaceId}`,
      { method: 'POST' }
    )
  },
}

