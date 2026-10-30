import {
  Card,
  CardHeader,
  CardTitle,
  CardDescription,
  CardContent,
} from '@/components/ui/card'
import { Badge } from '@/components/ui/badge'
import { LayoutDashboard, Shield, Cpu, KeyRound, Server } from 'lucide-react'

export function DashboardPlaceholder() {
  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-bold tracking-tight text-white">
          Platform Overview
        </h1>
        <p className="text-sm text-zinc-400">
          Rightful production-grade software licensing management foundation.
        </p>
      </div>

      <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-4">
        <Card>
          <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
            <CardTitle className="text-sm font-medium text-zinc-300">
              Architecture
            </CardTitle>
            <Server className="h-4 w-4 text-blue-400" />
          </CardHeader>
          <CardContent>
            <div className="text-xl font-bold text-white">Modular Monolith</div>
            <p className="text-xs text-zinc-500 mt-1">Package-by-feature pattern</p>
          </CardContent>
        </Card>

        <Card>
          <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
            <CardTitle className="text-sm font-medium text-zinc-300">
              Backend Core
            </CardTitle>
            <Cpu className="h-4 w-4 text-emerald-400" />
          </CardHeader>
          <CardContent>
            <div className="text-xl font-bold text-white">Java 25 LTS</div>
            <p className="text-xs text-zinc-500 mt-1">Spring Boot 3.4.x + JPA</p>
          </CardContent>
        </Card>

        <Card>
          <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
            <CardTitle className="text-sm font-medium text-zinc-300">
              Asymmetric Keys
            </CardTitle>
            <KeyRound className="h-4 w-4 text-purple-400" />
          </CardHeader>
          <CardContent>
            <div className="text-xl font-bold text-white">Ed25519</div>
            <p className="text-xs text-zinc-500 mt-1">Signed entitlement tokens</p>
          </CardContent>
        </Card>

        <Card>
          <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
            <CardTitle className="text-sm font-medium text-zinc-300">
              Key Hashing
            </CardTitle>
            <Shield className="h-4 w-4 text-amber-400" />
          </CardHeader>
          <CardContent>
            <div className="text-xl font-bold text-white">HMAC-SHA-256</div>
            <p className="text-xs text-zinc-500 mt-1">Zero plaintext database storage</p>
          </CardContent>
        </Card>
      </div>

      <Card>
        <CardHeader>
          <div className="flex items-center justify-between">
            <div className="flex items-center gap-2">
              <LayoutDashboard className="h-5 w-5 text-blue-500" />
              <CardTitle>Milestone 1 Foundation Status</CardTitle>
            </div>
            <Badge
              variant="outline"
              className="border-blue-500/30 text-blue-400 bg-blue-500/10"
            >
              Active Milestone
            </Badge>
          </div>
          <CardDescription>
            Scaffolded repositories, strict TypeScript, Tailwind CSS v4, Spring Boot REST
            baseline, and architectural blueprints.
          </CardDescription>
        </CardHeader>
        <CardContent className="space-y-3 text-sm text-zinc-400">
          <p>
            The dashboard business analytics and charting engine will be mounted in
            Milestone 8. Full database schema and entity mappings are scheduled for{' '}
            <strong className="text-zinc-200">Milestone Step 3.3</strong>.
          </p>
          <div className="flex flex-wrap gap-2 pt-2">
            <Badge variant="secondary">Offline Grace: 7h Default</Badge>
            <Badge variant="secondary">Heartbeat: 5min Interval</Badge>
            <Badge variant="secondary">Supabase Auth JWKS</Badge>
            <Badge variant="secondary">Multi-Tenant Workspaces</Badge>
          </div>
        </CardContent>
      </Card>
    </div>
  )
}
