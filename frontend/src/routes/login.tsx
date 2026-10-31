import { createFileRoute } from '@tanstack/react-router'
import { AuthCardFlip } from '@/components/auth/AuthCardFlip'

export const Route = createFileRoute('/login')({
  head: () => ({
    meta: [
      { title: 'Sign In | Rightful' },
      { name: 'description', content: 'Sign in to access your Rightful licensing workspaces.' },
    ],
  }),
  component: LoginPage,
})

function LoginPage() {
  return <AuthCardFlip initialMode="login" />
}
