import type { RiskLevel } from '../types/analysis'

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
  if (score < 0.20) return 'LOW'
  if (score < 0.50) return 'MEDIUM'
  if (score < 0.75) return 'HIGH'
  return 'CRITICAL'
}
