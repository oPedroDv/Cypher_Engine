import { useQuery } from '@tanstack/react-query'
import { getHealth, measureLatency } from '../services/healthService'

export const healthKeys = {
  status: ['health', 'status'] as const,
  latency: ['health', 'latency'] as const,
}

export function useHealth() {
  return useQuery({
    queryKey: healthKeys.status,
    queryFn: getHealth,
    refetchInterval: 30_000,
    staleTime: 15_000,
  })
}

export function useLatency() {
  return useQuery({
    queryKey: healthKeys.latency,
    queryFn: measureLatency,
    refetchInterval: 60_000,
    staleTime: 30_000,
  })
}
