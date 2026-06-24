export function cn(...classes: (string | false | undefined | null)[]): string {
  return classes.filter(Boolean).join(' ')
}

export function formatScore(score: number): string {
  return Math.round(score * 100).toString()
}

export function parseCurrency(value: string): number {
  const raw = value.trim().replace(/^R\$\s*/, '').replace(/\s/g, '')
  if (!raw || !/^\d+(?:[.,]\d+)*$/.test(raw)) {
    throw new Error('Informe um valor de antecipação válido.')
  }

  let normalized: string
  if (raw.includes(',')) {
    const parts = raw.split(',')
    if (parts.length !== 2 || parts[1].length > 2 || !/^\d{1,3}(?:\.\d{3})*$|^\d+$/.test(parts[0])) {
      throw new Error('Informe o valor de antecipação com no máximo duas casas decimais.')
    }
    normalized = `${parts[0].replace(/\./g, '')}.${parts[1]}`
  } else {
    const parts = raw.split('.')
    if (parts.length === 1) {
      normalized = raw
    } else if (parts.length === 2 && parts[1].length <= 2) {
      normalized = raw
    } else if (parts.slice(1).every((part) => part.length === 3)) {
      normalized = parts.join('')
    } else {
      throw new Error('Informe o valor de antecipação com no máximo duas casas decimais.')
    }
  }

  const parsed = Number(normalized)
  if (!Number.isFinite(parsed)) {
    throw new Error('Informe um valor de antecipação válido.')
  }
  return parsed
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
