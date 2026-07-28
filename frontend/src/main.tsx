import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import './index.css'
import App from './App'
import { authenticateWithDevToken } from './services/authService'

async function bootstrap() {

  if (import.meta.env.DEV) {
    try {
      await authenticateWithDevToken()
    } catch (error) {
      console.warn('Dev authentication unavailable; protected requests will require login.', error)
    }
  }

  createRoot(document.getElementById('root')!).render(
    <StrictMode>
      <App />
    </StrictMode>,
  )
}

void bootstrap()
