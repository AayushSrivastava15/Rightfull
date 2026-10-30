import { useQuery } from '@tanstack/react-query'
import { apiClient } from '@/services/apiClient'
import type { SystemHealth } from '@/types'

export function useHealth() {
  return useQuery<SystemHealth>({
    queryKey: ['system-health'],
    queryFn: apiClient.getHealth,
    retry: 2,
    staleTime: 30000,
  })
}
