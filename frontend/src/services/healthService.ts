import { apiClient } from '../lib/axios'
import type { HealthResponse } from '../types/analysis'

export async function getHealth(): Promise<HealthResponse> {
  try {
    const { data } = await apiClient.get<HealthResponse>('/actuator/health')
    return data
  } catch {
    try {
      const { data } = await apiClient.get<HealthResponse>('/api/health')
      return data
    } catch {
      return { status: 'DOWN' }
    }
  }
}

export async function measureLatency(): Promise<number> {
  const start = performance.now()
  try {
    await apiClient.get('/actuator/health')
    return Math.round(performance.now() - start)
  } catch {
    return -1
  }
}
