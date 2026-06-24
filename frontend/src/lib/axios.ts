import axios from 'axios'
import type { ApiError } from '../types/analysis'

const BASE_URL = import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8080'
const API_KEY = import.meta.env.VITE_API_KEY ?? ''
const ACCESS_TOKEN = import.meta.env.VITE_ACCESS_TOKEN ?? ''

export const apiClient = axios.create({
  baseURL: BASE_URL,
  timeout: 30_000,
  headers: {
    'Content-Type': 'application/json',
    ...(ACCESS_TOKEN
      ? { Authorization: `Bearer ${ACCESS_TOKEN}` }
      : API_KEY ? { 'X-Api-Key': API_KEY } : {}),
  },
})

// ─── Request interceptor (refresh key from localStorage override) ──────────────
apiClient.interceptors.request.use((config) => {
  const lsKey = localStorage.getItem('cypher_api_key')
  const lsToken = localStorage.getItem('cypher_access_token')
  if (!ACCESS_TOKEN && !API_KEY) {
    if (lsToken) config.headers.Authorization = `Bearer ${lsToken}`
    else if (lsKey) config.headers['X-Api-Key'] = lsKey
  }
  return config
})

// ─── Response interceptor (normalize errors) ───────────────────────────────────
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
