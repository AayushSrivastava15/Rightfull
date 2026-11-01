import React, { useState } from 'react'
import {
  LayoutDashboard,
  Package,
  Sliders,
  Users,
  Code2,
  Settings,
  ShieldCheck,
  ChevronLeft,
  ChevronRight,
  KeyRound,
  FileKey2,
  Cpu,
  Webhook,
  Building2,
  LogOut,
  Sparkles,
  ExternalLink,
} from 'lucide-react'
import { Tooltip, TooltipContent, TooltipProvider, TooltipTrigger } from '@/components/ui/tooltip'
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { useAuth } from '@/contexts/AuthContext'
import { cn } from '@/lib/utils'

export type DashboardTab =
  | 'overview'
  | 'products'
  | 'configurations'
  | 'customers'
  | 'licenses'
  | 'activations'
  | 'developer'
  | 'team'
  | 'settings'

interface DashboardSidebarProps {
  activeTab: DashboardTab
  onSelectTab: (tab: DashboardTab) => void
  collapsed: boolean
  onToggleCollapse: () => void
  productCount?: number
  customerCount?: number
}

interface NavItem {
  id: DashboardTab
  label: string
  icon: React.ComponentType<{ className?: string }>
  badge?: string | undefined
  badgeVariant?: 'default' | 'secondary' | 'outline' | undefined
  upcoming?: boolean | undefined
}

interface NavSection {
  title: string
  items: NavItem[]
}

