import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom'
import { QueryClientProvider } from '@tanstack/react-query'
import { queryClient } from './lib/queryClient'
import { AppLayout } from './components/layout/AppLayout'
import { AuthProvider } from './contexts/AuthProvider'
import { useAuth } from './contexts/authContext'
import DashboardPage from './pages/Dashboard/DashboardPage'
import NewAnalysisPage from './pages/NewAnalysis/NewAnalysisPage'
import AnalysisDetailPage from './pages/AnalysisDetail/AnalysisDetailPage'
import HistoryPage from './pages/History/HistoryPage'
import ObservabilityPage from './pages/Observability/ObservabilityPage'
import LoginPage from './pages/Login/LoginPage'

function AuthenticatedApp() {
  const { authenticated } = useAuth()

  if (!authenticated) return <LoginPage />

  return (
    <BrowserRouter>
      <AppLayout>
        <Routes>
          <Route path="/"                   element={<DashboardPage />} />
          <Route path="/nova-analise"        element={<NewAnalysisPage />} />
          <Route path="/analise/:id"         element={<AnalysisDetailPage />} />
          <Route path="/historico"           element={<HistoryPage />} />
          <Route path="/observabilidade"     element={<ObservabilityPage />} />
          <Route path="*"                    element={<Navigate to="/" replace />} />
        </Routes>
      </AppLayout>
    </BrowserRouter>
  )
}

export default function App() {
  return (
    <QueryClientProvider client={queryClient}>
      <AuthProvider>
        <AuthenticatedApp />
      </AuthProvider>
    </QueryClientProvider>
  )
}
