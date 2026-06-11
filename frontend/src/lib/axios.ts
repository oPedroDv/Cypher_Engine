import axios from 'axios'

const BASE_URL = import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8080'
const API_KEY  = import.meta.env.VITE_API_KEY  ?? ''
const TENANT_ID = import.meta.env.VITE_TENANT_ID ?? ''

export const apiClient = axios.create({
  baseURL: BASE_URL,
  timeout: 30_000,
  headers: {
    'Content-Type': 'application/json',
    'X-Api-Key': API_KEY,
    'X-Tenant-Id': TENANT_ID,
  },
})

// ─── Request interceptor (refresh key from localStorage override) ──────────────
apiClient.interceptors.request.use((config) => {
  const lsKey = localStorage.getItem('cypher_api_key')
  const lsTenant = localStorage.getItem('cypher_tenant_id')
  if (lsKey) config.headers['X-Api-Key'] = lsKey
  if (lsTenant) config.headers['X-Tenant-Id'] = lsTenant
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
    return Promise.reject({ message, status: err.response?.status })
  },
)
