import { useEffect } from 'react'
import { createFileRoute, useNavigate, Link } from '@tanstack/react-router'
import {
  ShieldCheck,
  Building2,
  Users,
  LogOut,
  KeyRound,
  CheckCircle2,
  Package,
  Layers,
  ArrowRight,
} from 'lucide-react'
import { useAuth } from '@/contexts/AuthContext'
import { Button } from '@/components/ui/button'
import { Badge } from '@/components/ui/badge'
import {
  Card,
  CardHeader,
  CardTitle,
  CardDescription,
  CardContent,
} from '@/components/ui/card'

export const Route = createFileRoute('/dashboard')({
  head: () => ({
    meta: [
      { title: 'Workspace Dashboard | Rightful' },
      { name: 'description', content: 'Rightful Multi-Tenant Licensing Workspace Dashboard.' },
    ],
  }),
  component: DashboardPage,
})

function DashboardPage() {
  const navigate = useNavigate()
  const { session, profile, user, loading, signOut } = useAuth()

  useEffect(() => {
    if (!loading && !session) {
      navigate({ to: '/login' })
    }
  }, [session, loading, navigate])

  if (loading) {
    return (
      <div className="flex min-h-screen items-center justify-center bg-zinc-950 text-zinc-400">
        <div className="flex flex-col items-center gap-3">
          <div className="h-8 w-8 animate-spin rounded-full border-2 border-blue-500 border-t-transparent" />
          <p className="text-sm font-medium">Verifying workspace session...</p>
        </div>
      </div>
    )
  }

  if (!session) {
    return null
  }

  const defaultWs = profile?.defaultWorkspace
  const memberships = profile?.memberships ?? []
  const displayName = profile?.fullName || user?.email?.split('@')[0] || 'Developer'

  return (
    <div className="min-h-screen bg-zinc-950 text-zinc-100">
      {/* Top Navigation */}
      <header className="sticky top-0 z-40 flex h-16 w-full items-center justify-between border-b border-zinc-800 bg-zinc-950/80 px-6 backdrop-blur">
        <div className="flex items-center gap-3">
          <Link to="/" className="flex items-center gap-2">
            <div className="flex h-9 w-9 items-center justify-center rounded-lg bg-blue-600/10 text-blue-500 ring-1 ring-blue-500/20">
              <ShieldCheck className="h-5 w-5 text-blue-500" />
            </div>
            <span className="font-bold tracking-tight text-white">Rightful</span>
          </Link>
          <Badge variant="outline" className="border-blue-500/20 bg-blue-500/10 text-blue-400 text-xs">
            Workspace Console
          </Badge>
        </div>

        <div className="flex items-center gap-4">
          <div className="flex items-center gap-2">
            {profile?.avatarUrl ? (
              <img
                src={profile.avatarUrl}
                alt={displayName}
                className="h-8 w-8 rounded-full ring-1 ring-zinc-700 object-cover"
              />
            ) : (
              <div className="flex h-8 w-8 items-center justify-center rounded-full bg-zinc-800 text-zinc-300 ring-1 ring-zinc-700">
                <span className="text-xs font-bold uppercase">{displayName[0]}</span>
              </div>
            )}
            <div className="hidden sm:flex flex-col text-left">
              <span className="text-xs font-semibold text-white">{displayName}</span>
              <span className="text-[10px] text-zinc-400">{user?.email}</span>
            </div>
          </div>

          <Button
            variant="ghost"
            size="sm"
            className="gap-2 text-zinc-400 hover:text-red-400 hover:bg-zinc-900"
            onClick={async () => {
              await signOut()
              navigate({ to: '/login' })
            }}
          >
            <LogOut className="h-4 w-4" />
            <span className="hidden sm:inline">Sign Out</span>
          </Button>
        </div>
      </header>

      {/* Main Content */}
      <main className="mx-auto max-w-6xl p-6 md:p-8 space-y-8">
        {/* Welcome Banner */}
        <div className="flex flex-col md:flex-row md:items-center justify-between gap-4 rounded-2xl border border-zinc-800 bg-zinc-900/40 p-6 backdrop-blur">
          <div className="space-y-1">
            <h1 className="text-2xl font-bold tracking-tight text-white">
              Welcome back, {displayName}
            </h1>
            <p className="text-sm text-zinc-400">
              Your software licensing platform is active and ready to issue, validate, and manage client keys.
            </p>
          </div>
          <div className="flex items-center gap-3">
            <Badge
              variant="outline"
              className="border-emerald-500/20 bg-emerald-500/10 text-emerald-400 px-3 py-1 text-xs"
            >
              <CheckCircle2 className="mr-1.5 h-3.5 w-3.5" />
              Backend Connected
            </Badge>
          </div>
        </div>

        {/* Primary Workspace Card */}
        <div className="grid gap-6 md:grid-cols-3">
          <Card className="md:col-span-2 border-zinc-800 bg-zinc-900/60 shadow-lg">
            <CardHeader className="flex flex-row items-center justify-between pb-2">
              <div className="space-y-1">
                <CardTitle className="text-lg font-bold text-white flex items-center gap-2">
                  <Building2 className="h-5 w-5 text-blue-500" />
                  Primary Workspace
                </CardTitle>
                <CardDescription className="text-zinc-400">
                  Default multi-tenant operational boundary
                </CardDescription>
              </div>
              {defaultWs?.tier && (
                <Badge variant="secondary" className="font-mono text-xs uppercase bg-zinc-800 text-zinc-300">
                  {defaultWs.tier} Tier
                </Badge>
              )}
            </CardHeader>
            <CardContent className="space-y-4 pt-4">
              <div className="grid grid-cols-2 gap-4 rounded-xl border border-zinc-800/80 bg-zinc-950/50 p-4">
                <div>
                  <span className="text-xs text-zinc-500 font-medium">Workspace Name</span>
                  <p className="text-sm font-semibold text-white mt-0.5">
                    {defaultWs?.name || 'Personal Workspace'}
                  </p>
                </div>
                <div>
                  <span className="text-xs text-zinc-500 font-medium">Workspace Slug</span>
                  <p className="text-sm font-mono text-blue-400 mt-0.5">
                    {defaultWs?.slug || 'workspace'}
                  </p>
                </div>
                <div>
                  <span className="text-xs text-zinc-500 font-medium">Your Role</span>
                  <div className="mt-0.5">
                    <Badge variant="outline" className="border-blue-500/30 text-blue-400 font-mono text-xs">
                      {defaultWs?.role || 'OWNER'}
                    </Badge>
                  </div>
                </div>
                <div>
                  <span className="text-xs text-zinc-500 font-medium">Application User ID</span>
                  <p className="text-xs font-mono text-zinc-400 mt-0.5 truncate" title={profile?.id}>
                    {profile?.id || 'Provisioning...'}
                  </p>
                </div>
              </div>

              <div className="flex gap-3">
                <Button className="gap-2 bg-blue-600 hover:bg-blue-500 text-white" size="sm">
                  <KeyRound className="h-4 w-4" />
                  Issue New License
                </Button>
                <Button variant="outline" size="sm" className="border-zinc-700 bg-transparent hover:bg-zinc-800">
                  <Package className="h-4 w-4 mr-1.5" />
                  Register Product
                </Button>
              </div>
            </CardContent>
          </Card>

          {/* Memberships Card */}
          <Card className="border-zinc-800 bg-zinc-900/60 shadow-lg">
            <CardHeader className="pb-2">
              <CardTitle className="text-lg font-bold text-white flex items-center gap-2">
                <Users className="h-5 w-5 text-indigo-400" />
                Workspaces ({memberships.length})
              </CardTitle>
              <CardDescription className="text-zinc-400">
                Organizations you have access to
              </CardDescription>
            </CardHeader>
            <CardContent className="space-y-3 pt-3">
              {memberships.map((m) => (
                <div
                  key={m.workspaceId}
                  className="flex items-center justify-between rounded-lg border border-zinc-800/80 bg-zinc-950/40 p-3"
                >
                  <div className="space-y-0.5">
                    <p className="text-xs font-semibold text-white">{m.workspaceName}</p>
                    <p className="text-[10px] font-mono text-zinc-500">{m.workspaceSlug}</p>
                  </div>
                  <Badge variant="outline" className="text-[10px] font-mono border-zinc-700 text-zinc-300">
                    {m.role}
                  </Badge>
                </div>
              ))}
            </CardContent>
          </Card>
        </div>

        {/* Feature Roadmap Cards */}
        <div className="grid gap-4 sm:grid-cols-3">
          <div className="group rounded-xl border border-zinc-800 bg-zinc-900/40 p-5 transition-colors hover:border-zinc-700">
            <div className="flex items-center justify-between">
              <span className="grid h-9 w-9 place-items-center rounded-lg bg-blue-500/10 text-blue-400">
                <KeyRound className="h-5 w-5" />
              </span>
              <ArrowRight className="h-4 w-4 text-zinc-600 transition-transform group-hover:translate-x-0.5 group-hover:text-zinc-300" />
            </div>
            <h3 className="mt-4 font-semibold text-white">Cryptographic Licensing</h3>
            <p className="mt-1 text-xs text-zinc-400">
              Ed25519-signed offline verification and HMAC-SHA-256 non-reversible storage.
            </p>
          </div>

          <div className="group rounded-xl border border-zinc-800 bg-zinc-900/40 p-5 transition-colors hover:border-zinc-700">
            <div className="flex items-center justify-between">
              <span className="grid h-9 w-9 place-items-center rounded-lg bg-indigo-500/10 text-indigo-400">
                <Layers className="h-5 w-5" />
              </span>
              <ArrowRight className="h-4 w-4 text-zinc-600 transition-transform group-hover:translate-x-0.5 group-hover:text-zinc-300" />
            </div>
            <h3 className="mt-4 font-semibold text-white">Device Node-Locking</h3>
            <p className="mt-1 text-xs text-zinc-400">
              Seat enforcement, hardware fingerprint hashing, and heartbeat validation.
            </p>
          </div>

          <div className="group rounded-xl border border-zinc-800 bg-zinc-900/40 p-5 transition-colors hover:border-zinc-700">
            <div className="flex items-center justify-between">
              <span className="grid h-9 w-9 place-items-center rounded-lg bg-emerald-500/10 text-emerald-400">
                <ShieldCheck className="h-5 w-5" />
              </span>
              <ArrowRight className="h-4 w-4 text-zinc-600 transition-transform group-hover:translate-x-0.5 group-hover:text-zinc-300" />
            </div>
            <h3 className="mt-4 font-semibold text-white">RBAC Security</h3>
            <p className="mt-1 text-xs text-zinc-400">
              Granular OWNER, ADMIN, MEMBER, and VIEWER roles across isolated tenants.
            </p>
          </div>
        </div>
      </main>
    </div>
  )
}

