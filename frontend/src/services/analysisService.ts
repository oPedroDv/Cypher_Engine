import { apiClient } from '../lib/axios'
import type {
  AnalysisRequest,
  AnalysisResponse,
  OutcomeRequest,
  PaginatedResponse,
  StatisticsResponse,
} from '../types/analysis'


export async function createAnalysis(req: AnalysisRequest): Promise<AnalysisResponse> {
  const { data } = await apiClient.post<AnalysisResponse>('/api/v1/analyses', req)
  return data
}


export async function getAnalysis(id: string): Promise<AnalysisResponse> {
  const { data } = await apiClient.get<AnalysisResponse>(`/api/v1/analyses/${id}`)
  return data
}


export async function registerOutcome(analysisId: string, req: OutcomeRequest): Promise<void> {
  await apiClient.post(`/api/v1/analyses/${analysisId}/outcome`, req)
}


export async function getStatistics(): Promise<StatisticsResponse> {
  const { data } = await apiClient.get<StatisticsResponse>('/api/v1/statistics')
  return data
}


export async function listAnalyses(params?: {
  page?: number
  size?: number
  riskLevel?: string
}): Promise<PaginatedResponse<AnalysisResponse>> {
  const { data } = await apiClient.get<PaginatedResponse<AnalysisResponse>>('/api/v1/analyses', { params })
  return data
}
