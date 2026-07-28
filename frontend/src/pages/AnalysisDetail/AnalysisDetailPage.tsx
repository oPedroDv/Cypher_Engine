import { useParams, useNavigate } from 'react-router-dom'
import {
  ArrowLeft, ShieldCheck, AlertTriangle, Copy, Check,
  TrendingUp, DollarSign, Clock, Database
} from 'lucide-react'
import { useState } from 'react'
import { useAnalysis, useRegisterOutcome } from '../../hooks/useAnalysis'
import { ScoreGauge } from '../../components/ui/ScoreGauge'
import { RiskBadge } from '../../components/ui/RiskBadge'
import { RiskBar } from '../../components/ui/RiskBar'
import { formatScore, formatCurrency, formatPct, formatDate } from '../../lib/utils'
import { cn } from '../../lib/utils'
import type { OutcomeType } from '../../types/analysis'

function errorMessage(error: unknown, fallback: string): string {
  if (error instanceof Error) return error.message
  if (typeof error === 'object' && error !== null && 'message' in error && typeof error.message === 'string') {
    return error.message
  }
  return fallback
}

function todayLocalDate(): string {
  const now = new Date()
  return new Date(now.getTime() - now.getTimezoneOffset() * 60_000).toISOString().slice(0, 10)
}

function CopyButton({ text }: { text: string }) {
  const [copied, setCopied] = useState(false)
  return (
    <button
      onClick={() => { navigator.clipboard.writeText(text); setCopied(true); setTimeout(() => setCopied(false), 2000) }}
      className="btn-ghost p-1"
    >
      {copied ? <Check className="w-3.5 h-3.5 text-risk-low" /> : <Copy className="w-3.5 h-3.5" />}
    </button>
  )
}

