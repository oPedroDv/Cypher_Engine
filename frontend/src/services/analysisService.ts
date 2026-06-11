import { apiClient } from '../lib/axios'
import type {
  AnalysisRequest,
  AnalysisResponse,
  OutcomeRequest,
  PaginatedResponse,
  StatisticsResponse,
} from '../types/analysis'
import { MOCK_STATISTICS, MOCK_ANALYSIS_LIST } from './mocks'

// ─── Create Analysis ───────────────────────────────────────────────────────────
export async function createAnalysis(req: AnalysisRequest): Promise<AnalysisResponse> {
  const { data } = await apiClient.post<AnalysisResponse>('/api/v1/analyses', req)
  return data
}

// ─── Get Analysis by ID ────────────────────────────────────────────────────────
export async function getAnalysis(id: string): Promise<AnalysisResponse> {
  const { data } = await apiClient.get<AnalysisResponse>(`/api/v1/analyses/${id}`)
  return data
}

// ─── Register Outcome ──────────────────────────────────────────────────────────
export async function registerOutcome(analysisId: string, req: OutcomeRequest): Promise<void> {
  await apiClient.post(`/api/v1/analyses/${analysisId}/outcome`, req)
}

// ─── Statistics — endpoint real ───────────────────────────────────────────────
export async function getStatistics(): Promise<StatisticsResponse> {
  try {
    const { data } = await apiClient.get<StatisticsResponse>('/api/v1/statistics')
    return data
  } catch {
    return MOCK_STATISTICS
  }
}

// ─── List Analyses — endpoint real ────────────────────────────────────────────
export async function listAnalyses(params?: {
  page?: number
  size?: number
  riskLevel?: string
}): Promise<PaginatedResponse<AnalysisResponse>> {
  try {
    const { data } = await apiClient.get<PaginatedResponse<AnalysisResponse>>('/api/v1/analyses', { params })
    return data
  } catch {
    // Fallback: retorna dados mock se o backend ainda não estiver disponível
    const list = MOCK_ANALYSIS_LIST
    const page = params?.page ?? 0
    const size = params?.size ?? 10
    const start = page * size
    return {
      content: list.slice(start, start + size),
      totalElements: list.length,
      totalPages: Math.ceil(list.length / size),
      number: page,
      page,
      size,
    }
  }
}
