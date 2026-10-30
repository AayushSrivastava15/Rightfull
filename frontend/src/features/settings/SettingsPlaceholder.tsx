import {
  Card,
  CardHeader,
  CardTitle,
  CardDescription,
  CardContent,
} from '@/components/ui/card'
import { Badge } from '@/components/ui/badge'
import { Settings as SettingsIcon, Key, Shield, Webhook } from 'lucide-react'

export function SettingsPlaceholder() {
  return (
    <div className="space-y-6">
      <div className="flex items-center gap-3">
        <div className="flex h-9 w-9 items-center justify-center rounded-lg bg-zinc-800 text-zinc-300">
          <SettingsIcon className="h-5 w-5" />
        </div>
        <div>
          <h1 className="text-2xl font-bold tracking-tight text-white">Settings</h1>
          <p className="text-sm text-zinc-400">
            Workspace preferences, developer API keys, and webhook integration endpoints.
          </p>
        </div>
      </div>

      <div className="grid gap-4 md:grid-cols-3">
        <Card>
          <CardHeader>
            <div className="flex items-center gap-2">
              <Key className="h-5 w-5 text-blue-500" />
              <CardTitle className="text-base">Developer API Keys</CardTitle>
            </div>
            <CardDescription className="text-xs">
              M2M keys for CI/CD and automated license issuance.
            </CardDescription>
          </CardHeader>
          <CardContent>
            <Badge variant="outline">Milestone 7</Badge>
          </CardContent>
        </Card>

        <Card>
          <CardHeader>
            <div className="flex items-center gap-2">
              <Webhook className="h-5 w-5 text-purple-400" />
              <CardTitle className="text-base">Webhooks</CardTitle>
            </div>
            <CardDescription className="text-xs">
              Event payloads for license creation, activation, and expiration.
            </CardDescription>
          </CardHeader>
          <CardContent>
            <Badge variant="outline">Milestone 7</Badge>
          </CardContent>
        </Card>

        <Card>
          <CardHeader>
            <div className="flex items-center gap-2">
              <Shield className="h-5 w-5 text-emerald-400" />
              <CardTitle className="text-base">Audit Logs</CardTitle>
            </div>
            <CardDescription className="text-xs">
              Cryptographic verification logs and security audit trail.
            </CardDescription>
          </CardHeader>
          <CardContent>
            <Badge variant="outline">Milestone 3.3 / 6</Badge>
          </CardContent>
        </Card>
      </div>
    </div>
  )
}
