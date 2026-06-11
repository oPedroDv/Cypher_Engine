export function cn(...classes: (string | false | undefined | null)[]): string {
  return classes.filter(Boolean).join(' ')
}

export function formatScore(score: number): string {
  return Math.round(score * 100).toString()
}

export function parseNumber(value: string): number {
  return Number(value.replace(/\./g, '').replace(',', '.'))
}

export function formatCurrency(value: number): string {
  return new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' }).format(value)
}

export function formatPct(value: number): string {
  return `${value.toFixed(2)}%`
}

export function formatDate(iso: string): string {
  return new Intl.DateTimeFormat('pt-BR', {
    day: '2-digit', month: '2-digit', year: 'numeric',
    hour: '2-digit', minute: '2-digit',
  }).format(new Date(iso))
}

export function formatDateShort(iso: string): string {
  return new Intl.DateTimeFormat('pt-BR', { day: '2-digit', month: '2-digit', year: 'numeric' }).format(new Date(iso))
}

export function truncate(str: string, n = 20): string {
  return str.length > n ? str.slice(0, n) + '...' : str
}

export function generateIdempotencyKey(): string {
  return crypto.randomUUID()
}
