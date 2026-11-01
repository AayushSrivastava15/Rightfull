import React, { useEffect, useState } from 'react'
import { createFileRoute, useNavigate } from '@tanstack/react-router'
import { useQuery, useQueryClient } from '@tanstack/react-query'
import { ShieldCheck, X } from 'lucide-react'
import { motion, useReducedMotion } from 'motion/react'
import { useAuth } from '@/contexts/AuthContext'
import { apiClient } from '@/services/apiClient'
import { DashboardSidebar, type DashboardTab } from '@/components/dashboard/DashboardSidebar'
import { DashboardHeader } from '@/components/dashboard/DashboardHeader'
import { CommandMenu } from '@/components/dashboard/CommandMenu'
import { OverviewView } from '@/components/dashboard/OverviewView'
import { ProductsView } from '@/components/dashboard/ProductsView'
import { DeveloperDocsView } from '@/components/dashboard/DeveloperDocsView'
import { WorkspaceTeamView } from '@/components/dashboard/WorkspaceTeamView'
import { LicenseConfigurationPanel } from '@/components/licensing/LicenseConfigurationPanel'
import { CustomerDirectoryPanel } from '@/components/licensing/CustomerDirectoryPanel'
import { IssuedLicensesPanel } from '@/components/licensing/IssuedLicensesPanel'
import { DeviceActivationsPanel } from '@/components/licensing/DeviceActivationsPanel'
import { type Customer, type LicenseConfiguration, type Product } from '@/types'

export const Route = createFileRoute('/dashboard')({
  head: () => ({
    meta: [
      { title: 'Workspace Control Center | Rightful' },
      { name: 'description', content: 'Rightful Multi-Tenant Licensing Workspace Dashboard.' },
    ],
  }),
  component: DashboardPage,
})

