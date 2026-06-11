import { type RiskLevel } from '../../types/analysis'
import { cn } from '../../lib/utils'

const CONFIG: Record<RiskLevel, { label: string; className: string; dot: string }> = {
  LOW:      { label: 'BAIXO',    className: 'badge-low',      dot: 'bg-risk-low' },
  MEDIUM:   { label: 'MÉDIO',    className: 'badge-medium',   dot: 'bg-risk-medium' },
  HIGH:     { label: 'ALTO',     className: 'badge-high',     dot: 'bg-risk-high' },
  CRITICAL: { label: 'CRÍTICO',  className: 'badge-critical', dot: 'bg-risk-critical' },
}

interface Props {
  level: RiskLevel
  size?: 'sm' | 'md' | 'lg'
  showDot?: boolean
}

export function RiskBadge({ level, size = 'md', showDot = true }: Props) {
  const cfg = CONFIG[level]
  return (
    <span className={cn(cfg.className, size === 'lg' && 'text-sm px-3 py-1')}>
      {showDot && <span className={cn('w-1.5 h-1.5 rounded-full inline-block', cfg.dot)} />}
      {cfg.label}
    </span>
  )
}

const RISK_COLORS: Record<RiskLevel, string> = {
  LOW: '#10b981',
  MEDIUM: '#f59e0b',
  HIGH: '#ef4444',
  CRITICAL: '#9333ea',
}

export function riskColor(level: RiskLevel): string {
  return RISK_COLORS[level]
}

export function scoreToLevel(score: number): RiskLevel {
  if (score < 0.30) return 'LOW'
  if (score < 0.60) return 'MEDIUM'
  if (score < 0.80) return 'HIGH'
  return 'CRITICAL'
}
