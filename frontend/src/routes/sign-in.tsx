import { createFileRoute } from '@tanstack/react-router'
import { AuthCardFlip } from '@/components/auth/AuthCardFlip'

export const Route = createFileRoute('/sign-in')({
  head: () => ({
    meta: [
      { title: 'Sign In | Rightful' },
      { name: 'description', content: 'Sign in to access your Rightful licensing workspaces.' },
    ],
  }),
  component: SignInPage,
})

function SignInPage() {
  return <AuthCardFlip initialMode="login" />
}

