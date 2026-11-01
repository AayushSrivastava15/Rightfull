import React from 'react'
import {
  Package,
  Sliders,
  Users,
  KeyRound,
  ShieldCheck,
  ArrowRight,
  Plus,
  Terminal,
  Clock,
  Sparkles,
  ExternalLink,
  Cpu,
  Layers,
  FileCheck2,
} from 'lucide-react'
import { MagicCard } from '@/components/ui/magic-card'
import { BentoGrid, BentoCard } from '@/components/ui/bento-grid'
import { NumberTicker } from '@/components/ui/number-ticker'
import { ShimmerButton } from '@/components/ui/shimmer-button'
import { OnboardingChecklist } from './OnboardingChecklist'
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { type Product, type Customer, type LicenseConfiguration } from '@/types'
import { type DashboardTab } from './DashboardSidebar'

interface OverviewViewProps {
  displayName: string
  workspaceName?: string | undefined
  products: Product[]
  configurations: LicenseConfiguration[]
  customers: Customer[]
  onNavigate: (tab: DashboardTab) => void
  onOpenCreateProduct: () => void
}

function getGreeting(name: string): string {
  const hour = new Date().getHours()
  if (hour < 12) return `Good morning, ${name}`
  if (hour < 18) return `Good afternoon, ${name}`
  return `Good evening, ${name}`
}

