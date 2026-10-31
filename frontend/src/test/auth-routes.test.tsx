import { QueryClient } from '@tanstack/react-query'
import { createRouter } from '@tanstack/react-router'
import { describe, expect, it } from 'vitest'
import { routeTree } from '@/routeTree.gen'

describe('Authentication Routes Configuration', () => {
  it('registers login, signup, and dashboard routes in router', () => {
    const router = createRouter({
      routeTree,
      context: { queryClient: new QueryClient() },
    })

    const loginMatch = router.matchRoutes('/login')
    expect(loginMatch.length).toBeGreaterThan(0)
    expect(loginMatch.at(-1)?.routeId).toBe('/login')

    const signupMatch = router.matchRoutes('/signup')
    expect(signupMatch.length).toBeGreaterThan(0)
    expect(signupMatch.at(-1)?.routeId).toBe('/signup')

    const dashboardMatch = router.matchRoutes('/dashboard')
    expect(dashboardMatch.length).toBeGreaterThan(0)
    expect(dashboardMatch.at(-1)?.routeId).toBe('/dashboard')

    const signInMatch = router.matchRoutes('/sign-in')
    expect(signInMatch.length).toBeGreaterThan(0)
    expect(signInMatch.at(-1)?.routeId).toBe('/sign-in')

    const signUpMatch = router.matchRoutes('/sign-up')
    expect(signUpMatch.length).toBeGreaterThan(0)
    expect(signUpMatch.at(-1)?.routeId).toBe('/sign-up')
  })
})

