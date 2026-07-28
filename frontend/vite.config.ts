import { defineConfig, loadEnv } from 'vite'
import react from '@vitejs/plugin-react'
import path from 'path'

export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), '')
  const apiBase = env.VITE_API_BASE_URL || 'http://localhost:8080'

  const verifyProxyTls = mode !== 'development'

  return {
    plugins: [react()],
    resolve: {
      alias: { '@': path.resolve(__dirname, './src') },
    },
    server: {
      port: 5173,
      host: true,
      proxy: {
        '/v1': {
          target: apiBase,
          changeOrigin: true,
          secure: verifyProxyTls,
        },
        '/api': {
          target: apiBase,
          changeOrigin: true,
          secure: verifyProxyTls,
        },
        '/actuator': {
          target: apiBase,
          changeOrigin: true,
          secure: verifyProxyTls,
        },
      },
    },
  }
})
