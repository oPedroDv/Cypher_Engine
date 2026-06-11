import { type ReactNode } from 'react'
import { Sidebar } from './Sidebar'

interface Props { children: ReactNode }

export function AppLayout({ children }: Props) {
  return (
    <div className="flex min-h-screen bg-bg-base">
      <Sidebar />
      <main className="flex-1 ml-60 min-h-screen">
        <div className="max-w-7xl mx-auto px-6 py-8 animate-in">
          {children}
        </div>
      </main>
    </div>
  )
}