export function DashboardSidebar({
  activeTab,
  onSelectTab,
  collapsed,
  onToggleCollapse,
  productCount,
  customerCount,
}: DashboardSidebarProps) {
  const { profile, user, signOut } = useAuth()
  const defaultWs = profile?.defaultWorkspace
  const role = defaultWs?.role || 'MEMBER'

  const sections: NavSection[] = [
    {
      title: 'Workspace',
      items: [
        { id: 'overview', label: 'Overview', icon: LayoutDashboard },
        {
          id: 'products',
          label: 'Products & Keys',
          icon: Package,
          badge: productCount !== undefined && productCount > 0 ? String(productCount) : undefined,
          badgeVariant: 'secondary',
        },
        {
          id: 'customers',
          label: 'Customers',
          icon: Users,
          badge: customerCount !== undefined && customerCount > 0 ? String(customerCount) : undefined,
          badgeVariant: 'secondary',
        },
        { id: 'team', label: 'Team & Access', icon: ShieldCheck },
      ],
    },
    {
      title: 'Licensing',
      items: [
        { id: 'configurations', label: 'License Plans', icon: Sliders },
        { id: 'licenses', label: 'Issued Licenses', icon: FileKey2 },
        { id: 'activations', label: 'Device Activations', icon: Cpu },
      ],
    },
    {
      title: 'Developer',
      items: [
        { id: 'developer', label: 'API & Documentation', icon: Code2 },
        {
          id: 'overview',
          label: 'Webhooks',
          icon: Webhook,
          badge: 'Upcoming',
          badgeVariant: 'outline',
          upcoming: true,
        },
      ],
    },
    {
      title: 'Settings',
      items: [
        { id: 'settings', label: 'Workspace Settings', icon: Settings },
      ],
    },
  ]

  return (
    <TooltipProvider delayDuration={150}>
      <aside
        className={cn(
          'sticky top-0 h-screen h-dvh flex flex-col border-r border-sidebar-border bg-sidebar transition-all duration-300 ease-in-out select-none shrink-0 z-20',
          collapsed ? 'w-16' : 'w-64'
        )}
      >
        {/* Workspace Brand Header */}
        <div className="flex h-16 shrink-0 items-center justify-between border-b border-sidebar-border px-3.5">
          {!collapsed ? (
            <div className="flex items-center gap-3 min-w-0">
              <div className="flex h-9 w-9 shrink-0 items-center justify-center rounded-lg bg-primary/10 text-primary ring-1 ring-primary/20">
                <ShieldCheck className="h-5 w-5" />
              </div>
              <div className="flex flex-col min-w-0">
                <div className="flex items-center gap-1.5">
                  <span className="font-bold text-sm tracking-tight text-sidebar-foreground truncate">
                    {defaultWs?.name || 'Rightful'}
                  </span>
                  <Badge
                    variant="outline"
                    className="text-[9px] uppercase px-1 py-0 font-mono tracking-wider border-primary/30 text-primary"
                  >
                    {role}
                  </Badge>
                </div>
                <span className="text-[10px] text-muted-foreground truncate font-mono">
                  {defaultWs?.slug || 'workspace'}
                </span>
              </div>
            </div>
          ) : (
            <div className="mx-auto flex h-9 w-9 items-center justify-center rounded-lg bg-primary/10 text-primary ring-1 ring-primary/20">
              <ShieldCheck className="h-5 w-5" />
            </div>
          )}

          {/* Collapse Toggle Button */}
          <button
            onClick={onToggleCollapse}
            className={cn(
              'hidden lg:flex h-6 w-6 items-center justify-center rounded-md text-muted-foreground hover:bg-sidebar-accent hover:text-sidebar-foreground transition-colors',
              collapsed && 'absolute -right-3 top-5 z-20 rounded-full border border-border bg-background shadow-sm'
            )}
            aria-label={collapsed ? 'Expand sidebar' : 'Collapse sidebar'}
          >
            {collapsed ? (
              <ChevronRight className="h-3.5 w-3.5" />
            ) : (
              <ChevronLeft className="h-4 w-4" />
            )}
          </button>
        </div>

        {/* Navigation Sections */}
        <div className="flex-1 min-h-0 overflow-y-auto px-2.5 py-4 space-y-6 custom-scrollbar">
          {sections.map((section) => (
            <div key={section.title} className="space-y-1">
              {!collapsed && (
                <p className="px-2.5 text-[10px] font-semibold uppercase tracking-wider text-muted-foreground/80">
                  {section.title}
                </p>
              )}
              <div className="space-y-0.5">
                {section.items.map((item) => {
                  const Icon = item.icon
                  const isActive = activeTab === item.id && !item.upcoming

                  const buttonContent = (
                    <button
                      key={item.label}
                      onClick={() => {
                        if (!item.upcoming) {
                          onSelectTab(item.id)
                        }
                      }}
                      disabled={item.upcoming}
                      className={cn(
                        'group flex w-full items-center gap-2.5 rounded-lg px-2.5 py-2 text-xs font-medium transition-all text-left',
                        isActive
                          ? 'bg-primary/10 text-primary font-semibold dark:bg-primary/15'
                          : item.upcoming
                          ? 'cursor-not-allowed text-muted-foreground/60 hover:text-muted-foreground/60'
                          : 'text-sidebar-foreground/80 hover:bg-sidebar-accent hover:text-sidebar-foreground'
                      )}
                    >
                      <Icon
                        className={cn(
                          'h-4 w-4 shrink-0 transition-colors',
                          isActive
                            ? 'text-primary'
                            : 'text-muted-foreground group-hover:text-sidebar-foreground'
                        )}
                      />
                      {!collapsed && (
                        <div className="flex flex-1 items-center justify-between min-w-0">
                          <span className="truncate">{item.label}</span>
                          {item.badge && (
                            <Badge
                              variant={item.badgeVariant ?? 'secondary'}
                              className={cn(
                                'text-[10px] px-1.5 py-0 shrink-0 font-normal',
                                item.upcoming && 'text-[9px] border-muted text-muted-foreground/70'
                              )}
                            >
                              {item.badge}
                            </Badge>
                          )}
                        </div>
                      )}
                    </button>
                  )

                  if (collapsed) {
                    return (
                      <Tooltip key={item.label}>
                        <TooltipTrigger asChild>{buttonContent}</TooltipTrigger>
                        <TooltipContent side="right" align="center" className="flex items-center gap-2 text-xs">
                          <span>{item.label}</span>
                          {item.badge && (
                            <Badge variant="outline" className="text-[9px] px-1 py-0">
                              {item.badge}
                            </Badge>
                          )}
                        </TooltipContent>
                      </Tooltip>
                    )
                  }

                  return buttonContent
                })}
              </div>
            </div>
          ))}
        </div>

        {/* User Footer Profile */}
        <div className="mt-auto shrink-0 border-t border-sidebar-border p-2.5 bg-sidebar">
          {!collapsed ? (
            <div className="flex items-center justify-between gap-2 rounded-lg p-2 bg-sidebar-accent/50">
              <div className="flex items-center gap-2.5 min-w-0">
                {profile?.avatarUrl ? (
                  <img
                    src={profile.avatarUrl}
                    alt={profile.fullName || 'User'}
                    className="h-7 w-7 rounded-full object-cover ring-1 ring-border"
                  />
                ) : (
                  <div className="flex h-7 w-7 items-center justify-center rounded-full bg-primary/10 text-primary text-xs font-bold uppercase ring-1 ring-primary/20">
                    {(profile?.fullName || user?.email || 'U')[0]}
                  </div>
                )}
                <div className="flex flex-col min-w-0">
                  <span className="text-xs font-semibold text-sidebar-foreground truncate">
                    {profile?.fullName || user?.email?.split('@')[0]}
                  </span>
                  <span className="text-[10px] text-muted-foreground truncate">
                    {user?.email}
                  </span>
                </div>
              </div>

              <button
                onClick={signOut}
                title="Sign Out"
                className="rounded-md p-1.5 text-muted-foreground hover:bg-destructive/10 hover:text-destructive transition-colors"
                aria-label="Sign out"
              >
                <LogOut className="h-3.5 w-3.5" />
              </button>
            </div>
          ) : (
            <Tooltip>
              <TooltipTrigger asChild>
                <button
                  onClick={signOut}
                  className="mx-auto flex h-8 w-8 items-center justify-center rounded-lg text-muted-foreground hover:bg-destructive/10 hover:text-destructive transition-colors"
                  aria-label="Sign out"
                >
                  <LogOut className="h-4 w-4" />
                </button>
              </TooltipTrigger>
              <TooltipContent side="right">Sign out</TooltipContent>
            </Tooltip>
          )}
        </div>
      </aside>
    </TooltipProvider>
  )
}
