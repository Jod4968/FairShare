import { createContext, useContext, useMemo, useState, type ReactNode } from 'react'
import { authApi } from '../api/client'

type AuthContextValue = {
  token: string | null
  isAuthenticated: boolean
  signIn: (email: string, password: string) => Promise<void>
  register: (name: string, email: string, password: string) => Promise<void>
  signOut: () => void
}

const TOKEN_KEY = 'fairshare_token'
const AuthContext = createContext<AuthContextValue | undefined>(undefined)

export function AuthProvider({ children }: { children: ReactNode }) {
  const [token, setToken] = useState(() => localStorage.getItem(TOKEN_KEY))

  const saveToken = (value: string) => {
    localStorage.setItem(TOKEN_KEY, value)
    setToken(value)
  }

  const value = useMemo<AuthContextValue>(() => ({
    token,
    isAuthenticated: Boolean(token),
    async signIn(email, password) {
      const response = await authApi.login(email, password)
      saveToken(response.token)
    },
    async register(name, email, password) {
      const response = await authApi.register(name, email, password)
      saveToken(response.token)
    },
    signOut() {
      localStorage.removeItem(TOKEN_KEY)
      setToken(null)
    },
  }), [token])

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

export function useAuth() {
  const context = useContext(AuthContext)
  if (!context) throw new Error('useAuth must be used inside AuthProvider')
  return context
}
