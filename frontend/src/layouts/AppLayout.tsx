import { Outlet } from 'react-router-dom'
import { Header } from '@/components/shared/Header'
import { Sidebar } from '@/components/shared/Sidebar'

export function AppLayout() {
  return (
    <div className="flex min-h-screen flex-col bg-zinc-950 text-zinc-100">
      <Header />
      <div className="flex flex-1">
        <Sidebar />
        <main className="flex-1 p-6 md:p-8">
          <Outlet />
        </main>
      </div>
    </div>
  )
}
