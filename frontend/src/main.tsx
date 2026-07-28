import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import './index.css'
import App from './App'
import { authenticateWithDevToken, restoreCredential } from './services/authService'

async function bootstrap() {
  const restored = restoreCredential()

  if (!restored && import.meta.env.DEV) {
    try {
      await authenticateWithDevToken()
    } catch (error) {
      console.warn('Dev authentication unavailable; login será solicitado.', error)
    }
  }

  createRoot(document.getElementById('root')!).render(
    <StrictMode>
      <App />
    </StrictMode>,
  )
}

void bootstrap()
