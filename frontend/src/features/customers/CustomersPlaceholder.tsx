import {
  Card,
  CardHeader,
  CardTitle,
  CardDescription,
  CardContent,
} from '@/components/ui/card'
import { Badge } from '@/components/ui/badge'
import { Users, User, Building } from 'lucide-react'

export function CustomersPlaceholder() {
  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-bold tracking-tight text-white">Customers</h1>
        <p className="text-sm text-zinc-400">
          Manage license assignees across individual developers and enterprise
          organizations.
        </p>
      </div>

      <Card>
        <CardHeader>
          <div className="flex items-center justify-between">
            <div className="flex items-center gap-2">
              <Users className="h-5 w-5 text-blue-500" />
              <CardTitle>Customer Domain Model</CardTitle>
            </div>
            <Badge variant="outline">Milestone 4</Badge>
          </div>
          <CardDescription>
            Segregated customer records linked to issued licenses.
          </CardDescription>
        </CardHeader>
        <CardContent className="space-y-4">
          <div className="grid gap-4 md:grid-cols-2">
            <div className="rounded-lg border border-zinc-800 bg-zinc-900/40 p-4">
              <div className="flex items-center gap-2 font-medium text-white">
                <User className="h-4 w-4 text-emerald-400" />
                <span>Individual Licenses</span>
              </div>
              <p className="mt-1 text-xs text-zinc-400">
                Single-seat, named developer licenses linked directly to personal email
                identities.
              </p>
            </div>

            <div className="rounded-lg border border-zinc-800 bg-zinc-900/40 p-4">
              <div className="flex items-center gap-2 font-medium text-white">
                <Building className="h-4 w-4 text-purple-400" />
                <span>Organization Licenses</span>
              </div>
              <p className="mt-1 text-xs text-zinc-400">
                Multi-seat, pooled team licensing with centralized billing contact and
                domain-level seat claims.
              </p>
            </div>
          </div>
        </CardContent>
      </Card>
    </div>
  )
}
