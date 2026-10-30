import {
  Card,
  CardHeader,
  CardTitle,
  CardDescription,
  CardContent,
} from '@/components/ui/card'
import { Badge } from '@/components/ui/badge'
import { KeyRound, ShieldAlert, Clock, RefreshCw } from 'lucide-react'

export function LicensingPlaceholder() {
  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-bold tracking-tight text-white">
          License Management
        </h1>
        <p className="text-sm text-zinc-400">
          Issue, revoke, renew, and inspect cryptographically secured software licenses.
        </p>
      </div>

      <div className="grid gap-4 md:grid-cols-2">
        <Card>
          <CardHeader>
            <div className="flex items-center gap-2">
              <KeyRound className="h-5 w-5 text-blue-500" />
              <CardTitle>HMAC-SHA-256 Storage</CardTitle>
            </div>
            <CardDescription>
              Confidentiality guarantee through non-reversible key hashing.
            </CardDescription>
          </CardHeader>
          <CardContent className="text-xs text-zinc-400 space-y-2">
            <p>
              License keys are rendered exactly once to the user upon issuance. Rightful
              stores only a secure HMAC-SHA-256 digest and public key prefix.
            </p>
            <div className="rounded bg-zinc-900 p-2 font-mono text-[11px] text-zinc-300">
              Format: RF-[ENV]-[PROD]-[ENTROPY]-[CHECKSUM]
            </div>
          </CardContent>
        </Card>

        <Card>
          <CardHeader>
            <div className="flex items-center gap-2">
              <Clock className="h-5 w-5 text-purple-400" />
              <CardTitle>Validation & Heartbeats</CardTitle>
            </div>
            <CardDescription>
              Configurable 5-minute heartbeats and 7-hour offline grace window.
            </CardDescription>
          </CardHeader>
          <CardContent className="text-xs text-zinc-400 space-y-2">
            <p>
              Applications poll `/api/v1/validation/heartbeat` every 5 minutes. If
              unreachable, applications transition into a safe 7-hour offline grace
              period.
            </p>
            <div className="flex gap-2">
              <Badge variant="outline">Heartbeat: 5m</Badge>
              <Badge variant="outline">Grace: 7h Default</Badge>
            </div>
          </CardContent>
        </Card>

        <Card>
          <CardHeader>
            <div className="flex items-center gap-2">
              <ShieldAlert className="h-5 w-5 text-red-400" />
              <CardTitle>Revocation & Expiration</CardTitle>
            </div>
            <CardDescription>
              Immediate revocation propagation across active nodes.
            </CardDescription>
          </CardHeader>
          <CardContent className="text-xs text-zinc-400">
            Revoked licenses fail subsequent heartbeats immediately and refuse offline
            renewal tokens.
          </CardContent>
        </Card>

        <Card>
          <CardHeader>
            <div className="flex items-center gap-2">
              <RefreshCw className="h-5 w-5 text-emerald-400" />
              <CardTitle>Seat & Device Management</CardTitle>
            </div>
            <CardDescription>
              Node-locking and concurrent device activation controls.
            </CardDescription>
          </CardHeader>
          <CardContent className="text-xs text-zinc-400">
            Enforces maximum concurrent seats, hardware fingerprint matching, and device
            deactivation.
          </CardContent>
        </Card>
      </div>
    </div>
  )
}
