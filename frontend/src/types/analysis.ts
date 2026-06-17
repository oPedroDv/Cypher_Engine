// ─── Risk Levels ───────────────────────────────────────────────────────────────
export type RiskLevel = 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL'

export interface FactorDto {
  name: string
  category: string
  score: number
  weight: number
  contribution: number
  direction: 'INCREASE' | 'DECREASE' | 'NEUTRAL'
  explanation: string
  dataSource: string
  isFallback: boolean
}

export interface FinancialDto {
  faceValue: number
  requestedAdvanceValue: number
  expectedLossPct: number
  riskAdjustedRoiPct: number
  maxAdvanceSuggested: number
  suggestedMonthlyRatePct: number
  advanceRatio: number
  isViable: boolean
}

export interface ScoreAdjustmentDto {
  type: string
  from: number
  to: number
  reason: string
}

// ─── Analysis Request ───────────────────────────────────────────────────────────
export interface AnalysisRequest {
  xmlBase64: string
  idempotencyKey?: string
  requestedAdvanceValue?: number
  requestedMonthlyRate?: number
}

// ─── Analysis Response ─────────────────────────────────────────────────────────
export interface AnalysisResponse {
  analysisId: string
  invoiceId: string
  idempotent: boolean
  score: number           // 0.0–1.0
  riskLevel: RiskLevel
  recommendation: string
  modelVersion: string
  dataIsPartial: boolean
  scoreAdjustments: ScoreAdjustmentDto[]
  factors: FactorDto[]
  financial: FinancialDto | null
  createdAt: string       // ISO 8601
}

// ─── Outcome ───────────────────────────────────────────────────────────────────
export type OutcomeType = 'APPROVED' | 'REJECTED' | 'MANUAL_REVIEW'

export interface OutcomeRequest {
  outcome: OutcomeType
  notes?: string
}

// ─── Statistics ────────────────────────────────────────────────────────
export interface StatisticsResponse {
  totalAnalyses: number
  totalRisksFound: number
  avgScore: number
  byRiskLevel: Record<RiskLevel, number>
  analysesLast30Days: DailyCount[]
}

export interface DailyCount {
  date: string
  count: number
}

// ─── Pagination — Spring Page JSON usa 'number' para página atual ───────────────────────
export interface PaginatedResponse<T> {
  content: T[]
  totalElements: number
  totalPages: number
  /** Spring serializa a página atual como 'number', não 'page' */
  number: number
  size: number
  /** Alias para compatibilidade com dados mock */
  page?: number
}

// ─── Health ────────────────────────────────────────────────────────────────────
export type HealthStatus = 'UP' | 'DOWN' | 'UNKNOWN'

export interface HealthResponse {
  status: HealthStatus
  components?: Record<string, { status: HealthStatus; details?: Record<string, unknown> }>
}

// ─── Company ───────────────────────────────────────────────────────────────────
export interface CompanyResponse {
  cnpj: string
  legalName: string
  tradeName?: string
  status: string
  state: string
  city: string
  openingDate?: string
  mainActivity?: string
}

// ─── UI helpers ────────────────────────────────────────────────────────────────
export interface ApiError {
  message: string
  status?: number
  timestamp?: string
}
