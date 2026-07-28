import { useCallback, useEffect, useMemo, useState, type ReactNode } from 'react'
import { onUnauthorized } from '../lib/axios'
import { isAuthenticated, logout, signInWithApiKey, signInWithToken } from '../services/authService'
import { queryClient } from '../lib/queryClient'
import { AuthContext, type AuthState } from './authContext'

export function AuthProvider({ children }: { children: ReactNode }) {
  const [authenticated, setAuthenticated] = useState(isAuthenticated)
  const [expired, setExpired] = useState(false)

  const signOut = useCallback(() => {
    logout()
    queryClient.clear()
    setAuthenticated(false)
  }, [])

  useEffect(() => {
    onUnauthorized(() => {
      logout()
      queryClient.clear()
      setAuthenticated(false)
      setExpired(true)
    })
    return () => onUnauthorized(null)
  }, [])

  const value = useMemo<AuthState>(() => ({
    authenticated,
    expired,
    signInWithToken: (token: string) => {
      signInWithToken(token)
      setExpired(false)
      setAuthenticated(true)
    },
    signInWithApiKey: (apiKey: string) => {
      signInWithApiKey(apiKey)
      setExpired(false)
      setAuthenticated(true)
    },
    signOut,
  }), [authenticated, expired, signOut])

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}