export function OverviewView({
  displayName,
  workspaceName,
  products,
  configurations,
  customers,
  onNavigate,
  onOpenCreateProduct,
}: OverviewViewProps) {
  const activeProducts = products.filter((p) => p.status === 'ACTIVE')
  const greeting = getGreeting(displayName)

  // Real workspace activity items derived from data
  const activityItems = [
    ...products.map((p) => ({
      id: `prod-${p.id}`,
      type: 'product',
      title: `Product "${p.name}" registered`,
      subtitle: `Slug: ${p.slug} • Status: ${p.status}`,
      date: p.createdAt ? new Date(p.createdAt).toLocaleDateString() : 'Recent',
      icon: Package,
      color: 'text-blue-500',
    })),
    ...configurations.map((c) => ({
      id: `conf-${c.id}`,
      type: 'configuration',
      title: `License plan "${c.name}" configured`,
      subtitle: `Version ${c.version} • ${c.durationDays ? `${c.durationDays} days` : 'Perpetual'} • ${c.seatLimit} seats`,
      date: c.createdAt ? new Date(c.createdAt).toLocaleDateString() : 'Recent',
      icon: Sliders,
      color: 'text-violet-500',
    })),
    ...customers.map((cust) => ({
      id: `cust-${cust.id}`,
      type: 'customer',
      title: `Customer "${cust.name}" added`,
      subtitle: `${cust.email} • ${cust.company || 'Direct'}`,
      date: cust.createdAt ? new Date(cust.createdAt).toLocaleDateString() : 'Recent',
      icon: Users,
      color: 'text-emerald-500',
    })),
  ].slice(0, 5)

  return (
    <div className="space-y-8 animate-in fade-in-50 duration-300">
      {/* A. Welcome Banner & Hero Actions */}
      <div className="relative overflow-hidden rounded-2xl border border-border/70 bg-gradient-to-br from-card via-card to-primary/5 p-6 md:p-8 shadow-sm">
        <div className="flex flex-col md:flex-row md:items-center justify-between gap-6">
          <div className="space-y-2 max-w-xl">
            <div className="inline-flex items-center gap-2 rounded-full border border-primary/20 bg-primary/10 px-3 py-1 text-xs font-medium text-primary">
              <Sparkles className="h-3.5 w-3.5" />
              <span>Rightful Core Licensing Foundation</span>
            </div>
            <h1 className="text-2xl md:text-3xl font-bold tracking-tight text-foreground">
              {greeting}
            </h1>
            <p className="text-sm text-muted-foreground leading-relaxed">
              Your cryptographic software licensing infrastructure for{' '}
              <span className="font-semibold text-foreground">{workspaceName || 'this workspace'}</span>.
              Manage software products, asymmetric Ed25519 keys, license rules, and customer directories.
            </p>
          </div>

          <div className="flex flex-wrap items-center gap-3">
            <ShimmerButton onClick={onOpenCreateProduct} className="shadow-sm">
              <Plus className="h-3.5 w-3.5 mr-1.5" />
              Register Product
            </ShimmerButton>
            <Button
              variant="outline"
              size="sm"
              onClick={() => onNavigate('configurations')}
              className="border-border/80 hover:bg-muted"
            >
              <Sliders className="h-3.5 w-3.5 mr-1.5" />
              Manage Plans
            </Button>
          </div>
        </div>
      </div>

      {/* B. Main Real Metrics Section */}
      <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
        {/* Metric 1: Total Products */}
        <MagicCard className="p-5">
          <div className="flex items-center justify-between">
            <span className="text-xs font-medium text-muted-foreground">Total Products</span>
            <div className="flex h-8 w-8 items-center justify-center rounded-lg bg-blue-500/10 text-blue-500">
              <Package className="h-4 w-4" />
            </div>
          </div>
          <div className="mt-3">
            <div className="text-2xl md:text-3xl font-bold tracking-tight text-foreground">
              <NumberTicker value={products.length} />
            </div>
            <p className="text-[11px] text-muted-foreground mt-1">
              {activeProducts.length} active in catalog
            </p>
          </div>
        </MagicCard>

        {/* Metric 2: License Configurations */}
        <MagicCard className="p-5">
          <div className="flex items-center justify-between">
            <span className="text-xs font-medium text-muted-foreground">License Plans</span>
            <div className="flex h-8 w-8 items-center justify-center rounded-lg bg-violet-500/10 text-violet-500">
              <Sliders className="h-4 w-4" />
            </div>
          </div>
          <div className="mt-3">
            <div className="text-2xl md:text-3xl font-bold tracking-tight text-foreground">
              <NumberTicker value={configurations.length} />
            </div>
            <p className="text-[11px] text-muted-foreground mt-1">
              Configured entitlement presets
            </p>
          </div>
        </MagicCard>

        {/* Metric 3: Customers */}
        <MagicCard className="p-5">
          <div className="flex items-center justify-between">
            <span className="text-xs font-medium text-muted-foreground">Customers</span>
            <div className="flex h-8 w-8 items-center justify-center rounded-lg bg-emerald-500/10 text-emerald-500">
              <Users className="h-4 w-4" />
            </div>
          </div>
          <div className="mt-3">
            <div className="text-2xl md:text-3xl font-bold tracking-tight text-foreground">
              <NumberTicker value={customers.length} />
            </div>
            <p className="text-[11px] text-muted-foreground mt-1">
              Registered client directory
            </p>
          </div>
        </MagicCard>

        {/* Metric 4: Cryptography & Security */}
        <MagicCard className="p-5">
          <div className="flex items-center justify-between">
            <span className="text-xs font-medium text-muted-foreground">Cryptography</span>
            <div className="flex h-8 w-8 items-center justify-center rounded-lg bg-amber-500/10 text-amber-500">
              <KeyRound className="h-4 w-4" />
            </div>
          </div>
          <div className="mt-3">
            <div className="text-lg md:text-xl font-bold tracking-tight text-foreground">
              Ed25519
            </div>
            <p className="text-[11px] text-muted-foreground mt-1">
              AES-256-GCM encrypted vault
            </p>
          </div>
        </MagicCard>
      </div>

      {/* C. Getting Started Checklist */}
      <OnboardingChecklist
        hasProducts={products.length > 0}
        hasSigningKeys={products.length > 0}
        hasConfigurations={configurations.length > 0}
        hasCustomers={customers.length > 0}
        onNavigate={onNavigate}
      />

      {/* D. Bento Grid Layout */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        {/* Product Catalog Spotlight (2 Cols) */}
        <div className="lg:col-span-2 rounded-xl border border-border/70 bg-card p-6 shadow-sm">
          <div className="flex items-center justify-between mb-4">
            <div className="flex items-center gap-2.5">
              <div className="flex h-8 w-8 items-center justify-center rounded-lg bg-blue-500/10 text-blue-500">
                <Package className="h-4 w-4" />
              </div>
              <div>
                <h3 className="text-sm font-semibold text-foreground">Products in Workspace</h3>
                <p className="text-xs text-muted-foreground">
                  Registered software products with active Ed25519 key pairs.
                </p>
              </div>
            </div>

            <Button
              variant="ghost"
              size="sm"
              onClick={() => onNavigate('products')}
              className="text-xs text-primary gap-1"
            >
              <span>View All</span>
              <ArrowRight className="h-3.5 w-3.5" />
            </Button>
          </div>

          {products.length === 0 ? (
            <div className="flex flex-col items-center justify-center rounded-lg border border-dashed border-border py-12 text-center">
              <Package className="h-8 w-8 text-muted-foreground/60 mb-2" />
              <p className="text-sm font-medium text-foreground">No products registered yet</p>
              <p className="text-xs text-muted-foreground max-w-sm mt-1">
                Register your first software application to configure license plans and generate cryptographic signing keys.
              </p>
              <Button size="sm" onClick={onOpenCreateProduct} className="mt-4 gap-1.5">
                <Plus className="h-3.5 w-3.5" />
                Register Product
              </Button>
            </div>
          ) : (
            <div className="divide-y divide-border/60">
              {products.slice(0, 4).map((product) => (
                <div
                  key={product.id}
                  className="flex items-center justify-between py-3 group hover:bg-muted/20 px-2 rounded-lg transition-colors"
                >
                  <div className="flex items-center gap-3 min-w-0">
                    <div className="h-8 w-8 rounded-lg bg-secondary flex items-center justify-center text-xs font-mono font-bold text-foreground">
                      {product.name.substring(0, 2).toUpperCase()}
                    </div>
                    <div className="min-w-0">
                      <div className="flex items-center gap-2">
                        <span className="text-xs font-semibold text-foreground truncate">
                          {product.name}
                        </span>
                        <Badge
                          variant={product.status === 'ACTIVE' ? 'secondary' : 'outline'}
                          className="text-[10px] px-1.5 py-0 font-normal"
                        >
                          {product.status}
                        </Badge>
                      </div>
                      <span className="text-[11px] text-muted-foreground font-mono truncate">
                        {product.slug}
                      </span>
                    </div>
                  </div>

                  <div className="flex items-center gap-2">
                    <Badge variant="outline" className="text-[10px] text-emerald-500 border-emerald-500/30 gap-1 hidden sm:flex">
                      <KeyRound className="h-3 w-3" />
                      Ed25519
                    </Badge>
                    <Button
                      variant="ghost"
                      size="sm"
                      onClick={() => onNavigate('products')}
                      className="h-7 px-2 text-xs text-muted-foreground group-hover:text-foreground"
                    >
                      Manage
                    </Button>
                  </div>
                </div>
              ))}
            </div>
          )}
        </div>

        {/* Recent Workspace Activity (1 Col) */}
        <div className="rounded-xl border border-border/70 bg-card p-6 shadow-sm flex flex-col justify-between">
          <div>
            <div className="flex items-center gap-2.5 mb-4">
              <div className="flex h-8 w-8 items-center justify-center rounded-lg bg-emerald-500/10 text-emerald-500">
                <Clock className="h-4 w-4" />
              </div>
              <div>
                <h3 className="text-sm font-semibold text-foreground">Recent Activity</h3>
                <p className="text-xs text-muted-foreground">Audit feed of workspace changes</p>
              </div>
            </div>

            {activityItems.length === 0 ? (
              <div className="flex flex-col items-center justify-center py-10 text-center text-muted-foreground">
                <Clock className="h-6 w-6 mb-2 opacity-50" />
                <p className="text-xs">No recent activity recorded.</p>
                <p className="text-[11px] text-muted-foreground/80 mt-0.5">
                  Register a product or plan to see events here.
                </p>
              </div>
            ) : (
              <div className="relative pl-4 space-y-4 border-l border-border/60">
                {activityItems.map((item) => {
                  const Icon = item.icon
                  return (
                    <div key={item.id} className="relative group">
                      <div className="absolute -left-[21px] top-1 h-2.5 w-2.5 rounded-full border-2 border-background bg-primary" />
                      <div>
                        <p className="text-xs font-medium text-foreground">{item.title}</p>
                        <p className="text-[11px] text-muted-foreground mt-0.5">{item.subtitle}</p>
                        <span className="text-[10px] text-muted-foreground/70">{item.date}</span>
                      </div>
                    </div>
                  )
                })}
              </div>
            )}
          </div>

          <div className="mt-6 pt-4 border-t border-border/60">
            <div className="flex items-center justify-between text-[11px] text-muted-foreground">
              <span>Audit Logging: Active</span>
              <Badge variant="outline" className="text-[10px] text-emerald-500 border-emerald-500/30">
                PostgreSQL RLS
              </Badge>
            </div>
          </div>
        </div>
      </div>

      {/* E. Licensing Architecture & Status Overview */}
      <div className="rounded-xl border border-border/70 bg-card p-6 shadow-sm">
        <div className="flex items-center justify-between mb-4">
          <div className="flex items-center gap-2.5">
            <div className="flex h-8 w-8 items-center justify-center rounded-lg bg-violet-500/10 text-violet-500">
              <Layers className="h-4 w-4" />
            </div>
            <div>
              <h3 className="text-sm font-semibold text-foreground">Licensing Infrastructure Status</h3>
              <p className="text-xs text-muted-foreground">
                How Rightful protects and manages your proprietary desktop, mobile, and server software.
              </p>
            </div>
          </div>
        </div>

        <div className="grid grid-cols-1 md:grid-cols-3 gap-4 pt-2">
          <div className="rounded-lg border border-border/60 p-4 bg-muted/20 space-y-2">
            <div className="flex items-center justify-between">
              <span className="text-xs font-semibold text-foreground flex items-center gap-1.5">
                <KeyRound className="h-4 w-4 text-primary" />
                Ed25519 Signing
              </span>
              <Badge variant="secondary" className="text-[10px] text-emerald-500 font-mono">
                OPERATIONAL
              </Badge>
            </div>
            <p className="text-xs text-muted-foreground leading-relaxed">
              License keys are cryptographically signed using asymmetric Ed25519 key pairs. Private keys are stored in an AES-256-GCM hardware/software encrypted vault.
            </p>
          </div>

          <div className="rounded-lg border border-border/60 p-4 bg-muted/20 space-y-2">
            <div className="flex items-center justify-between">
              <span className="text-xs font-semibold text-foreground flex items-center gap-1.5">
                <Sliders className="h-4 w-4 text-violet-500" />
                License Plans
              </span>
              <Badge variant="secondary" className="text-[10px] text-emerald-500 font-mono">
                V7 ACTIVE
              </Badge>
            </div>
            <p className="text-xs text-muted-foreground leading-relaxed">
              Configurable duration, seats, activations, and developer-defined JSON entitlements with immutable historical version tracking.
            </p>
          </div>

          <div className="rounded-lg border border-border/60 p-4 bg-muted/20 space-y-2">
            <div className="flex items-center justify-between">
              <span className="text-xs font-semibold text-foreground flex items-center gap-1.5">
                <Cpu className="h-4 w-4 text-amber-500" />
                Issuance & Activations
              </span>
              <Badge variant="outline" className="text-[10px] text-muted-foreground font-mono">
                UPCOMING
              </Badge>
            </div>
            <p className="text-xs text-muted-foreground leading-relaxed">
              High-throughput signed token issuance, device fingerprinting, machine heartbeat checks, and instant one-click revocation.
            </p>
          </div>
        </div>
      </div>
    </div>
  )
}
