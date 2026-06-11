import { cn } from '../../lib/utils'

interface Props {
  value: number        // –1 to +1
  direction: string
  label: string
  className?: string
}

export function RiskBar({ value, direction, className }: Props) {
  const isPositive = direction === 'DECREASE'
  const pct = Math.abs(value) * 100
  const color = isPositive ? '#10b981' : direction === 'NEUTRAL' ? '#6b7280' : '#ef4444'

  return (
    <div className={cn('flex items-center gap-3', className)}>
      <div className="flex-1 bg-bg-border rounded-full h-1.5 overflow-hidden">
        <div
          className="h-full rounded-full transition-all duration-700"
          style={{ width: `${Math.min(pct, 100)}%`, backgroundColor: color, boxShadow: `0 0 6px ${color}60` }}
        />
      </div>
      <span className="text-xs font-semibold w-16 text-right" style={{ color }}>
        {isPositive ? 'POSITIVO' : direction === 'NEUTRAL' ? 'NEUTRO' : 'NEGATIVO'}
      </span>
    </div>
  )
}
