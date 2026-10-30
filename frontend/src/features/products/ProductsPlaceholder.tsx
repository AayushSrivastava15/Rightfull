import {
  Card,
  CardHeader,
  CardTitle,
  CardDescription,
  CardContent,
} from '@/components/ui/card'
import { Badge } from '@/components/ui/badge'
import { Box, Key, Sliders } from 'lucide-react'

export function ProductsPlaceholder() {
  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-bold tracking-tight text-white">
          Software Products & Plans
        </h1>
        <p className="text-sm text-zinc-400">
          Register client applications, define plan tiers, and manage Ed25519
          cryptographic keys.
        </p>
      </div>

      <Card>
        <CardHeader>
          <div className="flex items-center justify-between">
            <div className="flex items-center gap-2">
              <Box className="h-5 w-5 text-blue-500" />
              <CardTitle>Product Catalog Architecture</CardTitle>
            </div>
            <Badge variant="outline">Milestone 4</Badge>
          </div>
          <CardDescription>
            Configurable software products, versions, and tiered entitlement matrices.
          </CardDescription>
        </CardHeader>
        <CardContent className="space-y-4">
          <div className="grid gap-4 md:grid-cols-2">
            <div className="rounded-lg border border-zinc-800 bg-zinc-900/40 p-4">
              <div className="flex items-center gap-2 font-medium text-white">
                <Key className="h-4 w-4 text-purple-400" />
                <span>Ed25519 Asymmetric Keypairs</span>
              </div>
              <p className="mt-1 text-xs text-zinc-400">
                Each product generates a dedicated Ed25519 keypair. The public key is
                embedded in client SDKs for offline verification.
              </p>
            </div>

            <div className="rounded-lg border border-zinc-800 bg-zinc-900/40 p-4">
              <div className="flex items-center gap-2 font-medium text-white">
                <Sliders className="h-4 w-4 text-emerald-400" />
                <span>Configurable Plan Tiers</span>
              </div>
              <p className="mt-1 text-xs text-zinc-400">
                Seat constraints, custom feature flags, and custom offline grace periods
                (defaulting to 7 hours).
              </p>
            </div>
          </div>
        </CardContent>
      </Card>
    </div>
  )
}
