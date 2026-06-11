import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import {
  getAnalysis,
  createAnalysis,
  listAnalyses,
  getStatistics,
  registerOutcome,
} from '../services/analysisService'
import type { AnalysisRequest, OutcomeRequest } from '../types/analysis'

// ─── Keys ──────────────────────────────────────────────────────────────────────
export const analysisKeys = {
  all: ['analyses'] as const,
  lists: () => [...analysisKeys.all, 'list'] as const,
  list: (params: object) => [...analysisKeys.lists(), params] as const,
  detail: (id: string) => [...analysisKeys.all, 'detail', id] as const,
  statistics: () => [...analysisKeys.all, 'statistics'] as const,
}

// ─── Get single analysis ───────────────────────────────────────────────────────
export function useAnalysis(id: string | undefined) {
  return useQuery({
    queryKey: analysisKeys.detail(id ?? ''),
    queryFn: () => getAnalysis(id!),
    enabled: !!id,
  })
}

// ─── List analyses ─────────────────────────────────────────────────────────────
export function useAnalysisList(params?: { page?: number; size?: number; riskLevel?: string }) {
  return useQuery({
    queryKey: analysisKeys.list(params ?? {}),
    queryFn: () => listAnalyses(params),
  })
}

// ─── Statistics ────────────────────────────────────────────────────────────────
export function useStatistics() {
  return useQuery({
    queryKey: analysisKeys.statistics(),
    queryFn: getStatistics,
    staleTime: 1000 * 60 * 5,
  })
}

// ─── Create analysis ───────────────────────────────────────────────────────────
export function useCreateAnalysis() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (req: AnalysisRequest) => createAnalysis(req),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: analysisKeys.lists() })
      qc.invalidateQueries({ queryKey: analysisKeys.statistics() })
    },
  })
}

// ─── Register outcome ──────────────────────────────────────────────────────────
export function useRegisterOutcome(analysisId: string) {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (req: OutcomeRequest) => registerOutcome(analysisId, req),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: analysisKeys.detail(analysisId) })
    },
  })
}
