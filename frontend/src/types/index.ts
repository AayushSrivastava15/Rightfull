export type UserRole = 'OWNER' | 'ADMIN' | 'DEVELOPER' | 'VIEWER'

export interface User {
  id: string
  supabaseUserId: string
  email: string
  fullName?: string
  avatarUrl?: string
  createdAt: string
}

export interface Workspace {
  id: string
  name: string
  slug: string
  ownerUserId: string
  role?: UserRole
  createdAt: string
}

export interface Product {
  id: string
  workspaceId: string
  name: string
  slug: string
  description?: string
  ed25519PublicKey: string
  createdAt: string
}

export interface LicensePlan {
  id: string
  productId: string
  name: string
  code: string
  maxSeats: number
  offlineGracePeriodHours: number
  validationIntervalMinutes: number
  features: Record<string, unknown>
  createdAt: string
}

export type CustomerType = 'INDIVIDUAL' | 'ORGANIZATION'

export interface Customer {
  id: string
  workspaceId: string
  type: CustomerType
  name: string
  email: string
  companyName?: string
  createdAt: string
}

export type LicenseStatus = 'ACTIVE' | 'SUSPENDED' | 'REVOKED' | 'EXPIRED'

export interface License {
  id: string
  workspaceId: string
  productId: string
  planId: string
  customerId: string
  keyPrefix: string
  status: LicenseStatus
  maxSeats: number
  usedSeats: number
  expiresAt?: string
  lastValidatedAt?: string
  createdAt: string
}

export interface DeviceActivation {
  id: string
  licenseId: string
  deviceFingerprint: string
  deviceName: string
  platform: string
  ipAddress: string
  activatedAt: string
  lastHeartbeatAt: string
}

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
