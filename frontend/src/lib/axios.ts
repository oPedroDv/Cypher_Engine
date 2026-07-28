import axios from 'axios'
import type { ApiError } from '../types/analysis'

const BASE_URL = import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8080'


let authToken: string | null = null


export function setAuthToken(token: string | null): void {
  authToken = token?.trim() || null
}

export const apiClient = axios.create({
  baseURL: BASE_URL,
  timeout: 30_000,
  headers: {
    'Content-Type': 'application/json',
  },
})


apiClient.interceptors.request.use((config) => {
  if (authToken) config.headers.Authorization = `Bearer ${authToken}`
  else delete config.headers.Authorization
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
    return Promise.reject(apiError)
  },
)
