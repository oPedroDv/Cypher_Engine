
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


export interface AnalysisRequest {
  xmlBase64: string
  idempotencyKey?: string
  requestedAdvanceValue?: number
  requestedMonthlyRate?: number
}


export interface AnalysisResponse {
  analysisId: string
  invoiceId: string
  idempotent: boolean
  score: number
  riskLevel: RiskLevel
  recommendation: string
  modelVersion: string
  dataIsPartial: boolean
  scoreAdjustments: ScoreAdjustmentDto[]
  factors: FactorDto[]
  financial: FinancialDto | null
  createdAt: string
}


export type OutcomeType = 'PAID' | 'PARTIAL' | 'DEFAULT' | 'CANCELLED'

export interface OutcomeRequest {
  outcome: OutcomeType
  eventDate: string
  amountReceived?: number
  daysLate?: number
  notes?: string
}


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


export interface PaginatedResponse<T> {
  content: T[]
  totalElements: number
  totalPages: number

  number: number
  size: number
}


export type HealthStatus = 'UP' | 'DOWN' | 'UNKNOWN'

export interface HealthResponse {
  status: HealthStatus
  components?: Record<string, { status: HealthStatus; details?: Record<string, unknown> }>
}


export interface CompanyResponse {
  id: string
  cnpj: string
  legalName: string
  tradeName: string | null
  status: string
  fit: boolean
  criticalStatus: boolean
  statusUpdatedAt: string
  createdAt: string
}


export interface ApiError {
  message: string
  status?: number
  errorCode?: string
  existingAnalysisId?: string
  errors?: string[]
}
