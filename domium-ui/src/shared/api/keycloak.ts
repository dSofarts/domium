import {
  KEYCLOAK_CLIENT_ID,
  KEYCLOAK_CLIENT_SECRET,
  KEYCLOAK_REALM,
  KEYCLOAK_URL
} from '@/constants/site.constants'

export interface KeycloakTokenResponse {
  access_token: string
  expires_in: number
  refresh_expires_in: number
  refresh_token: string
  token_type: string
}

export interface KeycloakProfile {
  preferred_username?: string
  name?: string
  email?: string
}

export async function requestToken(
  username: string,
  password: string
): Promise<KeycloakTokenResponse> {
  const body = new URLSearchParams({
    grant_type: 'password',
    client_id: KEYCLOAK_CLIENT_ID,
    client_secret: KEYCLOAK_CLIENT_SECRET,
    scope: 'openid profile email',
    username,
    password
  })

  const response = await fetch(
    `${KEYCLOAK_URL}/realms/${KEYCLOAK_REALM}/protocol/openid-connect/token`,
    {
      method: 'POST',
      headers: {
        'Content-Type': 'application/x-www-form-urlencoded'
      },
      body
    }
  )

  if (!response.ok) {
    const errorText = await response.text()
    throw new Error(errorText || 'Ошибка авторизации')
  }

  return (await response.json()) as KeycloakTokenResponse
}

export async function fetchUserProfile(
  accessToken: string
): Promise<KeycloakProfile | null> {
  const response = await fetch(
    `${KEYCLOAK_URL}/realms/${KEYCLOAK_REALM}/protocol/openid-connect/userinfo`,
    {
      headers: {
        Authorization: `Bearer ${accessToken}`
      }
    }
  )

  if (!response.ok) {
    return null
  }

  return (await response.json()) as KeycloakProfile
}

export function decodeJwtPayload(accessToken: string): KeycloakProfile | null {
  try {
    const payload = accessToken.split('.')[1]
    const decoded = atob(payload.replace(/-/g, '+').replace(/_/g, '/'))
    return JSON.parse(decoded) as KeycloakProfile
  } catch {
    return null
  }
}

export function extractRoles(accessToken: string): string[] {
  try {
    const payload = accessToken.split('.')[1]
    const decoded = atob(payload.replace(/-/g, '+').replace(/_/g, '/'))
    const parsed = JSON.parse(decoded) as {
      realm_access?: { roles?: string[] }
      resource_access?: Record<string, { roles?: string[] }>
    }
    const roles = new Set<string>()
    parsed.realm_access?.roles?.forEach(role => roles.add(role.toUpperCase()))
    Object.values(parsed.resource_access || {}).forEach(resource => {
      resource.roles?.forEach(role => roles.add(role.toUpperCase()))
    })
    return Array.from(roles)
  } catch {
    return []
  }
}
