import { render, screen, fireEvent } from '@testing-library/react'
import { describe, expect, it, vi } from 'vitest'
import { AuthCardFlip } from '@/components/auth/AuthCardFlip'

// Mock tanstack router navigation and link
vi.mock('@tanstack/react-router', () => ({
  useNavigate: () => vi.fn(),
  Link: ({ children, to, className }: any) => (
    <a href={to} className={className}>
      {children}
    </a>
  ),
}))

// Mock AuthContext
vi.mock('@/contexts/AuthContext', () => ({
  useAuth: () => ({
    signInWithEmail: vi.fn(),
    signUpWithEmail: vi.fn(),
    signInWithOAuth: vi.fn(),
    session: null,
    profile: null,
    error: null,
  }),
}))

describe('AuthCardFlip Component', () => {
  it('renders sign in card on initial login mode', () => {
    render(<AuthCardFlip initialMode="login" />)

    expect(screen.getByRole('heading', { name: /Sign In to Rightful/i })).toBeInTheDocument()
    expect(screen.getByRole('button', { name: /^Sign In$/i })).toBeInTheDocument()
  })

  it('renders sign up card when initialMode is signup', () => {
    render(<AuthCardFlip initialMode="signup" />)

    expect(screen.getByRole('heading', { name: /Create an Account/i })).toBeInTheDocument()
    expect(screen.getByRole('button', { name: /^Create Account$/i })).toBeInTheDocument()
  })

  it('flips between Sign In and Sign Up when flip buttons are clicked', () => {
    render(<AuthCardFlip initialMode="login" />)

    const signUpFlipBtn = screen.getByRole('button', { name: /^Sign Up$/i })
    expect(signUpFlipBtn).toBeInTheDocument()

    // Click flip to signup
    fireEvent.click(signUpFlipBtn)
    expect(document.title).toBe('Create Account | Rightful')

    // Click flip to signin
    const signInFlipBtn = screen.getByRole('button', { name: /^Sign In$/i })
    expect(signInFlipBtn).toBeInTheDocument()
    fireEvent.click(signInFlipBtn)
    expect(document.title).toBe('Sign In | Rightful')
  })

  it('toggles password visibility', () => {
    render(<AuthCardFlip initialMode="login" />)

    const toggleBtn = screen.getByLabelText(/Show sign in password/i)
    expect(toggleBtn).toBeInTheDocument()

    const passwordInput = screen.getByPlaceholderText('••••••••')
    expect(passwordInput).toHaveAttribute('type', 'password')

    fireEvent.click(toggleBtn)
    expect(passwordInput).toHaveAttribute('type', 'text')
  })
})
