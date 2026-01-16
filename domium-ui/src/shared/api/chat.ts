import { apiFetch } from './client'
import { getAuthUserId } from '@/shared/auth/auth'

export interface ChatResponse {
  chatId: string
  projectId: string
  lastMessage?: string | null
}

export async function getChatByProject(projectId: string) {
  const userId = getAuthUserId()
  if (!userId) {
    throw new Error('Не найден userId для чата')
  }
  return apiFetch<ChatResponse>(`/api/chat-service/chat/${projectId}`, {
    headers: {
      'X-User-Id': userId
    }
  })
}

export async function createChat(params: {
  projectId: string
  userName: string
  managerId: string
  managerName: string
}) {
  const userId = getAuthUserId()
  if (!userId) {
    throw new Error('Не найден userId для чата')
  }
  return apiFetch<ChatResponse>(`/api/chat-service/chat/create`, {
    method: 'POST',
    headers: {
      'X-User-Id': userId
    },
    body: JSON.stringify(params)
  })
}
