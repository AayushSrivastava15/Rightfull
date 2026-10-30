import { Outlet } from 'react-router-dom'
import { ShieldCheck } from 'lucide-react'

export function AuthLayout() {
  return (
    <div className="flex min-h-screen flex-col items-center justify-center bg-zinc-950 p-4 text-zinc-100">
      <div className="mb-6 flex items-center gap-3">
        <div className="flex h-10 w-10 items-center justify-center rounded-xl bg-blue-600/10 text-blue-500 ring-1 ring-blue-500/20">
          <ShieldCheck className="h-6 w-6 text-blue-500" />
        </div>
        <span className="text-xl font-bold tracking-tight text-white">Rightful</span>
      </div>
      <div className="w-full max-w-md">
        <Outlet />
      </div>
    </div>
  )
}
