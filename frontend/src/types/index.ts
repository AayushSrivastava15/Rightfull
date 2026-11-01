export type UserRole = 'OWNER' | 'ADMIN' | 'MEMBER' | 'VIEWER'

export interface ApiResponse<T> {
  success: boolean
  data?: T
  message?: string
  timestamp: string
}

export interface SystemHealth {
  status: 'UP' | 'DOWN' | 'DEGRADED'
  version: string
  product: string
  javaVersion?: string
  springBootVersion?: string
  timestamp: string
}

export interface WorkspaceSummary {
  id: string
  name: string
  slug: string
  tier: 'COMMUNITY' | 'PRO' | 'ENTERPRISE'
  role: UserRole
}

export interface WorkspaceMembership {
  workspaceId: string
  workspaceName: string
  workspaceSlug: string
  workspaceTier: 'COMMUNITY' | 'PRO' | 'ENTERPRISE'
  role: UserRole
  joinedAt: string
}

export interface UserProfile {
  id: string
  supabaseUserId: string
  email: string
  fullName?: string
  avatarUrl?: string
  active: boolean
  createdAt: string
  defaultWorkspace?: WorkspaceSummary
  memberships: WorkspaceMembership[]
}

export interface Product {
  id: string
  workspaceId: string
  name: string
  code: string
  description?: string
  status: 'ACTIVE' | 'ARCHIVED'
  createdAt: string
  updatedAt: string
}

export interface LicenseConfiguration {
  id: string
  workspaceId: string
  productId: string
  name: string
  description?: string
  durationDays?: number | null
  maximumActivations: number
  seatLimit: number
  offlineGracePeriodSeconds: number
  revalidationIntervalSeconds: number
  entitlements: Record<string, any>
  status: 'ACTIVE' | 'ARCHIVED'
  version: number
  createdAt: string
  updatedAt: string
  archivedAt?: string
}

export interface CreateConfigurationInput {
  name: string
  description?: string
  durationDays?: number | null
  maximumActivations: number
  seatLimit: number
  offlineGracePeriodSeconds?: number
  revalidationIntervalSeconds?: number
  entitlements?: Record<string, any>
}

export interface UpdateConfigurationInput {
  name: string
  description?: string
  durationDays?: number | null
  maximumActivations: number
  seatLimit: number
  offlineGracePeriodSeconds: number
  revalidationIntervalSeconds: number
  entitlements?: Record<string, any>
}

export interface Customer {
  id: string
  workspaceId: string
  name: string
  email: string
  company?: string
  externalReference?: string
  metadata: Record<string, any>
  status: 'ACTIVE' | 'ARCHIVED'
  createdAt: string
  updatedAt: string
  archivedAt?: string
}

export interface CreateCustomerInput {
  name: string
  email: string
  company?: string
  externalReference?: string
  metadata?: Record<string, any>
}

export interface UpdateCustomerInput {
  name: string
  company?: string
  externalReference?: string
  metadata?: Record<string, any>
}

export interface PageResponse<T> {
  content: T[]
  totalElements: number
  totalPages: number
  number: number
  size: number
}

