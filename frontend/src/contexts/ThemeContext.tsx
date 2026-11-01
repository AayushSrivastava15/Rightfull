import React, { createContext, useContext, useEffect, useState } from 'react'

export type Theme = 'light' | 'dark' | 'system'
export type ResolvedTheme = 'light' | 'dark'

interface ThemeContextType {
  theme: Theme
  resolvedTheme: ResolvedTheme
  setTheme: (theme: Theme) => void
}

const STORAGE_KEY = 'rightfull-theme'

const ThemeContext = createContext<ThemeContextType | undefined>(undefined)

function getSystemTheme(): ResolvedTheme {
  if (typeof window === 'undefined') return 'dark'
  return window.matchMedia('(prefers-color-scheme: dark)').matches ? 'dark' : 'light'
}

export function ThemeProvider({ children }: { children: React.ReactNode }) {
  const [theme, setThemeState] = useState<Theme>(() => {
    if (typeof window === 'undefined') return 'system'
    try {
      const stored = localStorage.getItem(STORAGE_KEY)
      if (stored === 'light' || stored === 'dark' || stored === 'system') {
        return stored
      }
    } catch {
      // localStorage may be disabled or restricted in privacy mode
    }
    return 'system'
  })

  const [resolvedTheme, setResolvedTheme] = useState<ResolvedTheme>(() => {
    if (theme === 'system') return getSystemTheme()
    return theme
  })

  // Apply theme class to document root and body for portal synchronization
  useEffect(() => {
    const root = document.documentElement
    const body = document.body
    const effectiveTheme = theme === 'system' ? getSystemTheme() : theme
    setResolvedTheme(effectiveTheme)

    if (effectiveTheme === 'dark') {
      root.classList.add('dark')
      body?.classList.add('dark')
    } else {
      root.classList.remove('dark')
      body?.classList.remove('dark')
    }
  }, [theme])

  // Listen for operating system preference changes when in 'system' mode
  useEffect(() => {
    if (typeof window === 'undefined') return

    const mediaQuery = window.matchMedia('(prefers-color-scheme: dark)')
    const handleChange = () => {
      if (theme === 'system') {
        const newSystemTheme = mediaQuery.matches ? 'dark' : 'light'
        setResolvedTheme(newSystemTheme)
        if (newSystemTheme === 'dark') {
          document.documentElement.classList.add('dark')
          document.body?.classList.add('dark')
        } else {
          document.documentElement.classList.remove('dark')
          document.body?.classList.remove('dark')
        }
      }
    }

    mediaQuery.addEventListener('change', handleChange)
    return () => mediaQuery.removeEventListener('change', handleChange)
  }, [theme])

  const setTheme = (newTheme: Theme) => {
    setThemeState(newTheme)
    try {
      localStorage.setItem(STORAGE_KEY, newTheme)
    } catch (e) {
      console.warn('Failed to persist theme in localStorage:', e)
    }
  }

  return (
    <ThemeContext.Provider value={{ theme, resolvedTheme, setTheme }}>
      {children}
    </ThemeContext.Provider>
  )
}

export function useTheme() {
  const context = useContext(ThemeContext)
  if (!context) {
    throw new Error('useTheme must be used within a ThemeProvider')
  }
  return context
}

