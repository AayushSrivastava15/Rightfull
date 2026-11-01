import { createClient } from '@supabase/supabase-js'

const supabaseUrl =
  import.meta.env['VITE_SUPABASE_URL'] || 'https://kcyccddrzsfxgrkeenop.supabase.co'
const supabaseAnonKey =
  import.meta.env['VITE_SUPABASE_ANON_KEY'] || 'placeholder-anon-key'

if (typeof window !== 'undefined' && !import.meta.env['VITE_SUPABASE_ANON_KEY']) {
  console.warn(
    '[Rightful] VITE_SUPABASE_ANON_KEY is not defined. Please configure frontend/.env.local with your Supabase Anon Key.'
  )
}

export const supabase = createClient(supabaseUrl, supabaseAnonKey, {
  auth: {
    persistSession: true,
    autoRefreshToken: true,
    detectSessionInUrl: true,
  },
})

