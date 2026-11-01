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
  code?: string
  slug?: string
  description?: string
  status: 'ACTIVE' | 'ARCHIVED'
  createdAt: string
  updatedAt: string
}

export interface CreateProductInput {
  name: string
  slug: string
  description?: string
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

export type LicenseStatus = 'ACTIVE' | 'SUSPENDED' | 'REVOKED' | 'EXPIRED'
export type ActivationStatus = 'ACTIVE' | 'DEACTIVATED'

export interface License {
  id: string
  workspaceId: string
  productId: string
  customerId?: string
  planId?: string
  planVersion?: number
  licenseKeyPrefix: string
  licenseKeyMasked: string
  licenseKeyLastFour: string
  status: LicenseStatus
  startsAt: string
  expiresAt?: string
  isPerpetual: boolean
  maxActivations: number
  currentActivations: number
  seatLimit: number
  offlineGracePeriodSeconds: number
  revalidationIntervalSeconds: number
  entitlements: Record<string, any>
  signingKeyId: string
  externalReference?: string
  suspendedAt?: string
  suspendedReason?: string
  revokedAt?: string
  revokedReason?: string
  renewedAt?: string
  createdAt: string
  updatedAt: string
}

export interface IssueLicenseInput {
  customerId?: string
  planId?: string
  durationDays?: number
  maximumActivations?: number
  seatLimit?: number
  offlineGracePeriodSeconds?: number
  revalidationIntervalSeconds?: number
  entitlements?: Record<string, any>
  externalReference?: string
  idempotencyKey?: string
}

export interface IssueLicenseResponse extends License {
  licenseKey: string // Plaintext raw key - shown ONLY once on issuance
}

export interface LicenseSummary {
  totalLicenses: number
  activeLicenses: number
  expiredLicenses: number
  suspendedLicenses: number
  revokedLicenses: number
}

export interface LicenseActivation {
  id: string
  workspaceId: string
  licenseId: string
  productId: string
  deviceIdentifierHash: string
  deviceName?: string
  devicePlatform?: string
  appVersion?: string
  status: ActivationStatus
  activatedAt: string
  lastValidatedAt: string
  deactivatedAt?: string
  deactivatedBy?: string
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

