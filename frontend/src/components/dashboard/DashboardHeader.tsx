import React from 'react'
import {
  Menu,
  Search,
  Command,
  Activity,
  ShieldCheck,
  ChevronRight,
} from 'lucide-react'
import { ThemeToggle } from '@/components/ui/theme-toggle'
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { type DashboardTab } from './DashboardSidebar'

interface DashboardHeaderProps {
  activeTab: DashboardTab
  onOpenCommand: () => void
  onToggleMobileSidebar: () => void
  systemHealth?: 'UP' | 'DOWN' | 'UNKNOWN' | undefined
  workspaceName?: string | undefined
}

const tabTitles: Record<DashboardTab, { section: string; title: string }> = {
  overview: { section: 'Workspace', title: 'Overview' },
  products: { section: 'Workspace', title: 'Products & Cryptographic Keys' },
  configurations: { section: 'Licensing', title: 'License Configurations' },
  licenses: { section: 'Licensing', title: 'Issued Licenses & Key Management' },
  activations: { section: 'Licensing', title: 'Device & Node Activations' },
  customers: { section: 'Workspace', title: 'Customer Directory' },
  developer: { section: 'Developer', title: 'API Documentation & SDKs' },
  team: { section: 'Workspace', title: 'Team Members & Roles' },
  settings: { section: 'Settings', title: 'Workspace Configuration' },
}

export function DashboardHeader({
  activeTab,
  onOpenCommand,
  onToggleMobileSidebar,
  systemHealth = 'UP',
  workspaceName,
}: DashboardHeaderProps) {
  const current = tabTitles[activeTab] || { section: 'Workspace', title: 'Overview' }

  return (
    <header className="sticky top-0 z-30 flex h-16 w-full items-center justify-between border-b border-border/60 bg-background/80 px-4 md:px-6 backdrop-blur-md">
      {/* Left: Mobile trigger & Breadcrumbs */}
      <div className="flex items-center gap-3">
        <button
          onClick={onToggleMobileSidebar}
          className="lg:hidden rounded-lg p-2 text-muted-foreground hover:bg-accent hover:text-foreground transition-colors"
          aria-label="Open mobile navigation"
        >
          <Menu className="h-5 w-5" />
        </button>

        {/* Breadcrumb Path */}
        <nav className="flex items-center gap-1.5 text-xs text-muted-foreground" aria-label="Breadcrumb">
          <span className="font-medium text-muted-foreground hover:text-foreground transition-colors hidden sm:inline">
            {workspaceName || 'Rightful'}
          </span>
          <ChevronRight className="h-3 w-3 text-muted-foreground/60 hidden sm:inline" />
          <span className="text-muted-foreground hidden md:inline">{current.section}</span>
          <ChevronRight className="h-3 w-3 text-muted-foreground/60 hidden md:inline" />
          <span className="font-semibold text-foreground">{current.title}</span>
        </nav>
      </div>

      {/* Right: Search command pill, Health status, Theme Toggle */}
      <div className="flex items-center gap-2.5 sm:gap-3">
        {/* Command Search Trigger Pill */}
        <button
          onClick={onOpenCommand}
          className="group hidden sm:flex items-center gap-2 rounded-lg border border-border/60 bg-muted/30 px-2.5 py-1.5 text-xs text-muted-foreground transition-all hover:border-border hover:bg-muted/60 hover:text-foreground"
          aria-label="Open Command Palette"
        >
          <Search className="h-3.5 w-3.5 text-muted-foreground group-hover:text-foreground transition-colors" />
          <span className="font-normal text-muted-foreground">Search commands...</span>
          <kbd className="pointer-events-none flex h-4 select-none items-center gap-0.5 rounded border border-border/80 bg-background px-1.5 font-mono text-[10px] font-medium text-muted-foreground">
            <span className="text-[10px]">⌘</span>K
          </kbd>
        </button>

        {/* Small mobile search icon */}
        <button
          onClick={onOpenCommand}
          className="sm:hidden rounded-lg p-2 text-muted-foreground hover:bg-accent hover:text-foreground transition-colors"
          aria-label="Open search"
        >
          <Search className="h-4 w-4" />
        </button>

        {/* Live Backend Health Indicator */}
        <div className="hidden md:flex items-center gap-1.5 rounded-full border border-border/50 bg-background/50 px-2.5 py-1 text-[11px] text-muted-foreground">
          <span
            className={`h-2 w-2 rounded-full ${
              systemHealth === 'UP' ? 'bg-emerald-500 animate-pulse' : 'bg-destructive'
            }`}
          />
          <span className="font-mono text-[10px] tracking-tight">API {systemHealth}</span>
        </div>

        {/* Global Theme Toggle */}
        <ThemeToggle />
      </div>
    </header>
  )
}
