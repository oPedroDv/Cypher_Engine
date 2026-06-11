import { defineConfig, loadEnv } from 'vite'
import react from '@vitejs/plugin-react'
import path from 'path'

export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), '')
  const apiBase = env.VITE_API_BASE_URL || 'http://localhost:8080'

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
          secure: false,
        },
        '/api': {
          target: apiBase,
          changeOrigin: true,
          secure: false,
        },
        '/actuator': {
          target: apiBase,
          changeOrigin: true,
          secure: false,
        },
      },
    },
  }
})
