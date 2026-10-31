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

