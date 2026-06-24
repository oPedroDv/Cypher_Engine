import {
  AreaChart, Area, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer,
} from 'recharts'
import type { DailyCount } from '../../types/analysis'

interface Props { data: DailyCount[] }

interface TooltipPayload {
  value: number
}

interface TimelineTooltipProps {
  active?: boolean
  payload?: TooltipPayload[]
  label?: string
}

const CustomTooltip = ({ active, payload, label }: TimelineTooltipProps) => {
  if (!active || !payload?.length) return null
  return (
    <div className="card-sm text-xs">
      <p className="text-gray-400">{label}</p>
      <p className="text-white font-semibold">{payload[0].value} análises</p>
    </div>
  )
}

export function AnalysisTimelineChart({ data }: Props) {
  const formatted = data.map(d => ({
    date: d.date.slice(5),  // MM-DD
    count: d.count,
  }))

  return (
    <ResponsiveContainer width="100%" height={200}>
      <AreaChart data={formatted} margin={{ top: 5, right: 5, left: -20, bottom: 0 }}>
        <defs>
          <linearGradient id="areaGrad" x1="0" y1="0" x2="0" y2="1">
            <stop offset="5%"  stopColor="#00d4ff" stopOpacity={0.3} />
            <stop offset="95%" stopColor="#00d4ff" stopOpacity={0} />
          </linearGradient>
        </defs>
        <CartesianGrid strokeDasharray="3 3" stroke="#1a2740" />
        <XAxis
          dataKey="date"
          tick={{ fill: '#4b5563', fontSize: 10 }}
          axisLine={{ stroke: '#1a2740' }}
          tickLine={false}
          interval={6}
        />
        <YAxis
          tick={{ fill: '#4b5563', fontSize: 10 }}
          axisLine={false}
          tickLine={false}
        />
        <Tooltip content={<CustomTooltip />} />
        <Area
          type="monotone"
          dataKey="count"
          stroke="#00d4ff"
          strokeWidth={2}
          fill="url(#areaGrad)"
          dot={false}
          activeDot={{ r: 4, fill: '#00d4ff', strokeWidth: 0 }}
        />
      </AreaChart>
    </ResponsiveContainer>
  )
}
