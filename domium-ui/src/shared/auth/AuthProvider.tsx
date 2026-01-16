'use client'

import React, { createContext, useContext, useEffect, useMemo, useState } from 'react'

import {
  AUTH_COOKIE_NAME,
  clearAuthSession,
  getAccessToken,
  readAuthUser,
  setAuthSession,
  type AuthTokens,
  type AuthUser
} from './auth'
import { extractRoles } from '@/shared/api/keycloak'

interface AuthContextValue {
  user: AuthUser | null
  login: (user: AuthUser, tokens?: AuthTokens) => void
  logout: () => void
  refresh: () => void
}

const AuthContext = createContext<AuthContextValue | null>(null)

export function AuthProvider({ children }: { children: React.ReactNode }) {
  const [user, setUser] = useState<AuthUser | null>(readAuthUser())

  useEffect(() => {
    if (!user) return
    const token = getAccessToken()
    if (!token) {
      setUser(null)
      return
    }
    if (!user.roles || user.roles.length === 0) {
      const roles = extractRoles(token)
      if (roles.length > 0) {
        const nextUser = { ...user, roles }
        setAuthSession(nextUser)
        setUser(nextUser)
        return
      }
    }
    const hasCookie = document.cookie
      .split('; ')
      .some(entry => entry.startsWith(`${AUTH_COOKIE_NAME}=`))
    if (!hasCookie) {
      document.cookie = `${AUTH_COOKIE_NAME}=1; path=/; max-age=2592000`
    }
  }, [user])

  function login(nextUser: AuthUser, tokens?: AuthTokens) {
    setAuthSession(nextUser, tokens)
    setUser(nextUser)
  }

  function logout() {
    clearAuthSession()
    setUser(null)
  }

  function refresh() {
    setUser(readAuthUser())
  }

  const value = useMemo(
    () => ({
      user,
      login,
      logout,
      refresh
    }),
    [user]
  )

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

export function useAuth() {
  const ctx = useContext(AuthContext)
  if (!ctx) {
    throw new Error('useAuth must be used within AuthProvider')
  }
  return ctx
}
