import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { Search, Filter, ArrowRight, Download, ChevronLeft, ChevronRight } from 'lucide-react'
import { useAnalysisList } from '../../hooks/useAnalysis'
import { RiskBadge } from '../../components/ui/RiskBadge'
import { formatScore, formatDate } from '../../lib/utils'
import type { RiskLevel } from '../../types/analysis'

const PAGE_SIZE = 10

const RISK_FILTERS: { label: string; value: RiskLevel | 'ALL' }[] = [
  { label: 'Todos', value: 'ALL' },
  { label: 'Baixo', value: 'LOW' },
  { label: 'Médio', value: 'MEDIUM' },
  { label: 'Alto', value: 'HIGH' },
  { label: 'Crítico', value: 'CRITICAL' },
]

export default function HistoryPage() {
  const navigate = useNavigate()
  const [page, setPage] = useState(0)
  const [search, setSearch] = useState('')
  const [riskFilter, setRiskFilter] = useState<RiskLevel | 'ALL'>('ALL')

  const { data, isLoading } = useAnalysisList({
    page,
    size: PAGE_SIZE,
    riskLevel: riskFilter === 'ALL' ? undefined : riskFilter,
  })

  const filtered = (data?.content ?? []).filter((a) => {
    const matchSearch =
      search === '' ||
      a.analysisId.toLowerCase().includes(search.toLowerCase()) ||
      a.invoiceId.toLowerCase().includes(search.toLowerCase())
    return matchSearch
  })

  const handleExportCsv = () => {
    if (!data?.content) return
    const rows = [
      ['ID Análise', 'Score', 'Nível de Risco', 'Data', 'Dados Parciais', 'Recomendação'],
      ...data.content.map((a) => [
        a.analysisId,
        formatScore(a.score),
        a.riskLevel,
        formatDate(a.createdAt),
        a.dataIsPartial ? 'Sim' : 'Não',
        a.recommendation,
      ]),
    ]
    const csv = rows.map((r) => r.map((c) => `"${c}"`).join(',')).join('\n')
    const blob = new Blob(['\uFEFF' + csv], { type: 'text/csv;charset=utf-8' })
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a'); a.href = url; a.download = 'historico-analises.csv'; a.click()
    URL.revokeObjectURL(url)
  }

  return (
    <div className="space-y-6 animate-in">
      {}
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold text-white">Histórico de Análises</h1>
          <p className="text-sm text-gray-500 mt-1">
            {data ? `${data.totalElements.toLocaleString('pt-BR')} análises no total` : 'Carregando…'}
          </p>
        </div>
        <button onClick={handleExportCsv} className="btn-secondary">
          <Download className="w-4 h-4" /> Exportar CSV
        </button>
      </div>

      {}
      <div className="card p-4 flex flex-col sm:flex-row gap-3">
        {}
        <div className="relative flex-1">
          <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-gray-500 pointer-events-none" />
          <input
            type="text"
            className="input pl-9"
            placeholder="Buscar por ID de análise ou NF-e…"
            value={search}
            onChange={(e) => { setSearch(e.target.value); setPage(0) }}
          />
        </div>

        {/* Risk filter */}
        <div className="flex items-center gap-1 p-1 bg-bg-elevated rounded-lg">
          <Filter className="w-3.5 h-3.5 text-gray-500 ml-1 shrink-0" />
          {RISK_FILTERS.map(({ label, value }) => (
            <button
              key={value}
              onClick={() => { setRiskFilter(value); setPage(0) }}
              className={
                riskFilter === value
                  ? 'px-3 py-1.5 rounded-md text-xs font-semibold bg-bg-surface text-white shadow'
                  : 'px-3 py-1.5 rounded-md text-xs font-medium text-gray-500 hover:text-gray-300'
              }
            >
              {label}
            </button>
          ))}
        </div>
      </div>

      {/* Table */}
      <div className="card p-0 overflow-hidden">
        <div className="overflow-x-auto">
          <table className="w-full">
            <thead>
              <tr className="border-b border-bg-border bg-bg-elevated/50">
                <th className="table-header">ID da Análise</th>
                <th className="table-header">Score</th>
                <th className="table-header">Nível de Risco</th>
                <th className="table-header">Data</th>
                <th className="table-header">Dados</th>
                <th className="table-header">Modelo</th>
                <th className="table-header"></th>
              </tr>
            </thead>
            <tbody>
              {isLoading ? (
                <tr>
                  <td colSpan={7} className="text-center py-12">
                    <div className="flex justify-center">
                      <div className="w-6 h-6 border-2 border-cyan/30 border-t-cyan rounded-full animate-spin" />
                    </div>
                  </td>
                </tr>
              ) : filtered.length === 0 ? (
                <tr>
                  <td colSpan={7} className="text-center py-12 text-gray-600 text-sm">
                    Nenhuma análise encontrada
                  </td>
                </tr>
              ) : (
                filtered.map((a) => (
                  <tr key={a.analysisId} className="table-row cursor-pointer" onClick={() => navigate(`/analise/${a.analysisId}`)}>
                    <td className="table-cell font-mono text-xs">
                      <span className="text-gray-500">{a.analysisId.slice(0, 8)}</span>
                      <span className="text-gray-700">…</span>
                    </td>
                    <td className="table-cell">
                      <span className="text-xl font-bold text-white">{formatScore(a.score)}</span>
                      <span className="text-gray-600 text-xs">/100</span>
                    </td>
                    <td className="table-cell"><RiskBadge level={a.riskLevel} /></td>
                    <td className="table-cell text-gray-400 text-xs">{formatDate(a.createdAt)}</td>
                    <td className="table-cell">
                      {a.dataIsPartial
                        ? <span className="badge badge-medium">Parcial</span>
                        : <span className="badge badge-low">Completo</span>}
                    </td>
                    <td className="table-cell mono">{a.modelVersion}</td>
                    <td className="table-cell">
                      <ArrowRight className="w-4 h-4 text-gray-600 hover:text-cyan transition-colors" />
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>

        {/* Pagination */}
        {data && data.totalPages > 1 && (
          <div className="flex items-center justify-between px-4 py-3 border-t border-bg-border">
            <p className="text-xs text-gray-500">
              Página {page + 1} de {data.totalPages} · {data.totalElements} resultados
            </p>
            <div className="flex gap-2">
              <button
                onClick={() => setPage(p => Math.max(0, p - 1))}
                disabled={page === 0}
                className="btn-ghost disabled:opacity-40"
              >
                <ChevronLeft className="w-4 h-4" />
              </button>
              {Array.from({ length: Math.min(data.totalPages, 5) }, (_, i) => {
                const p = Math.max(0, Math.min(data.totalPages - 5, page - 2)) + i
                return (
                  <button
                    key={p}
                    onClick={() => setPage(p)}
                    className={p === page
                      ? 'w-8 h-8 rounded-lg text-xs font-semibold bg-cyan/10 text-cyan border border-cyan/30'
                      : 'w-8 h-8 rounded-lg text-xs text-gray-500 hover:text-white hover:bg-bg-elevated'}
                  >
                    {p + 1}
                  </button>
                )
              })}
              <button
                onClick={() => setPage(p => Math.min(data.totalPages - 1, p + 1))}
                disabled={page >= data.totalPages - 1}
                className="btn-ghost disabled:opacity-40"
              >
                <ChevronRight className="w-4 h-4" />
              </button>
            </div>
          </div>
        )}
      </div>
    </div>
  )
}
