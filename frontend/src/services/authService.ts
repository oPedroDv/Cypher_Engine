import { apiClient, setAuthToken } from '../lib/axios'

interface TokenResponse {
  access_token: string
  token_type: 'Bearer'
}


export function acceptLoginToken(token: string): void {
  setAuthToken(token)
}

export async function authenticateWithDevToken(): Promise<void> {
  const { data } = await apiClient.get<TokenResponse>('/dev/token')
  setAuthToken(data.access_token)
}

export function logout(): void {
  setAuthToken(null)
}
