import { apiClient, getCredential, setCredential, type Credential } from '../lib/axios'

interface TokenResponse {
  access_token: string
  token_type: 'Bearer'
}

const STORAGE_KEY = 'cypher_credential'

function persist(credential: Credential | null): void {
  setCredential(credential)
  if (credential) sessionStorage.setItem(STORAGE_KEY, JSON.stringify(credential))
  else sessionStorage.removeItem(STORAGE_KEY)
}

/** Recarrega a credencial da sessão do navegador; usada no bootstrap da aplicação. */
export function restoreCredential(): boolean {
  const stored = sessionStorage.getItem(STORAGE_KEY)
  if (!stored) return false
  try {
    const parsed = JSON.parse(stored) as Credential
    if (parsed.kind !== 'bearer' && parsed.kind !== 'apiKey') return false
    setCredential(parsed)
    return getCredential() != null
  } catch {
    sessionStorage.removeItem(STORAGE_KEY)
    return false
  }
}

export function signInWithToken(token: string): void {
  persist({ kind: 'bearer', value: token })
}

export function signInWithApiKey(apiKey: string): void {
  persist({ kind: 'apiKey', value: apiKey })
}

export async function authenticateWithDevToken(): Promise<void> {
  const { data } = await apiClient.get<TokenResponse>('/dev/token')
  persist({ kind: 'bearer', value: data.access_token })
}

export function logout(): void {
  persist(null)
}

export function isAuthenticated(): boolean {
  return getCredential() != null
}
