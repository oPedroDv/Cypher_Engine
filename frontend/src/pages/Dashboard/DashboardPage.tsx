import { useNavigate } from 'react-router-dom'
import { BarChart3, AlertTriangle, Activity, TrendingUp, ArrowRight, RefreshCw } from 'lucide-react'
import { useStatistics } from '../../hooks/useAnalysis'
import { useHealth } from '../../hooks/useHealth'
import { RiskDistributionChart } from '../../components/charts/RiskDistributionChart'
import { AnalysisTimelineChart } from '../../components/charts/AnalysisTimelineChart'
import { RiskBadge } from '../../components/ui/RiskBadge'
import { formatScore, formatDate } from '../../lib/utils'
import { useAnalysisList } from '../../hooks/useAnalysis'

function StatCard({ label, value, sub, icon: Icon, color = '#00d4ff' }: {
  label: string; value: string | number; sub?: string; icon: React.ElementType; color?: string
}) {
  return (
    <div className="stat-card group">
      <div className="flex items-start justify-between">
        <p className="stat-label">{label}</p>
        <span className="p-2 rounded-lg bg-bg-elevated transition-colors group-hover:bg-bg-border">
          <Icon className="w-4 h-4" style={{ color }} />
        </span>
      </div>
      <p className="stat-value">{value}</p>
      {sub && <p className="stat-sub">{sub}</p>}
    </div>
  )
}

export default function DashboardPage() {
  const navigate = useNavigate()
  const { data: stats, isLoading: loadingStats, refetch } = useStatistics()
  const { data: health } = useHealth()
  const { data: listData, isLoading: loadingList } = useAnalysisList({ page: 0, size: 5 })

  const isOnline = health?.status === 'UP'

  if (loadingStats) {
    return (
      <div className="flex items-center justify-center h-64">
        <div className="flex flex-col items-center gap-3">
          <div className="w-8 h-8 border-2 border-cyan/30 border-t-cyan rounded-full animate-spin" />
          <p className="text-sm text-gray-500">Carregando dashboard…</p>
        </div>
      </div>
    )
  }

  return (
    <div className="space-y-6 animate-in">
      {/* Header */}
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold text-white">Dashboard</h1>
          <p className="text-sm text-gray-500 mt-1">Visão geral do motor de risco</p>
        </div>
        <div className="flex items-center gap-3">
          <span className={`badge ${isOnline ? 'badge-online' : 'badge-offline'}`}>
            <span className={`w-1.5 h-1.5 rounded-full ${isOnline ? 'bg-risk-low animate-pulse' : 'bg-risk-high'}`} />
            API {isOnline ? 'Online' : 'Offline'}
          </span>
          <button onClick={() => refetch()} className="btn-ghost">
            <RefreshCw className="w-4 h-4" />
          </button>
          <button onClick={() => navigate('/nova-analise')} className="btn-primary">
            <BarChart3 className="w-4 h-4" />
            Nova Análise
          </button>
        </div>
      </div>

      {/* Stat Cards */}
      <div className="grid grid-cols-2 lg:grid-cols-4 gap-4">
        <StatCard
          label="Total de Análises"
          value={(stats?.totalAnalyses ?? 0).toLocaleString('pt-BR')}
          sub="últimos 30 dias"
          icon={BarChart3}
          color="#00d4ff"
        />
        <StatCard
          label="Riscos Encontrados"
          value={(stats?.totalRisksFound ?? 0).toLocaleString('pt-BR')}
          sub={`${stats ? ((stats.totalRisksFound / stats.totalAnalyses) * 100).toFixed(1) : 0}% do total`}
          icon={AlertTriangle}
          color="#ef4444"
        />
        <StatCard
          label="Score Médio"
          value={stats ? `${formatScore(stats.avgScore)}/100` : '—'}
          sub="média ponderada"
          icon={TrendingUp}
          color="#f59e0b"
        />
        <StatCard
          label="API Status"
          value={isOnline ? 'Online' : 'Offline'}
          sub={isOnline ? 'Todos sistemas operacionais' : 'Verificar conexão'}
          icon={Activity}
          color={isOnline ? '#10b981' : '#ef4444'}
        />
      </div>

      {/* Charts row */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-4">
        {/* Timeline chart */}
        <div className="card lg:col-span-2">
          <div className="flex items-center justify-between mb-4">
            <h2 className="text-sm font-semibold text-white">Análises nos Últimos 30 Dias</h2>
          </div>
          {stats?.analysesLast30Days ? (
            <AnalysisTimelineChart data={stats.analysesLast30Days} />
          ) : (
            <div className="h-48 flex items-center justify-center text-gray-600 text-sm">Sem dados</div>
          )}
        </div>

        {/* Distribution chart */}
        <div className="card">
          <h2 className="text-sm font-semibold text-white mb-4">Distribuição por Risco</h2>
          {stats?.byRiskLevel ? (
            <RiskDistributionChart data={stats.byRiskLevel} />
          ) : (
            <div className="h-48 flex items-center justify-center text-gray-600 text-sm">Sem dados</div>
          )}
        </div>
      </div>

      {/* Recent analyses table */}
      <div className="card">
        <div className="flex items-center justify-between mb-4">
          <h2 className="text-sm font-semibold text-white">Análises Recentes</h2>
          <button onClick={() => navigate('/historico')} className="btn-ghost text-xs">
            Ver todas <ArrowRight className="w-3 h-3" />
          </button>
        </div>

        {loadingList ? (
          <div className="flex justify-center py-8">
            <div className="w-6 h-6 border-2 border-cyan/30 border-t-cyan rounded-full animate-spin" />
          </div>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full">
              <thead>
                <tr className="border-b border-bg-border">
                  <th className="table-header">ID da Análise</th>
                  <th className="table-header">Score</th>
                  <th className="table-header">Nível de Risco</th>
                  <th className="table-header">Data</th>
                  <th className="table-header">Dados Parciais</th>
                  <th className="table-header"></th>
                </tr>
              </thead>
              <tbody>
                {listData?.content.map((a) => (
                  <tr key={a.analysisId} className="table-row">
                    <td className="table-cell font-mono text-xs text-gray-500">{a.analysisId.slice(0, 8)}…</td>
                    <td className="table-cell font-bold text-white">{formatScore(a.score)}</td>
                    <td className="table-cell"><RiskBadge level={a.riskLevel} /></td>
                    <td className="table-cell text-gray-500">{formatDate(a.createdAt)}</td>
                    <td className="table-cell">
                      {a.dataIsPartial
                        ? <span className="badge badge-medium">Parcial</span>
                        : <span className="badge badge-low">Completo</span>}
                    </td>
                    <td className="table-cell">
                      <button
                        onClick={() => navigate(`/analise/${a.analysisId}`)}
                        className="btn-ghost text-xs"
                      >
                        Ver <ArrowRight className="w-3 h-3" />
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>
    </div>
  )
}
