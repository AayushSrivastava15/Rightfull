import {
  Card,
  CardHeader,
  CardTitle,
  CardDescription,
  CardContent,
} from '@/components/ui/card'
import { Badge } from '@/components/ui/badge'
import { ShieldCheck, Mail } from 'lucide-react'

export function AuthPlaceholder() {
  return (
    <Card className="border-zinc-800">
      <CardHeader className="text-center">
        <div className="mx-auto mb-2 flex h-12 w-12 items-center justify-center rounded-full bg-blue-600/10 text-blue-500 ring-1 ring-blue-500/20">
          <ShieldCheck className="h-6 w-6" />
        </div>
        <CardTitle className="text-xl">Authentication Architecture</CardTitle>
        <CardDescription>
          Supabase Auth integration boundary (Milestone 3)
        </CardDescription>
      </CardHeader>
      <CardContent className="space-y-4 text-sm text-zinc-300">
        <div className="rounded-lg border border-zinc-800 bg-zinc-900/50 p-4 space-y-2 text-xs">
          <p className="font-semibold text-zinc-200">Confirmed Auth Methods:</p>
          <div className="flex items-center gap-2 text-zinc-400">
            <Mail className="h-4 w-4 text-blue-400" />
            <span>Email and Password with Supabase Gotrue</span>
          </div>
          <div className="flex items-center gap-2 text-zinc-400">
            <span className="font-bold text-red-400 text-xs">G</span>
            <span>Google OAuth 2.0</span>
          </div>
          <div className="flex items-center gap-2 text-zinc-400">
            <svg
              className="h-4 w-4 fill-current text-white"
              viewBox="0 0 24 24"
              aria-hidden="true"
            >
              <path
                fillRule="evenodd"
                clipRule="evenodd"
                d="M12 2C6.477 2 2 6.484 2 12.017c0 4.425 2.865 8.18 6.839 9.504.5.092.682-.217.682-.483 0-.237-.008-.868-.013-1.703-2.782.605-3.369-1.343-3.369-1.343-.454-1.158-1.11-1.466-1.11-1.466-.908-.62.069-.608.069-.608 1.003.07 1.53 1.032 1.53 1.032.892 1.53 2.341 1.088 2.91.832.092-.647.35-1.088.636-1.338-2.22-.253-4.555-1.113-4.555-4.951 0-1.093.39-1.988 1.029-2.688-.103-.253-.446-1.272.098-2.65 0 0 .84-.27 2.75 1.026A9.564 9.564 0 0112 6.844c.85.004 1.705.115 2.504.337 1.909-1.296 2.747-1.027 2.747-1.027.546 1.379.202 2.398.1 2.651.64.7 1.028 1.595 1.028 2.688 0 3.848-2.339 4.695-4.566 4.943.359.309.678.92.678 1.855 0 1.338-.012 2.419-.012 2.747 0 .268.18.58.688.482A10.019 10.019 0 0022 12.017C22 6.484 17.522 2 12 2z"
              />
            </svg>
            <span>GitHub OAuth 2.0</span>
          </div>
        </div>

        <p className="text-xs text-zinc-400 leading-relaxed">
          In adherence to strict milestone boundaries, OAuth and Gotrue sessions will be
          wired after database entity mapping (Step 3.3). Spring Boot validates incoming
          JWTs using Supabase JWKS asynchronously.
        </p>

        <div className="flex justify-center">
          <Badge variant="outline" className="text-zinc-400">
            Resource Server Ready
          </Badge>
        </div>
      </CardContent>
    </Card>
  )
}
