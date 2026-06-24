import {
  PieChart, Pie, Cell, ResponsiveContainer, Tooltip, Legend,
} from 'recharts'

interface Props {
  data: { LOW: number; MEDIUM: number; HIGH: number; CRITICAL: number }
}

const LEVELS = [
  { key: 'LOW',      label: 'Muito Baixo / Baixo', color: '#10b981' },
  { key: 'MEDIUM',   label: 'Médio',               color: '#f59e0b' },
  { key: 'HIGH',     label: 'Alto',                color: '#ef4444' },
  { key: 'CRITICAL', label: 'Crítico',             color: '#9333ea' },
] as const

interface TooltipPayload {
  name: string
  value: number
}

interface DistributionTooltipProps {
  active?: boolean
  payload?: TooltipPayload[]
}

const CustomTooltip = ({ active, payload }: DistributionTooltipProps) => {
  if (!active || !payload?.length) return null
  const { name, value } = payload[0]
  return (
    <div className="card-sm text-xs">
      <p className="text-white font-semibold">{name}</p>
      <p className="text-gray-400">{value} análises</p>
    </div>
  )
}

export function RiskDistributionChart({ data }: Props) {
  const chartData = LEVELS.map(l => ({ name: l.label, value: data[l.key], color: l.color }))

  return (
    <ResponsiveContainer width="100%" height={220}>
      <PieChart>
        <Pie
          data={chartData}
          cx="50%"
          cy="50%"
          innerRadius={60}
          outerRadius={90}
          paddingAngle={3}
          dataKey="value"
          stroke="none"
        >
          {chartData.map((entry) => (
            <Cell key={entry.name} fill={entry.color} style={{ filter: `drop-shadow(0 0 6px ${entry.color}60)` }} />
          ))}
        </Pie>
        <Tooltip content={<CustomTooltip />} />
        <Legend
          iconType="circle"
          iconSize={8}
          formatter={(value) => <span className="text-xs text-gray-400">{value}</span>}
        />
      </PieChart>
    </ResponsiveContainer>
  )
}
