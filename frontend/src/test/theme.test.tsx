import React from 'react'
import { render, screen, fireEvent, act } from '@testing-library/react'
import { describe, it, expect, beforeEach, afterEach, vi } from 'vitest'
import { ThemeProvider, useTheme } from '@/contexts/ThemeContext'
import { ThemeToggle } from '@/components/ui/theme-toggle'

function ThemeTestConsumer() {
  const { theme, resolvedTheme, setTheme } = useTheme()
  return (
    <div>
      <span data-testid="current-theme">{theme}</span>
      <span data-testid="resolved-theme">{resolvedTheme}</span>
      <button onClick={() => setTheme('light')}>Set Light</button>
      <button onClick={() => setTheme('dark')}>Set Dark</button>
      <button onClick={() => setTheme('system')}>Set System</button>
    </div>
  )
}

describe('Global Theme System (rightfull-theme)', () => {
  let matchMediaMock: any

  beforeEach(() => {
    localStorage.clear()
    document.documentElement.className = ''

    // Mock window.matchMedia
    matchMediaMock = vi.fn().mockImplementation((query: string) => ({
      matches: false, // Default to light OS in tests
      media: query,
      onchange: null,
      addListener: vi.fn(),
      removeListener: vi.fn(),
      addEventListener: vi.fn(),
      removeEventListener: vi.fn(),
      dispatchEvent: vi.fn(),
    }))
    window.matchMedia = matchMediaMock
  })

  afterEach(() => {
    localStorage.clear()
    document.documentElement.className = ''
    vi.restoreAllMocks()
  })

  it('defaults to system theme when no localStorage value exists', () => {
    render(
      <ThemeProvider>
        <ThemeTestConsumer />
      </ThemeProvider>
    )

    expect(screen.getByTestId('current-theme')).toHaveTextContent('system')
    expect(screen.getByTestId('resolved-theme')).toHaveTextContent('light')
    expect(document.documentElement.classList.contains('dark')).toBe(false)
  })

  it('applies dark theme and persists to localStorage on selection', () => {
    render(
      <ThemeProvider>
        <ThemeTestConsumer />
      </ThemeProvider>
    )

    fireEvent.click(screen.getByText('Set Dark'))

    expect(screen.getByTestId('current-theme')).toHaveTextContent('dark')
    expect(screen.getByTestId('resolved-theme')).toHaveTextContent('dark')
    expect(document.documentElement.classList.contains('dark')).toBe(true)
    expect(localStorage.getItem('rightfull-theme')).toBe('dark')
  })

  it('applies light theme and persists to localStorage on selection', () => {
    render(
      <ThemeProvider>
        <ThemeTestConsumer />
      </ThemeProvider>
    )

    fireEvent.click(screen.getByText('Set Dark'))
    expect(document.documentElement.classList.contains('dark')).toBe(true)

    fireEvent.click(screen.getByText('Set Light'))

    expect(screen.getByTestId('current-theme')).toHaveTextContent('light')
    expect(screen.getByTestId('resolved-theme')).toHaveTextContent('light')
    expect(document.documentElement.classList.contains('dark')).toBe(false)
    expect(localStorage.getItem('rightfull-theme')).toBe('light')
  })

  it('restores theme from localStorage on reload', () => {
    localStorage.setItem('rightfull-theme', 'dark')

    render(
      <ThemeProvider>
        <ThemeTestConsumer />
      </ThemeProvider>
    )

    expect(screen.getByTestId('current-theme')).toHaveTextContent('dark')
    expect(screen.getByTestId('resolved-theme')).toHaveTextContent('dark')
    expect(document.documentElement.classList.contains('dark')).toBe(true)
  })

  it('handles invalid or corrupted localStorage values safely', () => {
    localStorage.setItem('rightfull-theme', 'unexpected_random_mode')

    render(
      <ThemeProvider>
        <ThemeTestConsumer />
      </ThemeProvider>
    )

    // Falls back to system gracefully
    expect(screen.getByTestId('current-theme')).toHaveTextContent('system')
  })

  it('renders ThemeToggle button with accessible label', () => {
    render(
      <ThemeProvider>
        <ThemeToggle />
      </ThemeProvider>
    )

    const toggleBtn = screen.getByRole('button', { name: /Toggle theme/i })
    expect(toggleBtn).toBeInTheDocument()
    expect(toggleBtn).toHaveAttribute('aria-label', expect.stringContaining('Toggle theme'))
  })

  it('renders dropdown options when opened', async () => {
    render(
      <ThemeProvider>
        <ThemeToggle defaultOpen={true} />
      </ThemeProvider>
    )

    expect(await screen.findByRole('menuitem', { name: /Light/i })).toBeInTheDocument()
    expect(screen.getByRole('menuitem', { name: /Dark/i })).toBeInTheDocument()
    expect(screen.getByRole('menuitem', { name: /System/i })).toBeInTheDocument()
  })

  it('switches theme via ThemeToggle dropdown selection', async () => {
    render(
      <ThemeProvider>
        <ThemeToggle defaultOpen={true} />
        <ThemeTestConsumer />
      </ThemeProvider>
    )

    const darkMenuItem = await screen.findByRole('menuitem', { name: /Dark/i })
    fireEvent.click(darkMenuItem)

    expect(localStorage.getItem('rightfull-theme')).toBe('dark')
    expect(document.documentElement.classList.contains('dark')).toBe(true)
  })
})
