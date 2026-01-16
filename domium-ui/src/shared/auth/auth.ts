import type { IUser } from '@/shared/types/user.interface'

export const AUTH_COOKIE_NAME = 'domium_auth'
const AUTH_USER_KEY = 'domium_auth_user'
const AUTH_TOKEN_KEY = 'domium_auth_token'
const AUTH_REFRESH_TOKEN_KEY = 'domium_auth_refresh_token'
const AUTH_TOKEN_EXPIRES_KEY = 'domium_auth_token_expires'

export interface AuthUser extends IUser {
  phone?: string
}

export interface AuthTokens {
  accessToken: string
  refreshToken?: string
  expiresAt?: number
}

export function readAuthUser(): AuthUser | null {
  if (typeof window === 'undefined') {
    return null
  }

  const raw = window.localStorage.getItem(AUTH_USER_KEY)
  if (!raw) return null

  try {
    return JSON.parse(raw) as AuthUser
  } catch {
    return null
  }
}

export function getAccessToken() {
  if (typeof window === 'undefined') return null
  const token = window.localStorage.getItem(AUTH_TOKEN_KEY)
  if (!token) return null

  const expiresAt = readTokenExpiresAt()
  if (expiresAt && Date.now() >= expiresAt) {
    clearAuthSession()
    return null
  }

  return token
}

export function getAuthUserId() {
  const token = getAccessToken()
  if (!token) return null

  try {
    const payload = token.split('.')[1]
    const decoded = atob(payload.replace(/-/g, '+').replace(/_/g, '/'))
    const parsed = JSON.parse(decoded) as { sub?: string }
    return parsed.sub || null
  } catch {
    return null
  }
}

export function setAuthSession(user: AuthUser, tokens?: AuthTokens) {
  if (typeof window === 'undefined') return

  window.localStorage.setItem(AUTH_USER_KEY, JSON.stringify(user))
  if (tokens?.accessToken) {
    window.localStorage.setItem(AUTH_TOKEN_KEY, tokens.accessToken)
    if (tokens.refreshToken) {
      window.localStorage.setItem(AUTH_REFRESH_TOKEN_KEY, tokens.refreshToken)
    }
    if (tokens.expiresAt) {
      window.localStorage.setItem(AUTH_TOKEN_EXPIRES_KEY, String(tokens.expiresAt))
    }
    document.cookie = `${AUTH_COOKIE_NAME}=1; path=/; max-age=2592000`
  }
}

export function clearAuthSession() {
  if (typeof window === 'undefined') return

  window.localStorage.removeItem(AUTH_USER_KEY)
  window.localStorage.removeItem(AUTH_TOKEN_KEY)
  window.localStorage.removeItem(AUTH_REFRESH_TOKEN_KEY)
  window.localStorage.removeItem(AUTH_TOKEN_EXPIRES_KEY)
  document.cookie = `${AUTH_COOKIE_NAME}=; path=/; max-age=0`
}

function readTokenExpiresAt() {
  if (typeof window === 'undefined') return null
  const raw = window.localStorage.getItem(AUTH_TOKEN_EXPIRES_KEY)
  if (!raw) return null
  const parsed = Number(raw)
  return Number.isFinite(parsed) ? parsed : null
}
