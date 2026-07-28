import { createContext, useContext } from 'react'

export interface AuthState {
  authenticated: boolean
  expired: boolean
  signInWithToken: (token: string) => void
  signInWithApiKey: (apiKey: string) => void
  signOut: () => void
}

export const AuthContext = createContext<AuthState | null>(null)

export function useAuth(): AuthState {
  const context = useContext(AuthContext)
  if (!context) throw new Error('useAuth deve ser usado dentro de AuthProvider')
  return context
}
