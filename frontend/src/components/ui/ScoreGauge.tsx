import { useEffect, useRef, useState } from 'react'
import { riskColor, scoreToLevel } from '../../lib/risk'

interface Props {
  score: number   // 0.0–1.0
  size?: number
  animate?: boolean
}

export function ScoreGauge({ score, size = 180, animate = true }: Props) {
  const [displayed, setDisplayed] = useState(animate ? 0 : score)
  const animRef = useRef<number | null>(null)

  useEffect(() => {
    if (!animate) { setDisplayed(score); return }
    const start = performance.now()
    const duration = 1200
    const from = 0
    const to = score
    const tick = (now: number) => {
      const t = Math.min((now - start) / duration, 1)
      const eased = 1 - Math.pow(1 - t, 3)
      setDisplayed(from + (to - from) * eased)
      if (t < 1) animRef.current = requestAnimationFrame(tick)
    }
    animRef.current = requestAnimationFrame(tick)
    return () => { if (animRef.current) cancelAnimationFrame(animRef.current) }
  }, [score, animate])

  const displayScore = Math.round(displayed * 100)
  const level = scoreToLevel(score)
  const color = riskColor(level)

  // SVG arc params
  const r = (size / 2) * 0.78
  const cx = size / 2
  const cy = size / 2
  const startAngle = -210  // degrees
  const totalArc = 240     // degrees
  const angle = startAngle + totalArc * displayed

  const toRad = (deg: number) => (deg * Math.PI) / 180
  const px = (deg: number) => cx + r * Math.cos(toRad(deg))
  const py = (deg: number) => cy + r * Math.sin(toRad(deg))

  const arcPath = (from: number, to: number) => {
    const large = to - from > 180 ? 1 : 0
    return `M ${px(from)} ${py(from)} A ${r} ${r} 0 ${large} 1 ${px(to)} ${py(to)}`
  }

  const labelMap: Record<string, string> = { LOW: 'BAIXO', MEDIUM: 'MÉDIO', HIGH: 'ALTO', CRITICAL: 'CRÍTICO' }

  return (
    <div className="flex flex-col items-center gap-2">
      <svg width={size} height={size} viewBox={`0 0 ${size} ${size}`}>
        {/* Track */}
        <path
          d={arcPath(startAngle, startAngle + totalArc)}
          fill="none"
          stroke="#1a2740"
          strokeWidth={size * 0.075}
          strokeLinecap="round"
        />
        {/* Fill */}
        {displayed > 0 && (
          <path
            d={arcPath(startAngle, angle)}
            fill="none"
            stroke={color}
            strokeWidth={size * 0.075}
            strokeLinecap="round"
            style={{ filter: `drop-shadow(0 0 8px ${color}80)` }}
          />
        )}
        {/* Center text */}
        <text x={cx} y={cy - size * 0.04} textAnchor="middle" fill="white" fontSize={size * 0.24} fontWeight="800" fontFamily="Inter">
          {displayScore}
        </text>
        <text x={cx} y={cy + size * 0.14} textAnchor="middle" fill="#6b7280" fontSize={size * 0.07} fontFamily="Inter">
          de 100
        </text>
      </svg>
      <span className="text-sm font-bold tracking-widest" style={{ color }}>
        RISCO {labelMap[level]}
      </span>
    </div>
  )
}
