import { useState, useCallback } from 'react'
import { useNavigate } from 'react-router-dom'
import { Upload, FileText, Zap, AlertCircle, CheckCircle2, X } from 'lucide-react'
import { useCreateAnalysis } from '../../hooks/useAnalysis'
import { generateIdempotencyKey, formatScore, parseCurrency } from '../../lib/utils'
import { RiskBadge } from '../../components/ui/RiskBadge'
import { ScoreGauge } from '../../components/ui/ScoreGauge'
import { cn } from '../../lib/utils'
import type { AnalysisResponse } from '../../types/analysis'

type Tab = 'upload' | 'paste'

function errorMessage(error: unknown, fallback: string): string {
  if (error instanceof Error) return error.message
  if (typeof error === 'object' && error !== null && 'message' in error && typeof error.message === 'string') {
    return error.message
  }
  return fallback
}

function existingAnalysisId(error: unknown): string | null {
  if (typeof error === 'object' && error !== null && 'existingAnalysisId' in error
      && typeof error.existingAnalysisId === 'string') {
    return error.existingAnalysisId
  }
  return null
}

export default function NewAnalysisPage() {
  const navigate = useNavigate()
  const { mutateAsync, isPending } = useCreateAnalysis()

  const [tab, setTab] = useState<Tab>('paste')
  const [xmlText, setXmlText] = useState('')
  const [fileName, setFileName] = useState('')
  const [advanceValue, setAdvanceValue] = useState('')
  const [monthlyRate, setMonthlyRate] = useState('')
  const [idempotencyKey, setIdempotencyKey] = useState(generateIdempotencyKey)
  const [result, setResult] = useState<AnalysisResponse | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [duplicateAnalysisId, setDuplicateAnalysisId] = useState<string | null>(null)
  const [dragOver, setDragOver] = useState(false)

  const handleFile = useCallback((file: File) => {
    if (!file.name.endsWith('.xml')) { setError('Arquivo deve ser .xml'); return }
    setFileName(file.name)
    const reader = new FileReader()
    reader.onload = (e) => setXmlText(e.target?.result as string ?? '')
    reader.readAsText(file)
  }, [])

  const handleDrop = useCallback((e: React.DragEvent) => {
    e.preventDefault(); setDragOver(false)
    const file = e.dataTransfer.files[0]
    if (file) handleFile(file)
  }, [handleFile])

  const handleSubmit = async () => {
    setError(null); setDuplicateAnalysisId(null); setResult(null)
    if (!xmlText.trim()) { setError('Informe o XML da NF-e.'); return }
    try {
      const xmlBase64 = btoa(unescape(encodeURIComponent(xmlText.trim())))
      const res = await mutateAsync({
        xmlBase64,
        idempotencyKey: idempotencyKey.trim() || undefined,
        requestedAdvanceValue: advanceValue ? parseCurrency(advanceValue) : undefined,
        requestedMonthlyRate: monthlyRate ? Number(monthlyRate) : undefined,
      })
      setResult(res)
    } catch (err: unknown) {
      setError(errorMessage(err, 'Erro ao processar análise.'))
      setDuplicateAnalysisId(existingAnalysisId(err))
    }
  }

  return (
    <div className="space-y-6 animate-in max-w-4xl">

      <div>
        <h1 className="text-2xl font-bold text-white">Nova Análise</h1>
        <p className="text-sm text-gray-500 mt-1">Envie uma NF-e para análise de risco financeiro</p>
      </div>


      <div className="flex gap-1 p-1 bg-bg-elevated rounded-xl w-fit">
        {(['paste', 'upload'] as Tab[]).map((t) => (
          <button
            key={t}
            onClick={() => setTab(t)}
            className={cn(
              'flex items-center gap-2 px-4 py-2 rounded-lg text-sm font-medium transition-all duration-200',
              tab === t ? 'bg-bg-surface text-white shadow-card' : 'text-gray-500 hover:text-gray-300'
            )}
          >
            {t === 'paste' ? <FileText className="w-4 h-4" /> : <Upload className="w-4 h-4" />}
            {t === 'paste' ? 'Colar XML' : 'Upload de Arquivo'}
          </button>
        ))}
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">

        <div className="lg:col-span-2 space-y-4">
          <div className="card">
            {tab === 'paste' ? (
              <div>
                <label className="input-label">XML da NF-e</label>
                <textarea
                  className="input min-h-[240px] font-mono text-xs resize-y"
                  placeholder={'<?xml version="1.0" encoding="UTF-8"?>\n<nfeProc xmlns="http://www.portalfiscal.inf.br/nfe">\n  ...\n</nfeProc>'}
                  value={xmlText}
                  onChange={(e) => setXmlText(e.target.value)}
                />
                <p className="text-xs text-gray-600 mt-1">{xmlText.length} caracteres</p>
              </div>
            ) : (
              <div
                onDragOver={(e) => { e.preventDefault(); setDragOver(true) }}
                onDragLeave={() => setDragOver(false)}
                onDrop={handleDrop}
                className={cn(
                  'border-2 border-dashed rounded-xl p-10 flex flex-col items-center justify-center gap-3 transition-all duration-200 cursor-pointer',
                  dragOver ? 'border-cyan bg-cyan/5' : 'border-bg-border hover:border-cyan/40'
                )}
                onClick={() => document.getElementById('xml-file-input')?.click()}
              >
                <input
                  id="xml-file-input"
                  type="file"
                  accept=".xml"
                  className="hidden"
                  onChange={(e) => { const f = e.target.files?.[0]; if (f) handleFile(f) }}
                />
                <Upload className={cn('w-10 h-10', dragOver ? 'text-cyan' : 'text-gray-600')} />
                <div className="text-center">
                  <p className="text-sm font-medium text-gray-300">
                    {fileName || 'Arraste o XML aqui ou clique para selecionar'}
                  </p>
                  <p className="text-xs text-gray-600 mt-1">Somente arquivos .xml</p>
                </div>
                {fileName && <span className="badge badge-low">{fileName}</span>}
              </div>
            )}
          </div>


          {error && (
            <div className="flex items-start gap-3 p-4 bg-risk-high/10 border border-risk-high/30 rounded-xl">
              <AlertCircle className="w-4 h-4 text-risk-high shrink-0 mt-0.5" />
              <p className="text-sm text-risk-high">{error}</p>
              {duplicateAnalysisId && (
                <button
                  onClick={() => navigate(`/analise/${duplicateAnalysisId}`)}
                  className="text-xs font-semibold text-cyan hover:underline whitespace-nowrap"
                >
                  Abrir análise existente
                </button>
              )}
              <button onClick={() => setError(null)} className="ml-auto">
                <X className="w-4 h-4 text-risk-high/60 hover:text-risk-high" />
              </button>
            </div>
          )}


          <button
            onClick={handleSubmit}
            disabled={isPending || !xmlText.trim()}
            className="btn-primary w-full justify-center py-3 text-base"
          >
            {isPending ? (
              <>
                <div className="w-4 h-4 border-2 border-bg-base/30 border-t-bg-base rounded-full animate-spin" />
                Analisando…
              </>
            ) : (
              <>
                <Zap className="w-5 h-5" />
                Analisar NF-e
              </>
            )}
          </button>
        </div>


        <div className="space-y-4">

          <div className="card space-y-4">
            <h3 className="text-sm font-semibold text-white">Parâmetros Financeiros</h3>
            <div>
              <label className="input-label">Valor de Antecipação (R$)</label>
              <input
                type="text"
                inputMode="decimal"
                className="input"
                placeholder="100.000,00"
                value={advanceValue}
                onChange={(e) => setAdvanceValue(e.target.value)}
              />
            </div>
            <div>
              <label className="input-label">Taxa Mensal (%)</label>
              <input
                type="number"
                step="0.01"
                className="input"
                placeholder="1.80"
                value={monthlyRate}
                onChange={(e) => setMonthlyRate(e.target.value)}
              />
            </div>
            <div>
              <label className="input-label">Chave de Idempotência</label>
              <div className="flex gap-2">
                <input
                  type="text"
                  className="input flex-1 font-mono text-xs"
                  value={idempotencyKey}
                  onChange={(e) => setIdempotencyKey(e.target.value)}
                />
                <button
                  onClick={() => setIdempotencyKey(generateIdempotencyKey())}
                  className="btn-secondary px-3"
                  title="Gerar nova chave"
                >
                  <Zap className="w-3.5 h-3.5" />
                </button>
              </div>
            </div>
          </div>


          {result && (
            <div className="card border-cyan/20 space-y-4">
              <div className="flex items-center gap-2">
                <CheckCircle2 className="w-4 h-4 text-risk-low" />
                <span className="text-sm font-semibold text-risk-low">Análise Concluída</span>
                {result.idempotent && <span className="badge badge-medium ml-auto">Idempotente</span>}
              </div>
              <div className="flex justify-center">
                <ScoreGauge score={result.score} size={140} />
              </div>
              <div className="flex items-center justify-between">
                <RiskBadge level={result.riskLevel} size="lg" />
                <span className="mono">Score: {formatScore(result.score)}/100</span>
              </div>
              <p className="text-xs text-gray-400 bg-bg-elevated rounded-lg px-3 py-2 leading-relaxed">
                {result.recommendation}
              </p>
              <button
                onClick={() => navigate(`/analise/${result.analysisId}`)}
                className="btn-primary w-full justify-center"
              >
                Ver Análise Completa
              </button>
            </div>
          )}
        </div>
      </div>
    </div>
  )
}