export default function AnalysisDetailPage() {
  const { id } = useParams<{ id: string }>()
  const navigate = useNavigate()
  const { data: analysis, isLoading, error } = useAnalysis(id)
  const { mutateAsync: registerOutcome, isPending: registering } = useRegisterOutcome(id ?? '')
  const [outcomeNote, setOutcomeNote] = useState('')
  const [outcomeDate, setOutcomeDate] = useState(todayLocalDate)
  const [outcomeSuccess, setOutcomeSuccess] = useState<OutcomeType | null>(null)
  const [outcomeError, setOutcomeError] = useState<string | null>(null)

  const handleOutcome = async (outcome: OutcomeType) => {
    setOutcomeError(null)
    try {
      await registerOutcome({ outcome, eventDate: outcomeDate, notes: outcomeNote || undefined })
      setOutcomeSuccess(outcome)
    } catch (error) {
      setOutcomeError(errorMessage(error, 'Não foi possível registrar o resultado. Tente novamente.'))
    }
  }

  if (isLoading) return (
    <div className="flex items-center justify-center h-64">
      <div className="w-8 h-8 border-2 border-cyan/30 border-t-cyan rounded-full animate-spin" />
    </div>
  )

  if (error || !analysis) return (
    <div className="card border-risk-high/30 text-center py-12">
      <AlertTriangle className="w-10 h-10 text-risk-high mx-auto mb-3" />
      <p className="text-white font-semibold">Análise não encontrada</p>
      <p className="text-gray-500 text-sm mt-1">{errorMessage(error, 'ID inválido ou não existe.')}</p>
      <button onClick={() => navigate(-1)} className="btn-secondary mt-4 mx-auto">Voltar</button>
    </div>
  )

  const riskLevelColor: Record<string, string> = {
    LOW: 'border-risk-low/30 bg-risk-low/5',
    MEDIUM: 'border-risk-medium/30 bg-risk-medium/5',
    HIGH: 'border-risk-high/30 bg-risk-high/5',
    CRITICAL: 'border-risk-critical/30 bg-risk-critical/5',
  }

  return (
    <div className="space-y-6 animate-in">

      <div className="flex items-start gap-4">
        <button onClick={() => navigate(-1)} className="btn-ghost mt-1">
          <ArrowLeft className="w-4 h-4" />
        </button>
        <div className="flex-1">
          <div className="flex items-center gap-3 flex-wrap">
            <h1 className="text-2xl font-bold text-white">Resultado da Análise</h1>
            {analysis.idempotent && <span className="badge badge-medium">Idempotente</span>}
            {analysis.dataIsPartial && <span className="badge badge-high">Dados Parciais</span>}
          </div>
          <p className="text-sm text-gray-500 mt-1 font-mono">{analysis.analysisId}</p>
          <p className="text-xs text-gray-600 mt-0.5">
            <Clock className="w-3 h-3 inline mr-1" />
            {formatDate(analysis.createdAt)} · Modelo {analysis.modelVersion}
          </p>
        </div>
        <CopyButton text={analysis.analysisId} />
      </div>


      <div className={cn('card border', riskLevelColor[analysis.riskLevel])}>
        <div className="flex flex-col lg:flex-row items-center gap-8">
          <div className="flex flex-col items-center gap-2 shrink-0">
            <ScoreGauge score={analysis.score} size={200} />
          </div>
          <div className="flex-1 space-y-4">
            <div className="flex items-center gap-3">
              <RiskBadge level={analysis.riskLevel} size="lg" />
              <span className="text-sm text-gray-500">Score: {formatScore(analysis.score)}/100</span>
            </div>
            <div className="p-4 bg-bg-elevated rounded-xl">
              <p className="text-xs text-gray-500 uppercase font-semibold tracking-wider mb-1">Recomendação</p>
              <p className="text-sm text-gray-200 leading-relaxed">{analysis.recommendation}</p>
            </div>
          </div>
        </div>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">

        {analysis.financial && (
          <div className="card">
            <div className="flex items-center gap-2 mb-4">
              <DollarSign className="w-4 h-4 text-cyan" />
              <h2 className="text-sm font-semibold text-white">Métricas Financeiras</h2>
              <span className={cn(
                'badge ml-auto',
                analysis.financial.isViable ? 'badge-low' : 'badge-high'
              )}>
                {analysis.financial.isViable ? 'Viável' : 'Inviável'}
              </span>
            </div>
            <div className="space-y-3">
              {[
                { label: 'Valor da Nota', value: formatCurrency(analysis.financial.faceValue) },
                { label: 'Valor de Antecipação', value: formatCurrency(analysis.financial.requestedAdvanceValue) },
                { label: 'Máx. Recomendado', value: formatCurrency(analysis.financial.maxAdvanceSuggested) },
                { label: 'Perda Esperada', value: formatPct(analysis.financial.expectedLossPct) },
                { label: 'ROI Ajustado ao Risco', value: formatPct(analysis.financial.riskAdjustedRoiPct) },
                { label: 'Taxa Mensal Sugerida', value: formatPct(analysis.financial.suggestedMonthlyRatePct) },
                { label: 'Razão de Antecipação', value: formatPct(analysis.financial.advanceRatio * 100) },
              ].map(({ label, value }) => (
                <div key={label} className="flex items-center justify-between py-2 border-b border-bg-border last:border-0">
                  <span className="text-xs text-gray-500">{label}</span>
                  <span className="text-sm font-semibold text-white">{value}</span>
                </div>
              ))}
            </div>
          </div>
        )}


        <div className="card">
          <div className="flex items-center gap-2 mb-4">
            <TrendingUp className="w-4 h-4 text-cyan" />
            <h2 className="text-sm font-semibold text-white">Fatores de Risco</h2>
            <span className="text-xs text-gray-600 ml-auto">{analysis.factors.length} fatores</span>
          </div>
          {analysis.factors.length === 0 ? (
            <p className="text-sm text-gray-600 text-center py-6">Nenhum fator registrado</p>
          ) : (
            <div className="space-y-4">
              {analysis.factors.map((f, i) => (
                <div key={i} className="space-y-1.5">
                  <div className="flex items-center justify-between">
                    <div className="flex items-center gap-2">
                      {f.isFallback && <span title="Dado de fallback"><Database className="w-3 h-3 text-gray-600" /></span>}
                      <span className="text-xs font-medium text-gray-300">{f.name}</span>
                      <span className="mono text-gray-600">{f.category}</span>
                    </div>
                    <span className="mono text-gray-500">{formatPct(f.contribution * 100)}</span>
                  </div>
                  <RiskBar value={f.score} direction={f.direction} label={f.name} />
                  <p className="text-xs text-gray-600 pl-1">{f.explanation}</p>
                </div>
              ))}
            </div>
          )}
        </div>
      </div>


      <div className="card">
        <div className="flex items-center gap-2 mb-4">
          <ShieldCheck className="w-4 h-4 text-cyan" />
          <h2 className="text-sm font-semibold text-white">Registrar Resultado da Operação</h2>
        </div>
        {outcomeSuccess ? (
          <div className="flex items-center gap-3 text-risk-low">
            <Check className="w-5 h-5" />
            <p className="text-sm font-semibold">
              Resultado registrado: {{ PAID: 'PAGO', PARTIAL: 'PAGO PARCIALMENTE', DEFAULT: 'INADIMPLENTE', CANCELLED: 'CANCELADO' }[outcomeSuccess]}
            </p>
          </div>
        ) : (
          <div className="space-y-3">
            <div>
              <label className="input-label">Data do evento</label>
              <input
                type="date"
                className="input"
                max={todayLocalDate()}
                value={outcomeDate}
                onChange={(e) => setOutcomeDate(e.target.value)}
                required
              />
            </div>
            <div>
              <label className="input-label">Observações (opcional)</label>
              <textarea
                className="input min-h-[80px] resize-none"
                placeholder="Adicione notas sobre a decisão…"
                value={outcomeNote}
                onChange={(e) => setOutcomeNote(e.target.value)}
              />
            </div>
            {outcomeError && (
              <div role="alert" className="flex items-center gap-2 text-sm text-risk-high">
                <AlertTriangle className="w-4 h-4 shrink-0" /> {outcomeError}
              </div>
            )}
            <div className="flex gap-3">
              <button
                onClick={() => handleOutcome('PAID')}
                disabled={registering || !outcomeDate}
                className="btn-primary flex-1 justify-center"
              >
                <ShieldCheck className="w-4 h-4" /> Pago
              </button>
              <button
                onClick={() => handleOutcome('PARTIAL')}
                disabled={registering || !outcomeDate}
                className="btn-secondary flex-1 justify-center"
              >
                Pagamento Parcial
              </button>
              <button
                onClick={() => handleOutcome('DEFAULT')}
                disabled={registering || !outcomeDate}
                className="flex-1 justify-center inline-flex items-center gap-2 px-5 py-2.5 rounded-lg font-semibold text-sm
                           border border-risk-high/40 text-risk-high bg-risk-high/5 hover:bg-risk-high/10
                           transition-all duration-200 disabled:opacity-50"
              >
                <AlertTriangle className="w-4 h-4" /> Inadimplente
              </button>
              <button
                onClick={() => handleOutcome('CANCELLED')}
                disabled={registering || !outcomeDate}
                className="btn-secondary flex-1 justify-center"
              >
                Cancelado
              </button>
            </div>
          </div>
        )}
      </div>
    </div>
  )
}
