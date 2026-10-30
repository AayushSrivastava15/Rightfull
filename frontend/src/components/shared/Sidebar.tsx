import { NavLink } from 'react-router-dom'
import {
  LayoutDashboard,
  Box,
  KeyRound,
  Users,
  Settings,
  Layers,
  ShieldAlert,
} from 'lucide-react'
import { cn } from '@/lib/utils'

const navItems = [
  { to: '/', label: 'Dashboard', icon: LayoutDashboard },
  { to: '/workspaces', label: 'Workspaces', icon: Layers },
  { to: '/products', label: 'Products', icon: Box },
  { to: '/licenses', label: 'Licenses', icon: KeyRound },
  { to: '/customers', label: 'Customers', icon: Users },
  { to: '/auth', label: 'Authentication', icon: ShieldAlert },
  { to: '/settings', label: 'Settings', icon: Settings },
]

export function Sidebar() {
  return (
    <aside className="hidden w-64 flex-col border-r border-zinc-800 bg-zinc-950/60 p-4 md:flex">
      <div className="mb-4 px-3 text-xs font-semibold uppercase tracking-wider text-zinc-500">
        Navigation
      </div>
      <nav className="flex flex-1 flex-col space-y-1">
        {navItems.map((item) => {
          const Icon = item.icon
          return (
            <NavLink
              key={item.to}
              to={item.to}
              end={item.to === '/'}
              className={({ isActive }) =>
                cn(
                  'flex items-center gap-3 rounded-lg px-3 py-2 text-sm font-medium transition-colors',
                  isActive
                    ? 'bg-blue-600/10 text-blue-400 font-semibold'
                    : 'text-zinc-400 hover:bg-zinc-900 hover:text-zinc-100'
                )
              }
            >
              <Icon className="h-4 w-4" />
              <span>{item.label}</span>
            </NavLink>
          )
        })}
      </nav>
      <div className="rounded-lg border border-zinc-800/80 bg-zinc-900/40 p-3 text-xs text-zinc-400">
        <p className="font-medium text-zinc-200">Milestone 1 Foundation</p>
        <p className="mt-1 text-zinc-500">
          Package-by-feature modular monolith ready for Milestone 3.3.
        </p>
      </div>
    </aside>
  )
}
