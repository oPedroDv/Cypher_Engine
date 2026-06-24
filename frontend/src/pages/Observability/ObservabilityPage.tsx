import { useState } from 'react'
import { Activity, Server, Clock, Wifi, WifiOff, RefreshCw, CheckCircle2, XCircle } from 'lucide-react'
import { useHealth, useLatency } from '../../hooks/useHealth'
import { cn } from '../../lib/utils'

const ENDPOINTS = [
  { label: 'POST /api/v1/analyses',       desc: 'Criar análise de risco' },
  { label: 'GET /api/v1/analyses/{id}',   desc: 'Buscar análise por ID' },
  { label: 'POST /api/v1/analyses/{id}/outcome', desc: 'Registrar decisão' },
  { label: 'GET /api/v1/companies/{cnpj}', desc: 'Perfil de empresa' },
  { label: 'GET /actuator/health',         desc: 'Health check' },
]

function StatusRow({ label, online }: { label: string; online: boolean }) {
  return (
    <div className="flex items-center gap-3 py-2.5 border-b border-bg-border last:border-0">
      {online
        ? <CheckCircle2 className="w-4 h-4 text-risk-low shrink-0" />
        : <XCircle className="w-4 h-4 text-risk-high shrink-0" />}
      <div className="flex-1 min-w-0">
        <p className="text-xs font-mono text-gray-300 truncate">{label}</p>
      </div>
      <span className={cn('badge text-[10px]', online ? 'badge-low' : 'badge-high')}>
        {online ? 'OK' : 'ERRO'}
      </span>
    </div>
  )
}

export default function ObservabilityPage() {
  const { data: health, isLoading, refetch, dataUpdatedAt } = useHealth()
  const { data: latency } = useLatency()
  const [isRefreshing, setIsRefreshing] = useState(false)

  const isOnline = health?.status === 'UP'

  const handleRefresh = async () => {
    setIsRefreshing(true)
    await refetch()
    setIsRefreshing(false)
  }

  const components = health?.components ?? {}

  const lastChecked = dataUpdatedAt
    ? new Date(dataUpdatedAt).toLocaleTimeString('pt-BR')
    : '—'

  return (
    <div className="space-y-6 animate-in">
      {/* Header */}
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold text-white">Observabilidade</h1>
          <p className="text-sm text-gray-500 mt-1">Monitoramento em tempo real da API</p>
        </div>
        <button onClick={handleRefresh} disabled={isRefreshing} className="btn-secondary">
          <RefreshCw className={cn('w-4 h-4', isRefreshing && 'animate-spin')} />
          Atualizar
        </button>
      </div>

      {/* Status Cards */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
        {/* API Status */}
        <div className={cn(
          'card border-2 flex flex-col gap-3',
          isLoading ? 'border-bg-border' : isOnline ? 'border-risk-low/30' : 'border-risk-high/30'
        )}>
          <div className="flex items-center gap-2">
            {isOnline
              ? <Wifi className="w-4 h-4 text-risk-low" />
              : <WifiOff className="w-4 h-4 text-risk-high" />}
            <span className="stat-label">API Status</span>
          </div>
          <div className="flex items-end gap-2">
            <span className={cn('text-2xl font-bold', isOnline ? 'text-risk-low' : 'text-risk-high')}>
              {isLoading ? '…' : isOnline ? 'Online' : 'Offline'}
            </span>
          </div>
          <p className="stat-sub">Atualizado: {lastChecked}</p>
        </div>

        {/* Latency */}
        <div className="card flex flex-col gap-3">
          <div className="flex items-center gap-2">
            <Clock className="w-4 h-4 text-cyan" />
            <span className="stat-label">Latência</span>
          </div>
          <span className="text-2xl font-bold text-white">
            {latency === undefined ? '…' : latency < 0 ? '—' : `${latency}ms`}
          </span>
          <p className="stat-sub">
            {latency !== undefined && latency > 0
              ? latency < 200 ? '✅ Excelente' : latency < 500 ? '⚠️ Aceitável' : '🔴 Lento'
              : 'API indisponível'}
          </p>
        </div>

        {/* Health Status */}
        <div className="card flex flex-col gap-3">
          <div className="flex items-center gap-2">
            <Activity className="w-4 h-4 text-cyan" />
            <span className="stat-label">Health Check</span>
          </div>
          <span className="text-2xl font-bold text-white">
            {health?.status ?? '—'}
          </span>
          <p className="stat-sub">/actuator/health</p>
        </div>

        {/* Components */}
        <div className="card flex flex-col gap-3">
          <div className="flex items-center gap-2">
            <Server className="w-4 h-4 text-cyan" />
            <span className="stat-label">Componentes</span>
          </div>
          <span className="text-2xl font-bold text-white">
            {Object.keys(components).length || '—'}
          </span>
          <p className="stat-sub">
            {Object.values(components).every(c => c.status === 'UP') ? 'Todos operacionais' : 'Verificar componentes'}
          </p>
        </div>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        {/* Spring Actuator Components */}
        <div className="card">
          <div className="flex items-center gap-2 mb-4">
            <Server className="w-4 h-4 text-cyan" />
            <h2 className="text-sm font-semibold text-white">Componentes da Aplicação</h2>
          </div>
          {Object.keys(components).length === 0 ? (
            <div className="py-6 text-center text-gray-600 text-sm">
              {isOnline
                ? 'Nenhum componente detalhado disponível'
                : 'API offline — não foi possível obter componentes'}
            </div>
          ) : (
            <div>
              {Object.entries(components).map(([name, comp]) => (
                <StatusRow key={name} label={name} online={comp.status === 'UP'} />
              ))}
            </div>
          )}
        </div>

        {/* Endpoint Matrix */}
        <div className="card">
          <div className="flex items-center gap-2 mb-4">
            <Activity className="w-4 h-4 text-cyan" />
            <h2 className="text-sm font-semibold text-white">Matriz de Endpoints</h2>
          </div>
          <div>
            {ENDPOINTS.map((ep) => (
              <div key={ep.label} className="flex items-start gap-3 py-2.5 border-b border-bg-border last:border-0">
                {isOnline
                  ? <CheckCircle2 className="w-4 h-4 text-risk-low shrink-0 mt-0.5" />
                  : <XCircle className="w-4 h-4 text-risk-high shrink-0 mt-0.5" />}
                <div className="flex-1 min-w-0">
                  <p className="text-xs font-mono text-cyan truncate">{ep.label}</p>
                  <p className="text-xs text-gray-500 mt-0.5">{ep.desc}</p>
                </div>
              </div>
            ))}
          </div>
        </div>
      </div>

      {/* Raw health response */}
      {health && (
        <div className="card">
          <h2 className="text-sm font-semibold text-white mb-3">Resposta Bruta — /actuator/health</h2>
          <pre className="text-xs font-mono text-gray-400 bg-bg-elevated rounded-lg p-4 overflow-x-auto leading-relaxed max-h-64 overflow-y-auto">
            {JSON.stringify(health, null, 2)}
          </pre>
        </div>
      )}
    </div>
  )
}
