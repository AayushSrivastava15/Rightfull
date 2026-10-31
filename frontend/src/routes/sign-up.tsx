import { createFileRoute } from '@tanstack/react-router'
import { AuthCardFlip } from '@/components/auth/AuthCardFlip'

export const Route = createFileRoute('/sign-up')({
  head: () => ({
    meta: [
      { title: 'Create Account | Rightful' },
      { name: 'description', content: 'Create a Rightful account and provision your licensing workspace.' },
    ],
  }),
  component: SignUpAliasPage,
})

function SignUpAliasPage() {
  return <AuthCardFlip initialMode="signup" />
}

