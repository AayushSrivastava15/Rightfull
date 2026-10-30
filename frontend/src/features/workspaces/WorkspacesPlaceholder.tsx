import {
  Card,
  CardHeader,
  CardTitle,
  CardDescription,
  CardContent,
} from '@/components/ui/card'
import { Badge } from '@/components/ui/badge'
import { Layers, ShieldCheck, Users } from 'lucide-react'

export function WorkspacesPlaceholder() {
  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-bold tracking-tight text-white">Workspaces</h1>
        <p className="text-sm text-zinc-400">
          Multi-tenant isolation and organizational boundaries.
        </p>
      </div>

      <Card>
        <CardHeader>
          <div className="flex items-center justify-between">
            <div className="flex items-center gap-2">
              <Layers className="h-5 w-5 text-blue-500" />
              <CardTitle>Tenant Isolation Engine</CardTitle>
            </div>
            <Badge variant="outline">Milestone 3</Badge>
          </div>
          <CardDescription>
            Strict logical workspace partitioning with role-based access control.
          </CardDescription>
        </CardHeader>
        <CardContent className="space-y-4">
          <div className="grid gap-4 md:grid-cols-3">
            <div className="rounded-lg border border-zinc-800 bg-zinc-900/40 p-4">
              <div className="flex items-center gap-2 font-medium text-white">
                <ShieldCheck className="h-4 w-4 text-emerald-400" />
                <span>Data Isolation</span>
              </div>
              <p className="mt-1 text-xs text-zinc-400">
                All entities (products, plans, licenses, customers) scoped strictly to
                `workspace_id`.
              </p>
            </div>

            <div className="rounded-lg border border-zinc-800 bg-zinc-900/40 p-4">
              <div className="flex items-center gap-2 font-medium text-white">
                <Users className="h-4 w-4 text-blue-400" />
                <span>RBAC Hierarchy</span>
              </div>
              <p className="mt-1 text-xs text-zinc-400">
                OWNER, ADMIN, DEVELOPER, and VIEWER roles enforced via Spring Security
                expressions.
              </p>
            </div>

            <div className="rounded-lg border border-zinc-800 bg-zinc-900/40 p-4">
              <div className="flex items-center gap-2 font-medium text-white">
                <Layers className="h-4 w-4 text-purple-400" />
                <span>Multi-Workspace</span>
              </div>
              <p className="mt-1 text-xs text-zinc-400">
                Users can belong to multiple workspaces with seamless context switching.
              </p>
            </div>
          </div>
        </CardContent>
      </Card>
    </div>
  )
}
