import { ShieldCheck, Activity } from 'lucide-react'
import { Badge } from '@/components/ui/badge'
import { useHealth } from '@/hooks/useHealth'

export function Header() {
  const { data: health, isLoading } = useHealth()

  return (
    <header className="sticky top-0 z-40 flex h-16 w-full items-center justify-between border-b border-zinc-800 bg-zinc-950/80 px-6 backdrop-blur">
      <div className="flex items-center gap-3">
        <div className="flex h-9 w-9 items-center justify-center rounded-lg bg-blue-600/10 text-blue-500 ring-1 ring-blue-500/20">
          <ShieldCheck className="h-5 w-5 text-blue-500" />
        </div>
        <div className="flex flex-col">
          <div className="flex items-center gap-2">
            <span className="font-bold tracking-tight text-white">Rightful</span>
            <Badge variant="outline" className="text-[10px] py-0 px-1.5 font-mono">
              v0.0.1
            </Badge>
          </div>
          <span className="text-xs text-zinc-400">Software Licensing Platform</span>
        </div>
      </div>

      <div className="flex items-center gap-4">
        <div className="flex items-center gap-2 text-xs text-zinc-400">
          <Activity className="h-4 w-4 text-zinc-500" />
          <span>Backend API:</span>
          {isLoading ? (
            <Badge variant="outline" className="text-zinc-500">
              Checking...
            </Badge>
          ) : health?.status === 'UP' ? (
            <Badge
              variant="success"
              className="text-emerald-400 border-emerald-500/20 bg-emerald-500/10"
            >
              Online ({health.springBootVersion ?? 'Spring Boot'})
            </Badge>
          ) : (
            <Badge variant="secondary" className="text-zinc-400">
              Connecting...
            </Badge>
          )}
        </div>
      </div>
    </header>
  )
}
