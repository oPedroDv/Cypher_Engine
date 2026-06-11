import { NavLink, useLocation } from 'react-router-dom'
import {
  LayoutDashboard,
  FilePlus2,
  History,
  Activity,
  ChevronRight,
  Zap,
} from 'lucide-react'
import { cn } from '../../lib/utils'
import { useHealth } from '../../hooks/useHealth'

const NAV = [
  { to: '/',              label: 'Dashboard',     icon: LayoutDashboard },
  { to: '/nova-analise',  label: 'Nova Análise',  icon: FilePlus2 },
  { to: '/historico',     label: 'Histórico',     icon: History },
  { to: '/observabilidade', label: 'Observabilidade', icon: Activity },
]

export function Sidebar() {
  const { data: health } = useHealth()
  const isOnline = health?.status === 'UP'
  const location = useLocation()

  return (
    <aside className="fixed left-0 top-0 h-full w-60 flex flex-col bg-bg-surface border-r border-bg-border z-30">
      {/* Logo */}
      <div className="flex items-center gap-3 px-5 py-5 border-b border-bg-border">
        <div className="w-8 h-8 rounded-lg bg-cypher-gradient flex items-center justify-center shadow-glow">
          <Zap className="w-4 h-4 text-bg-base" strokeWidth={2.5} />
        </div>
        <div>
          <span className="text-sm font-bold text-white tracking-wide">CYPHER</span>
          <p className="text-[10px] text-gray-500 leading-none mt-0.5">Risk Engine v1</p>
        </div>
      </div>

      {/* Nav */}
      <nav className="flex-1 px-3 py-4 space-y-1 overflow-y-auto">
        <p className="text-[10px] font-semibold text-gray-600 uppercase tracking-widest px-3 mb-3">
          Navegação
        </p>
        {NAV.map(({ to, label, icon: Icon }) => {
          const active = to === '/'
            ? location.pathname === '/'
            : location.pathname.startsWith(to)
          return (
            <NavLink key={to} to={to} className={cn('sidebar-link', active && 'active')}>
              <Icon className="w-4 h-4 shrink-0" />
              <span>{label}</span>
              {active && <ChevronRight className="w-3 h-3 ml-auto text-cyan" />}
            </NavLink>
          )
        })}
      </nav>

      {/* API Status */}
      <div className="px-4 py-4 border-t border-bg-border">
        <div className="flex items-center gap-2 px-3 py-2.5 rounded-lg bg-bg-elevated">
          <span className={cn(
            'w-2 h-2 rounded-full shrink-0',
            isOnline ? 'bg-risk-low animate-pulse' : 'bg-risk-high'
          )} />
          <div className="min-w-0">
            <p className="text-xs font-semibold text-gray-300">API Status</p>
            <p className={cn('text-[10px]', isOnline ? 'text-risk-low' : 'text-risk-high')}>
              {health == null ? 'Verificando…' : isOnline ? 'Online' : 'Offline'}
            </p>
          </div>
        </div>
      </div>
    </aside>
  )
}
