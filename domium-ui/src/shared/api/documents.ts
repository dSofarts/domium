import { API_URL } from '@/constants/site.constants'
import { getAccessToken } from '@/shared/auth/auth'
import { apiFetch } from './client'

export interface ApiDocumentGroup {
  id?: string
  type?: string
  title?: string
  rootDocumentId?: string
}

export interface ApiDocumentTemplate {
  id?: string
  code?: string
  name?: string
  required?: boolean
  stageCode?: string
}

export interface ApiDocumentInstance {
  id: string
  projectId: string
  userId?: string
  status: string
  title?: string
  createdAt?: string
  sentAt?: string
  viewedAt?: string
  signedAt?: string
  rejectedAt?: string
  dueDate?: string
  group?: ApiDocumentGroup
  template?: ApiDocumentTemplate
}

export interface ApiDocumentSignature {
  id: string
  documentId: string
  signerUserId: string
  signerType?: string
  type?: string
  signedAt?: string
  signaturePayloadJson?: unknown
  fileHash?: string
}

export interface ApiDocumentDetails {
  document: ApiDocumentInstance
  signatures?: ApiDocumentSignature[]
}

export async function listProjectDocuments(
  projectId: string,
  groupType?: string,
  stageId?: string
) {
  const params = new URLSearchParams()
  if (groupType) params.set('groupType', groupType)
  if (stageId) params.set('stage', stageId)
  const query = params.toString()
  const suffix = query ? `?${query}` : ''
  return apiFetch<ApiDocumentInstance[]>(
    `/api/document-service/projects/${projectId}/documents${suffix}`
  )
}

export async function getDocumentDetails(documentId: string) {
  return apiFetch<ApiDocumentDetails>(`/api/document-service/${documentId}`)
}

export async function uploadStageDocument(input: {
  projectId: string
  stageId: string
  recipientId: string
  file: File
  title?: string
  kind?: 'STAGE' | 'PHOTO'
}) {
  const body = new FormData()
  body.append('file', input.file)
  body.append('stageId', input.stageId)
  body.append('recipientId', input.recipientId)
  if (input.title) body.append('title', input.title)
  if (input.kind) body.append('kind', input.kind)
  return apiFetch<ApiDocumentInstance>(
    `/api/document-service/projects/${input.projectId}/documents/upload`,
    {
      method: 'POST',
      body
    }
  )
}

export function getDocumentFileUrl(documentId: string, options?: { download?: boolean }) {
  const params = new URLSearchParams()
  params.set('markViewed', 'true')
  if (options?.download) params.set('download', 'true')
  return `${API_URL}/api/document-service/${documentId}/file?${params.toString()}`
}

export async function downloadDocumentFile(documentId: string) {
  const token = getAccessToken()
  const response = await fetch(
    `${API_URL}/api/document-service/${documentId}/file-raw?markViewed=true`,
    {
      method: 'GET',
      headers: token ? { Authorization: `Bearer ${token}` } : undefined
    }
  )
  if (!response.ok) {
    const text = await response.text()
    throw new Error(text || 'Failed to download document')
  }
  const blob = await response.blob()
  const contentType = response.headers.get('content-type') || 'application/octet-stream'
  return { blob, contentType }
}

export async function fetchDocumentFilePublicUrl(documentId: string) {
  const response = await apiFetch<{ url: string }>(
    `/api/document-service/${documentId}/file-url`
  )
  const rawUrl = response.url
  if (typeof window === 'undefined') return rawUrl
  try {
    const parsed = new URL(rawUrl)
    if (parsed.hostname === 'minio') {
      parsed.hostname = window.location.hostname
      if (!parsed.port) parsed.port = '9000'
      return parsed.toString()
    }
  } catch {
    // ignore invalid URL
  }
  return rawUrl
}

export async function fetchDocumentImageObjectUrl(documentId: string) {
  const token = getAccessToken()
  const response = await fetch(
    `${API_URL}/api/document-service/${documentId}/file-raw`,
    {
      method: 'GET',
      headers: token ? { Authorization: `Bearer ${token}` } : undefined
    }
  )
  if (!response.ok) {
    const text = await response.text()
    throw new Error(text || 'Failed to load image')
  }
  const blob = await response.blob()
  return URL.createObjectURL(blob)
}

export async function signDocument(
  documentId: string,
  confirmationCode = '000000'
) {
  return apiFetch(`/api/document-service/${documentId}/sign`, {
    method: 'POST',
    body: JSON.stringify({
      signatureType: 'SIMPLE',
      confirmationCode
    })
  })
}

export async function rejectDocument(documentId: string, comment?: string) {
  return apiFetch(`/api/document-service/${documentId}/reject`, {
    method: 'POST',
    body: JSON.stringify({
      comment: comment || ''
    })
  })
}
