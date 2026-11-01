import {
  createContext,
  useContext,
  useEffect,
  useState,
  useCallback,
  type ReactNode,
} from 'react'
import type { User, Session, AuthError } from '@supabase/supabase-js'
import { supabase } from '@/lib/supabaseClient'
import { apiClient } from '@/services/apiClient'
import type { UserProfile } from '@/types'

interface AuthContextType {
  user: User | null
  session: Session | null
  profile: UserProfile | null
  loading: boolean
  error: string | null
  signInWithEmail: (email: string, password: string) => Promise<{ error: AuthError | null }>
  signUpWithEmail: (
    email: string,
    password: string,
    fullName?: string
  ) => Promise<{ error: AuthError | null }>
  signInWithOAuth: (provider: 'google' | 'github') => Promise<{ error: AuthError | null }>
  signOut: () => Promise<void>
  refreshProfile: () => Promise<void>
}

export const AuthContext = createContext<AuthContextType | undefined>(undefined)

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<User | null>(null)
  const [session, setSession] = useState<Session | null>(null)
  const [profile, setProfile] = useState<UserProfile | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  const fetchProfile = useCallback(async () => {
    try {
      const data = await apiClient.getUserProfile()
      setProfile(data)
      setError(null)
    } catch (err: unknown) {
      console.warn('[AuthContext] Could not load user profile from backend:', err)
    }
  }, [])

  useEffect(() => {
    if (typeof window === 'undefined') {
      setLoading(false)
      return
    }

    // 1. Initial Session Restoration
    supabase.auth.getSession().then(({ data: { session } }) => {
      setSession(session)
      setUser(session?.user ?? null)
      if (session) {
        fetchProfile().finally(() => setLoading(false))
      } else {
        setLoading(false)
      }
    })

    // 2. Auth State Change Listener
    const {
      data: { subscription },
    } = supabase.auth.onAuthStateChange(async (event, newSession) => {
      setSession(newSession)
      setUser(newSession?.user ?? null)

      if (event === 'SIGNED_IN' || event === 'TOKEN_REFRESHED') {
        if (newSession) {
          await fetchProfile()
        }
      } else if (event === 'SIGNED_OUT') {
        setProfile(null)
        setUser(null)
        setSession(null)
      }
      setLoading(false)
    })

    return () => {
      subscription.unsubscribe()
    }
  }, [fetchProfile])

  const signInWithEmail = async (email: string, password: string) => {
    setError(null)
    const { data, error } = await supabase.auth.signInWithPassword({
      email,
      password,
    })
    if (error) {
      setError(error.message)
      return { error }
    }
    setSession(data.session)
    setUser(data.user)
    if (data.session) {
      await fetchProfile()
    }
    return { error: null }
  }

  const signUpWithEmail = async (
    email: string,
    password: string,
    fullName?: string
  ) => {
    setError(null)
    const { data, error } = await supabase.auth.signUp({
      email,
      password,
      options: {
        data: {
          full_name: fullName,
          name: fullName,
        },
      },
    })
    if (error) {
      setError(error.message)
      return { error }
    }
    setSession(data.session)
    setUser(data.user)
    if (data.session) {
      await fetchProfile()
    }
    return { error: null }
  }

  const signInWithOAuth = async (provider: 'google' | 'github') => {
    setError(null)
    const { error } = await supabase.auth.signInWithOAuth({
      provider,
      options: {
        ...(typeof window !== 'undefined' ? { redirectTo: window.location.origin } : {}),
      },
    })
    if (error) {
      setError(error.message)
      return { error }
    }
    return { error: null }
  }

  const signOut = async () => {
    await supabase.auth.signOut()
    setUser(null)
    setSession(null)
    setProfile(null)
  }

  return (
    <AuthContext.Provider
      value={{
        user,
        session,
        profile,
        loading,
        error,
        signInWithEmail,
        signUpWithEmail,
        signInWithOAuth,
        signOut,
        refreshProfile: fetchProfile,
      }}
    >
      {children}
    </AuthContext.Provider>
  )
}

export function useAuth() {
  const context = useContext(AuthContext)
  if (!context) {
    throw new Error('useAuth must be used within an AuthProvider')
  }
  return context
}

