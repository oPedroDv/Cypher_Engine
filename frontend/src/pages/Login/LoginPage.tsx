import { useState } from 'react'
import { KeyRound, ShieldCheck, Zap } from 'lucide-react'
import { useAuth } from '../../contexts/authContext'
import { cn } from '../../lib/utils'

type Mode = 'bearer' | 'apiKey'

export default function LoginPage() {
  const { expired, signInWithToken, signInWithApiKey } = useAuth()
  const [mode, setMode] = useState<Mode>('bearer')
  const [value, setValue] = useState('')
  const [error, setError] = useState<string | null>(null)

  const handleSubmit = (event: React.FormEvent) => {
    event.preventDefault()
    const credential = value.trim()
    if (!credential) {
      setError(mode === 'bearer' ? 'Informe o token JWT.' : 'Informe a API key.')
      return
    }
    setError(null)
    if (mode === 'bearer') signInWithToken(credential)
    else signInWithApiKey(credential)
  }

  return (
    <div className="min-h-screen flex items-center justify-center bg-bg-base px-6">
      <form onSubmit={handleSubmit} className="card w-full max-w-md space-y-5">
        <div className="flex items-center gap-3">
          <div className="w-9 h-9 rounded-lg bg-cypher-gradient flex items-center justify-center shadow-glow">
            <Zap className="w-4 h-4 text-bg-base" strokeWidth={2.5} />
          </div>
          <div>
            <h1 className="text-lg font-bold text-white tracking-wide">CYPHER</h1>
            <p className="text-xs text-gray-500">Autentique-se para acessar o motor de risco</p>
          </div>
        </div>

        {expired && (
          <p className="text-sm text-risk-high">
            Sua sessão expirou ou a credencial foi recusada. Autentique-se novamente.
          </p>
        )}

        <div className="flex gap-2">
          <button
            type="button"
            onClick={() => { setMode('bearer'); setError(null) }}
            className={cn('btn-ghost flex-1 justify-center', mode === 'bearer' && 'text-white bg-bg-elevated')}
          >
            <ShieldCheck className="w-4 h-4" /> Token JWT
          </button>
          <button
            type="button"
            onClick={() => { setMode('apiKey'); setError(null) }}
            className={cn('btn-ghost flex-1 justify-center', mode === 'apiKey' && 'text-white bg-bg-elevated')}
          >
            <KeyRound className="w-4 h-4" /> API key
          </button>
        </div>

        <div>
          <label className="input-label" htmlFor="credential">
            {mode === 'bearer' ? 'Token de acesso' : 'API key'}
          </label>
          <textarea
            id="credential"
            className="input font-mono min-h-24"
            autoComplete="off"
            spellCheck={false}
            placeholder={mode === 'bearer' ? 'eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9…' : 'cypher_sk_…'}
            value={value}
            onChange={(event) => setValue(event.target.value)}
          />
          <p className="text-xs text-gray-500 mt-1.5">
            A credencial fica apenas nesta aba do navegador (<code className="mono">sessionStorage</code>) e é
            descartada ao encerrar a sessão.
          </p>
        </div>

        {error && <p className="text-sm text-risk-high">{error}</p>}

        <button type="submit" className="btn-primary w-full justify-center">Entrar</button>
      </form>
    </div>
  )
}
