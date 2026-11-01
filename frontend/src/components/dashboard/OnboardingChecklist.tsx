import React, { useState, useEffect } from 'react'
import {
  CheckCircle2,
  Circle,
  ArrowRight,
  Package,
  KeyRound,
  Sliders,
  UserPlus,
  BookOpen,
  ChevronDown,
  ChevronUp,
} from 'lucide-react'
import { Progress } from '@/components/ui/progress'
import { Button } from '@/components/ui/button'
import { Badge } from '@/components/ui/badge'

interface OnboardingChecklistProps {
  hasProducts: boolean
  hasSigningKeys: boolean
  hasConfigurations: boolean
  hasCustomers: boolean
  onNavigate: (tab: any) => void
}

export function OnboardingChecklist({
  hasProducts,
  hasSigningKeys,
  hasConfigurations,
  hasCustomers,
  onNavigate,
}: OnboardingChecklistProps) {
  const [hasReadDocs, setHasReadDocs] = useState(() => {
    if (typeof window === 'undefined') return false
    return localStorage.getItem('rightfull_docs_reviewed') === 'true'
  })
  const [collapsed, setCollapsed] = useState(false)

  const markDocsRead = () => {
    setHasReadDocs(true)
    try {
      localStorage.setItem('rightfull_docs_reviewed', 'true')
    } catch {}
    onNavigate('developer')
  }

  const tasks = [
    {
      id: 'product',
      title: 'Register your first product',
      description: 'Create a software catalog entry with code, version, and description.',
      completed: hasProducts,
      tab: 'products',
      icon: Package,
    },
    {
      id: 'signing-key',
      title: 'Generate Ed25519 signing keys',
      description: 'Asymmetric cryptographic key pairs for tamper-proof offline licenses.',
      completed: hasSigningKeys,
      tab: 'products',
      icon: KeyRound,
    },
    {
      id: 'configurations',
      title: 'Define license configuration presets',
      description: 'Configure feature entitlements, seat limits, and offline grace periods.',
      completed: hasConfigurations,
      tab: 'configurations',
      icon: Sliders,
    },
    {
      id: 'customer',
      title: 'Add a customer directory record (Optional)',
      description: 'Manage developer clients and organizations for license assignments.',
      completed: hasCustomers,
      tab: 'customers',
      icon: UserPlus,
    },
    {
      id: 'docs',
      title: 'Inspect API integration documentation',
      description: 'Review SDK guidance, curl examples, and Ed25519 verification standards.',
      completed: hasReadDocs,
      action: markDocsRead,
      icon: BookOpen,
    },
  ]

  const completedCount = tasks.filter((t) => t.completed).length
  const progressPercentage = Math.round((completedCount / tasks.length) * 100)

  // If all tasks are completed, optionally render a congratulatory banner
  const allCompleted = completedCount === tasks.length

  return (
    <div className="rounded-xl border border-border/60 bg-card p-5 shadow-sm transition-all dark:border-border/40">
      <div className="flex items-center justify-between">
        <div className="flex items-center gap-3">
          <div className="flex h-8 w-8 items-center justify-center rounded-lg bg-primary/10 text-primary">
            <CheckCircle2 className="h-4 w-4" />
          </div>
          <div>
            <div className="flex items-center gap-2">
              <h3 className="text-sm font-semibold text-foreground">
                Getting Started with Rightful
              </h3>
              <Badge variant="secondary" className="text-[10px] font-mono">
                {completedCount} of {tasks.length} Complete
              </Badge>
            </div>
            <p className="text-xs text-muted-foreground mt-0.5">
              Complete these initial foundation steps to issue and manage cryptographic licenses.
            </p>
          </div>
        </div>

        <button
          onClick={() => setCollapsed(!collapsed)}
          className="rounded-lg p-1.5 text-muted-foreground hover:bg-muted hover:text-foreground transition-colors"
          aria-label={collapsed ? 'Expand checklist' : 'Collapse checklist'}
        >
          {collapsed ? <ChevronDown className="h-4 w-4" /> : <ChevronUp className="h-4 w-4" />}
        </button>
      </div>

      <div className="mt-4">
        <Progress value={progressPercentage} className="h-1.5 bg-muted" />
      </div>

      {!collapsed && (
        <div className="mt-4 grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-2.5 pt-2">
          {tasks.map((task) => {
            const Icon = task.icon
            return (
              <div
                key={task.id}
                onClick={() => {
                  if (task.action) task.action()
                  else if (task.tab) onNavigate(task.tab)
                }}
                className={`group flex items-start gap-3 rounded-lg border p-3 text-left transition-all cursor-pointer ${
                  task.completed
                    ? 'border-border/40 bg-muted/20 opacity-80'
                    : 'border-border/70 bg-card hover:border-primary/50 hover:shadow-sm'
                }`}
              >
                <div className="mt-0.5">
                  {task.completed ? (
                    <CheckCircle2 className="h-4 w-4 text-emerald-500 fill-emerald-500/10" />
                  ) : (
                    <Circle className="h-4 w-4 text-muted-foreground group-hover:text-primary transition-colors" />
                  )}
                </div>
                <div className="flex-1 min-w-0">
                  <div className="flex items-center justify-between gap-1">
                    <p
                      className={`text-xs font-medium truncate ${
                        task.completed
                          ? 'line-through text-muted-foreground'
                          : 'text-foreground group-hover:text-primary transition-colors'
                      }`}
                    >
                      {task.title}
                    </p>
                  </div>
                  <p className="text-[11px] text-muted-foreground line-clamp-1 mt-0.5">
                    {task.description}
                  </p>
                </div>
                {!task.completed && (
                  <ArrowRight className="h-3.5 w-3.5 text-muted-foreground opacity-0 group-hover:opacity-100 group-hover:translate-x-0.5 transition-all mt-1" />
                )}
              </div>
            )
          })}
        </div>
      )}
    </div>
  )
}
