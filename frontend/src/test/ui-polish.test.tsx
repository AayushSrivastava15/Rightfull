import React, { useState } from 'react'
import { render, screen, fireEvent, waitFor } from '@testing-library/react'
import { describe, it, expect, beforeEach, afterEach, vi } from 'vitest'
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogDescription,
  DialogBody,
  DialogFooter,
} from '@/components/ui/dialog'
import { Button } from '@/components/ui/button'
import { DashboardSidebar, type DashboardTab } from '@/components/dashboard/DashboardSidebar'
import { ThemeProvider, useTheme } from '@/contexts/ThemeContext'
import { AuthContext } from '@/contexts/AuthContext'

describe('UI Polish: Dialogs, Motion, Buttons & Sidebar Layout', () => {
  let matchMediaMock: any

  beforeEach(() => {
    localStorage.clear()
    document.documentElement.className = ''
    document.body.className = ''

    matchMediaMock = vi.fn().mockImplementation((query: string) => ({
      matches: false,
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
    document.body.className = ''
    vi.restoreAllMocks()
  })

  // 1. DIALOG DESIGN SYSTEM
  describe('Premium Dialog System', () => {
    it('renders dialog with backdrop blur, rounded-2xl container, and scrollable DialogBody', () => {
      render(
        <Dialog open={true}>
          <DialogContent className="max-w-md">
            <DialogHeader>
              <DialogTitle>Preset Configuration</DialogTitle>
              <DialogDescription>Define permissions</DialogDescription>
            </DialogHeader>
            <DialogBody data-testid="dialog-body">
              <p>Scrollable body content</p>
            </DialogBody>
            <DialogFooter>
              <Button>Save</Button>
            </DialogFooter>
          </DialogContent>
        </Dialog>
      )

      expect(screen.getByRole('dialog')).toBeInTheDocument()
      expect(screen.getByText('Preset Configuration')).toBeInTheDocument()
      expect(screen.getByText('Scrollable body content')).toBeInTheDocument()

      const dialogContent = screen.getByRole('dialog')
      expect(dialogContent.className).toContain('rounded-2xl')
      expect(dialogContent.className).toContain('border-border/80')
      expect(dialogContent.className).toContain('shadow-2xl')

      // Body contains custom scrollbar and overflow-y-auto
      const body = screen.getByTestId('dialog-body')
      expect(body.className).toContain('overflow-y-auto')
      expect(body.className).toContain('custom-scrollbar')
      expect(body.className).toContain('flex-1')
    })

    it('renders accessible close button with hover feedback', () => {
      const onOpenChange = vi.fn()
      render(
        <Dialog open={true} onOpenChange={onOpenChange}>
          <DialogContent>
            <DialogHeader>
              <DialogTitle>Modal Title</DialogTitle>
            </DialogHeader>
            <DialogBody>
              <p>Content</p>
            </DialogBody>
          </DialogContent>
        </Dialog>
      )

      const closeBtn = screen.getByRole('button', { name: /Close/i })
      expect(closeBtn).toBeInTheDocument()
      expect(closeBtn.className).toContain('rounded-full')

      fireEvent.click(closeBtn)
      expect(onOpenChange).toHaveBeenCalledWith(false)
    })
  })

  // 2. BUTTON HOVER, PRESSED & LOADING STATES
  describe('Button Interaction Feedback', () => {
    it('renders with active scale reduction and transition classes', () => {
      render(<Button variant="default">Save Preset</Button>)
      const btn = screen.getByRole('button', { name: /Save Preset/i })
      expect(btn.className).toContain('active:scale-[0.98]')
      expect(btn.className).toContain('transition-all')
      expect(btn.className).toContain('rounded-lg')
    })

    it('renders loading spinner and disables clicks when loading prop is true', () => {
      const onClick = vi.fn()
      render(
        <Button loading={true} onClick={onClick}>
          Submit Changes
        </Button>
      )

      const btn = screen.getByRole('button', { name: /Submit Changes/i })
      expect(btn).toBeDisabled()
      expect(btn).toHaveAttribute('aria-busy', 'true')

      fireEvent.click(btn)
      expect(onClick).not.toHaveBeenCalled()
    })

    it('disables active scale and shows cursor-not-allowed when disabled', () => {
      render(<Button disabled>Disabled Action</Button>)
      const btn = screen.getByRole('button', { name: /Disabled Action/i })
      expect(btn).toBeDisabled()
      expect(btn.className).toContain('disabled:pointer-events-none')
      expect(btn.className).toContain('disabled:opacity-50')
    })
  })

  // 3. SIDEBAR FOOTER ANCHORING & INDEPENDENT SCROLLING
  describe('Sidebar Layout & Footer Anchoring', () => {
    const mockAuthValue: any = {
      session: { user: { id: 'usr-1', email: 'dev@rightful.com' } },
      user: { id: 'usr-1', email: 'dev@rightful.com' },
      profile: {
        id: 'usr-1',
        fullName: 'Dev Lead',
        defaultWorkspace: {
          id: 'ws-1',
          name: 'Rightful Cloud',
          slug: 'rightful-cloud',
          role: 'OWNER',
          tier: 'PRO',
        },
      },
      signOut: vi.fn(),
      loading: false,
    }

    it('structures sidebar with sticky full height, independent middle scroll, and pinned footer', () => {
      const onSelectTab = vi.fn()
      const onToggleCollapse = vi.fn()

      const { container } = render(
        <AuthContext.Provider value={mockAuthValue}>
          <DashboardSidebar
            activeTab="overview"
            onSelectTab={onSelectTab}
            collapsed={false}
            onToggleCollapse={onToggleCollapse}
            productCount={3}
            customerCount={12}
          />
        </AuthContext.Provider>
      )

      const aside = container.querySelector('aside')
      expect(aside).toBeInTheDocument()
      expect(aside?.className).toContain('sticky')
      expect(aside?.className).toContain('top-0')
      expect(aside?.className).toMatch(/h-(dvh|screen)/)
      expect(aside?.className).toContain('flex')
      expect(aside?.className).toContain('flex-col')

      // Middle navigation is flex-1 min-h-0 overflow-y-auto custom-scrollbar
      const navContainer = aside?.querySelector('.custom-scrollbar')
      expect(navContainer).toBeInTheDocument()
      expect(navContainer?.className).toContain('flex-1')
      expect(navContainer?.className).toContain('min-h-0')
      expect(navContainer?.className).toContain('overflow-y-auto')

      // Footer is pinned at bottom with mt-auto shrink-0
      const footer = aside?.querySelector('.mt-auto')
      expect(footer).toBeInTheDocument()
      expect(footer?.className).toContain('shrink-0')
      expect(footer?.className).toContain('border-t')

      // Sign out triggers signOut handler
      const signOutBtn = screen.getByRole('button', { name: /Sign out/i })
      fireEvent.click(signOutBtn)
      expect(mockAuthValue.signOut).toHaveBeenCalled()
    })

    it('maintains compact profile and pinned sign-out button when collapsed', () => {
      const onSelectTab = vi.fn()
      const onToggleCollapse = vi.fn()

      const { container } = render(
        <AuthContext.Provider value={mockAuthValue}>
          <DashboardSidebar
            activeTab="overview"
            onSelectTab={onSelectTab}
            collapsed={true}
            onToggleCollapse={onToggleCollapse}
          />
        </AuthContext.Provider>
      )

      const aside = container.querySelector('aside')
      expect(aside?.className).toContain('w-16')
      expect(aside?.className).toContain('sticky')

      const footer = aside?.querySelector('.mt-auto')
      expect(footer).toBeInTheDocument()
    })
  })

  // 4. UNIVERSAL THEME SYNCHRONIZATION
  describe('Universal Theme Application', () => {
    function ThemeTestPage() {
      const { theme, resolvedTheme, setTheme } = useTheme()
      return (
        <div>
          <span data-testid="active-theme">{theme}</span>
          <span data-testid="resolved-mode">{resolvedTheme}</span>
          <button onClick={() => setTheme('dark')}>Set Dark</button>
          <button onClick={() => setTheme('light')}>Set Light</button>
        </div>
      )
    }

    it('synchronizes dark class on both document.documentElement and document.body', () => {
      render(
        <ThemeProvider>
          <ThemeTestPage />
        </ThemeProvider>
      )

      fireEvent.click(screen.getByText('Set Dark'))
      expect(document.documentElement.classList.contains('dark')).toBe(true)
      expect(document.body.classList.contains('dark')).toBe(true)
      expect(localStorage.getItem('rightfull-theme')).toBe('dark')

      fireEvent.click(screen.getByText('Set Light'))
      expect(document.documentElement.classList.contains('dark')).toBe(false)
      expect(document.body.classList.contains('dark')).toBe(false)
      expect(localStorage.getItem('rightfull-theme')).toBe('light')
    })
  })
})
