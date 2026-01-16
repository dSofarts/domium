import { API_URL } from '@/constants/site.constants'
import { getAccessToken } from '@/shared/auth/auth'

export async function apiFetch<T>(
  path: string,
  options: RequestInit & { parseAsText?: boolean } = {}
): Promise<T> {
  const token = getAccessToken()
  const headers = new Headers(options.headers)

  if (!headers.has('Content-Type') && options.body && !(options.body instanceof FormData)) {
    headers.set('Content-Type', 'application/json')
  }

  if (token) {
    headers.set('Authorization', `Bearer ${token}`)
  }

  const url = path.startsWith('http') ? path : `${API_URL}${path}`
  const response = await fetch(url, {
    ...options,
    headers
  })

  if (!response.ok) {
    const errorText = await response.text()
    throw new Error(errorText || `Request failed with ${response.status}`)
  }

  if (options.parseAsText) {
    return (await response.text()) as T
  }

  const contentType = response.headers.get('content-type') || ''
  if (contentType.includes('application/json')) {
    return (await response.json()) as T
  }

  return (await response.text()) as T
}
