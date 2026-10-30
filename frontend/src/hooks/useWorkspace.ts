import { useState } from 'react'
import type { Workspace } from '@/types'

export function useWorkspace() {
  const [currentWorkspace, setCurrentWorkspace] = useState<Workspace | null>({
    id: 'default-ws-001',
    name: 'Primary Workspace',
    slug: 'primary',
    ownerUserId: 'usr-001',
    role: 'OWNER',
    createdAt: new Date().toISOString(),
  })

  return {
    currentWorkspace,
    setCurrentWorkspace,
    workspaces: currentWorkspace ? [currentWorkspace] : [],
  }
}

