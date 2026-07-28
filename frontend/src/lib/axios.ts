import axios from 'axios'
import type { ApiError } from '../types/analysis'

const BASE_URL = import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8080'

export type CredentialKind = 'bearer' | 'apiKey'

export interface Credential {
  kind: CredentialKind
  value: string
}

let credential: Credential | null = null
let unauthorizedHandler: (() => void) | null = null

export function setCredential(next: Credential | null): void {
  const value = next?.value.trim()
  credential = value ? { kind: next!.kind, value } : null
}

export function getCredential(): Credential | null {
  return credential
}

export function onUnauthorized(handler: (() => void) | null): void {
  unauthorizedHandler = handler
}

export const apiClient = axios.create({
  baseURL: BASE_URL,
  timeout: 30_000,
  headers: {
    'Content-Type': 'application/json',
  },
})

apiClient.interceptors.request.use((config) => {
  delete config.headers.Authorization
  delete config.headers['X-API-Key']
  if (credential?.kind === 'bearer') config.headers.Authorization = `Bearer ${credential.value}`
  else if (credential?.kind === 'apiKey') config.headers['X-API-Key'] = credential.value
  return config
})


apiClient.interceptors.response.use(
  (res) => res,
  (err) => {
    const message =
      err.response?.data?.message ??
      err.response?.data?.error ??
      err.message ??
      'Erro desconhecido'
    const apiError: ApiError = {
      message,
      status: err.response?.status,
      errorCode: err.response?.data?.error_code ?? err.response?.data?.error,
      existingAnalysisId: err.response?.data?.existing_analysis_id,
      errors: err.response?.data?.errors,
    }
    if (apiError.status === 401 && credential) unauthorizedHandler?.()
    return Promise.reject(apiError)
  },
)
