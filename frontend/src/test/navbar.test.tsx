import { render, screen, fireEvent } from '@testing-library/react'
import { describe, expect, it, vi } from 'vitest'
import { Navbar } from '@/components/landing/Navbar'

// Mock tanstack router link
vi.mock('@tanstack/react-router', () => ({
  Link: ({ children, to, className }: any) => (
    <a href={to} className={className}>
      {children}
    </a>
  ),
}))

// Mock AuthContext
vi.mock('@/contexts/AuthContext', () => ({
  useAuth: () => ({
    session: null,
    profile: null,
  }),
}))

describe('Navbar Component', () => {
  it('renders brand logo on left and navigation links', () => {
    render(<Navbar />)

    // Brand logo
    const logoLink = screen.getByRole('link', { name: /Rightful/i })
    expect(logoLink).toBeInTheDocument()
    expect(logoLink).toHaveAttribute('href', '/')

    // Navigation links
    expect(screen.getByRole('link', { name: /^Home$/i })).toBeInTheDocument()
    expect(screen.getByRole('link', { name: /^How it works$/i })).toBeInTheDocument()
    expect(screen.getByRole('link', { name: /^Features$/i })).toBeInTheDocument()
    expect(screen.getByRole('link', { name: /^Pricing$/i })).toBeInTheDocument()
    expect(screen.getByRole('link', { name: /^Docs$/i })).toBeInTheDocument()
  })

  it('renders auth action buttons when unauthenticated', () => {
    render(<Navbar />)

    expect(screen.getByRole('link', { name: /^Sign In$/i })).toBeInTheDocument()
    expect(screen.getByRole('link', { name: /^Get Started$/i })).toBeInTheDocument()
  })

  it('toggles theme when theme toggle button is clicked', () => {
    render(<Navbar />)

    const themeBtn = screen.getByLabelText(/Switch to (light|dark) theme/i)
    expect(themeBtn).toBeInTheDocument()

    fireEvent.click(themeBtn)
    expect(themeBtn).toBeInTheDocument()
  })

  it('opens and closes mobile menu on toggle', () => {
    render(<Navbar />)

    const menuToggle = screen.getByLabelText(/Open navigation menu/i)
    expect(menuToggle).toBeInTheDocument()

    // Open menu
    fireEvent.click(menuToggle)
    expect(screen.getByLabelText(/Close navigation menu/i)).toBeInTheDocument()

    // Close menu
    const closeToggle = screen.getByLabelText(/Close navigation menu/i)
    fireEvent.click(closeToggle)
    expect(screen.getByLabelText(/Open navigation menu/i)).toBeInTheDocument()
  })
})

