import type { ApiResponse, SystemHealth, UserProfile } from '@/types'
import { supabase } from '@/lib/supabaseClient'

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api/v1'

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
    token = localStorage.getItem('rightful_token') || undefined
    if (!token) {
      try {
        const { data } = await supabase.auth.getSession()
        token = data.session?.access_token
      } catch (e) {
        console.warn('Failed to retrieve Supabase session for API request:', e)
      }
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
    return request<SystemHealth>('/health')
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
}