function DashboardPage() {
  const navigate = useNavigate()
  const queryClient = useQueryClient()
  const shouldReduceMotion = useReducedMotion()
  const { session, profile, user, loading } = useAuth()

  const [activeTab, setActiveTab] = useState<DashboardTab>('overview')
  const [sidebarCollapsed, setSidebarCollapsed] = useState(() => {
    if (typeof window === 'undefined') return false
    return localStorage.getItem('rightfull_sidebar_collapsed') === 'true'
  })
  const [mobileSidebarOpen, setMobileSidebarOpen] = useState(false)
  const [commandOpen, setCommandOpen] = useState(false)
  const [isCreateProductOpen, setIsCreateProductOpen] = useState(false)

  // Redirect unauthenticated visitors
  useEffect(() => {
    if (!loading && !session) {
      navigate({ to: '/login' })
    }
  }, [session, loading, navigate])

  // Global Cmd+K / Ctrl+K keyboard shortcut
  useEffect(() => {
    const handleKeyDown = (e: KeyboardEvent) => {
      if ((e.metaKey || e.ctrlKey) && e.key === 'k') {
        e.preventDefault()
        setCommandOpen((prev) => !prev)
      }
    }
    window.addEventListener('keydown', handleKeyDown)
    return () => window.removeEventListener('keydown', handleKeyDown)
  }, [])

  const defaultWs = profile?.defaultWorkspace
  const displayName = profile?.fullName || user?.email?.split('@')[0] || 'Developer'
  const canManage = defaultWs?.role === 'OWNER' || defaultWs?.role === 'ADMIN'

  const toggleSidebarCollapse = () => {
    setSidebarCollapsed((prev) => {
      const next = !prev
      try {
        localStorage.setItem('rightfull_sidebar_collapsed', String(next))
      } catch {}
      return next
    })
  }

  // 1. Fetch Products
  const {
    data: products = [],
    isLoading: isLoadingProducts,
    refetch: refetchProducts,
  } = useQuery({
    queryKey: ['products', defaultWs?.id],
    queryFn: () => (defaultWs?.id ? apiClient.getProducts(defaultWs.id) : Promise.resolve([])),
    enabled: !!defaultWs?.id,
  })

  // 2. Fetch Customers
  const { data: customerPage } = useQuery({
    queryKey: ['customers', defaultWs?.id],
    queryFn: () => (defaultWs?.id ? apiClient.getCustomers(defaultWs.id, '', 'ACTIVE', 0, 50) : Promise.resolve(null)),
    enabled: !!defaultWs?.id,
  })
  const customers: Customer[] = customerPage?.content || []

  // 3. Fetch License Configurations (for first active product if available)
  const firstProductId = products[0]?.id
  const { data: configurations = [] } = useQuery({
    queryKey: ['configurations', defaultWs?.id, firstProductId],
    queryFn: () =>
      defaultWs?.id && firstProductId
        ? apiClient.getConfigurations(defaultWs.id, firstProductId)
        : Promise.resolve([]),
    enabled: !!defaultWs?.id && !!firstProductId,
  })

  // 4. Fetch System Health
  const { data: health } = useQuery({
    queryKey: ['system-health'],
    queryFn: apiClient.getHealth,
    staleTime: 60_000,
  })

  if (loading) {
    return (
      <div className="flex min-h-screen items-center justify-center bg-background text-muted-foreground">
        <div className="flex flex-col items-center gap-3">
          <div className="h-8 w-8 animate-spin rounded-full border-2 border-primary border-t-transparent" />
          <p className="text-sm font-medium">Verifying workspace session...</p>
        </div>
      </div>
    )
  }

  if (!session) {
    return null
  }

  return (
    <div className="flex min-h-screen w-full bg-background text-foreground antialiased selection:bg-primary/20 selection:text-primary">
      {/* Desktop Collapsible Sidebar */}
      <div className="hidden lg:block shrink-0">
        <DashboardSidebar
          activeTab={activeTab}
          onSelectTab={setActiveTab}
          collapsed={sidebarCollapsed}
          onToggleCollapse={toggleSidebarCollapse}
          productCount={products.length}
          customerCount={customers.length}
        />
      </div>

      {/* Mobile Drawer Sidebar */}
      {mobileSidebarOpen && (
        <div className="fixed inset-0 z-50 flex lg:hidden">
          <div
            className="fixed inset-0 bg-black/50 backdrop-blur-sm transition-opacity duration-200"
            onClick={() => setMobileSidebarOpen(false)}
          />
          <div className="relative z-50 flex h-full h-dvh w-72 flex-col bg-sidebar shadow-2xl">
            <div className="absolute right-2 top-3 z-30">
              <button
                onClick={() => setMobileSidebarOpen(false)}
                className="rounded-lg p-2 text-muted-foreground hover:bg-sidebar-accent hover:text-sidebar-foreground transition-colors"
                aria-label="Close sidebar"
              >
                <X className="h-5 w-5" />
              </button>
            </div>
            <DashboardSidebar
              activeTab={activeTab}
              onSelectTab={(tab) => {
                setActiveTab(tab)
                setMobileSidebarOpen(false)
              }}
              collapsed={false}
              onToggleCollapse={() => {}}
              productCount={products.length}
              customerCount={customers.length}
            />
          </div>
        </div>
      )}

      {/* Main Content Area */}
      <div className="flex flex-1 flex-col min-w-0">
        {/* Top Header */}
        <DashboardHeader
          activeTab={activeTab}
          onOpenCommand={() => setCommandOpen(true)}
          onToggleMobileSidebar={() => setMobileSidebarOpen(true)}
          systemHealth={health?.status === 'UP' ? 'UP' : 'UNKNOWN'}
          workspaceName={defaultWs?.name}
        />

        {/* Viewport Content with Smooth Transitions */}
        <main className="flex-1 p-4 md:p-8 max-w-7xl w-full mx-auto space-y-8">
          <motion.div
            key={activeTab}
            initial={shouldReduceMotion ? false : { opacity: 0, y: 8 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ duration: 0.22, ease: [0.16, 1, 0.3, 1] }}
            className="space-y-8"
          >
            {activeTab === 'overview' && (
              <OverviewView
                displayName={displayName}
                workspaceName={defaultWs?.name}
                products={products}
                configurations={configurations}
                customers={customers}
                onNavigate={setActiveTab}
                onOpenCreateProduct={() => {
                  setActiveTab('products')
                  setIsCreateProductOpen(true)
                }}
              />
            )}

            {activeTab === 'products' && defaultWs?.id && (
              <ProductsView
                workspaceId={defaultWs.id}
                products={products}
                canManage={canManage}
                isLoading={isLoadingProducts}
                onRefresh={refetchProducts}
                isCreateOpen={isCreateProductOpen}
                setIsCreateOpen={setIsCreateProductOpen}
              />
            )}

            {activeTab === 'configurations' && defaultWs?.id && (
              <div className="space-y-6">
                <div>
                  <h2 className="text-xl font-bold tracking-tight text-foreground">
                    Product License Configurations
                  </h2>
                  <p className="text-xs text-muted-foreground mt-0.5">
                    Define reusable licensing permission presets, seat limits, and developer-defined JSON entitlements.
                  </p>
                </div>
                <LicenseConfigurationPanel
                  workspaceId={defaultWs.id}
                  products={products}
                  canManage={canManage}
                />
              </div>
            )}

            {activeTab === 'licenses' && defaultWs?.id && (
              <IssuedLicensesPanel
                workspaceId={defaultWs.id}
                products={products}
              />
            )}

            {activeTab === 'activations' && defaultWs?.id && (
              <DeviceActivationsPanel
                workspaceId={defaultWs.id}
                products={products}
              />
            )}

            {activeTab === 'customers' && defaultWs?.id && (
              <div className="space-y-6">
                <div>
                  <h2 className="text-xl font-bold tracking-tight text-foreground">
                    Customer Directory
                  </h2>
                  <p className="text-xs text-muted-foreground mt-0.5">
                    Maintain developer client organizations, email contacts, and custom external references.
                  </p>
                </div>
                <CustomerDirectoryPanel
                  workspaceId={defaultWs.id}
                  canManage={canManage}
                />
              </div>
            )}

            {activeTab === 'developer' && defaultWs?.id && (
              <DeveloperDocsView workspaceId={defaultWs.id} />
            )}

            {(activeTab === 'team' || activeTab === 'settings') && (
              <WorkspaceTeamView />
            )}
          </motion.div>
        </main>
      </div>

      {/* Global Command Palette */}
      <CommandMenu
        open={commandOpen}
        onOpenChange={setCommandOpen}
        onNavigate={setActiveTab}
      />
    </div>
  )
}
