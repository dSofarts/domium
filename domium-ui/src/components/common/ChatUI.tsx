'use client'

import { useEffect, useMemo, useRef, useState } from 'react'
import { RSocketClient, encodeRoute } from 'rsocket-core'
import { Flowable } from 'rsocket-flowable'
import RSocketWebSocketClient from 'rsocket-websocket-client'

import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { ScrollArea } from '@/components/ui/scroll-area'

import { Avatar, AvatarFallback } from '../ui/Avatar'
import { Button } from '../ui/Button'
import { Input } from '../ui/Input'

import { useAuth } from '@/shared/auth/AuthProvider'
import { getAuthUserId } from '@/shared/auth/auth'
import { createChat } from '@/shared/api/chat'
import { CHAT_RSOCKET_URL } from '@/constants/site.constants'

interface Message {
  id: string
  text: string
  author: 'me' | 'manager'
  createdAt: Date
}

interface ApiMessage {
  id: string
  senderId: string
  content: string
  createdAt: string
}

interface ChatUIProps {
  projectId: string
  managerId: string
  managerName: string
}

export default function ChatUI({ projectId, managerId, managerName }: ChatUIProps) {
  const { user } = useAuth()
  const userId = useMemo(() => getAuthUserId(), [user])
  const [chatId, setChatId] = useState<string | null>(null)
  const [messages, setMessages] = useState<Message[]>([])
  const [value, setValue] = useState('')
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [collapsed, setCollapsed] = useState(true)
  const [socketReady, setSocketReady] = useState(false)
  const socketRef = useRef<any>(null)
  const connectCancelRef = useRef<(() => void) | null>(null)
  const streamCancelRef = useRef<(() => void) | null>(null)
  const decoderRef = useRef(new TextDecoder())
  const activeChatRef = useRef<string | null>(null)

  useEffect(() => {
    let mounted = true
    async function ensureChat() {
      if (!userId || !projectId) return
      setError('')
      setLoading(true)
      try {
        const created = await createChat({
          projectId,
          userName: user?.name || 'Пользователь',
          managerId,
          managerName
        })
        if (mounted) setChatId(created.chatId)
        if (mounted && created.lastMessage) {
          setMessages([
            {
              id: crypto.randomUUID(),
              text: created.lastMessage,
              author: 'manager',
              createdAt: new Date()
            }
          ])
        }
      } catch {
        if (mounted) setError('Не удалось создать чат.')
      } finally {
        if (mounted) setLoading(false)
      }
    }

    ensureChat()
    return () => {
      mounted = false
    }
  }, [projectId, managerId, managerName, user?.name, userId])

  useEffect(() => {
    if (!chatId || !userId) return
    const chatKey = `${chatId}:${userId}`
    if (activeChatRef.current === chatKey) return
    let alive = true
    activeChatRef.current = chatKey
    setSocketReady(false)

    if (socketRef.current) {
      socketRef.current.close()
      socketRef.current = null
    }
    connectCancelRef.current?.()
    streamCancelRef.current?.()

    const client = new RSocketClient({
      setup: {
        keepAlive: 20000,
        lifetime: 60000,
        dataMimeType: 'application/json',
        metadataMimeType: 'message/x.rsocket.routing.v0'
      },
      transport: new RSocketWebSocketClient({
        url: CHAT_RSOCKET_URL
      })
    })

    const connection = client.connect().subscribe({
      onComplete: socket => {
        if (!alive) {
          socket.close()
          return
        }
        socketRef.current = socket
        setSocketReady(true)
        loadHistory(socket, chatId, userId)
        subscribeToMessages(socket, chatId, userId)
      },
      onError: () => {
        if (alive) setError('Не удалось подключиться к чату.')
      }
    })

    connectCancelRef.current = () => {
      if (typeof connection?.cancel === 'function') {
        connection.cancel()
      }
    }

    return () => {
      alive = false
      activeChatRef.current = null
      connectCancelRef.current?.()
      streamCancelRef.current?.()
      if (socketRef.current) {
        socketRef.current.close()
        socketRef.current = null
      }
    }
  }, [chatId, userId])

  function sendMessage() {
    setError('')
    if (!value.trim()) return
    if (!socketRef.current || !chatId || !userId) {
      setError('Чат пока не подключен.')
      return
    }

    const content = value.trim()
    setValue('')
    socketRef.current
      .requestResponse({
        data: JSON.stringify({ chatId, userId, content }),
        metadata: encodeRoute('send')
      })
      .subscribe({
        onComplete: (payload: any) => {
          const parsed = parsePayload<ApiMessage>(payload)
          const createdAt = parsed?.createdAt ? new Date(parsed.createdAt) : new Date()
          const author: Message['author'] =
            parsed?.senderId === userId ? 'me' : 'manager'
          setMessages(prev => [
            ...prev,
            {
              id: parsed?.id || crypto.randomUUID(),
              text: parsed?.content || content,
              author,
              createdAt
            }
          ])
        },
        onError: () => {
          setError('Не удалось отправить сообщение.')
        }
      })
  }

  function parsePayload<T>(payload: { data?: unknown }) {
    if (!payload?.data) return null
    try {
      if (typeof payload.data === 'string') {
        return JSON.parse(payload.data) as T
      }
      if (payload.data instanceof Uint8Array) {
        const text = decoderRef.current.decode(payload.data)
        return JSON.parse(text) as T
      }
      if (payload.data instanceof ArrayBuffer) {
        const text = decoderRef.current.decode(new Uint8Array(payload.data))
        return JSON.parse(text) as T
      }
      return payload.data as T
    } catch {
      return null
    }
  }

  function loadHistory(
    socket: { requestResponse: (input: { data: unknown; metadata: unknown }) => Flowable<unknown> },
    chatId: string,
    userId: string
  ) {
    const now = new Date().toISOString().replace('Z', '')
    socket
      .requestResponse({
        data: JSON.stringify({
          userId,
          chatId,
          beforeTimestamp: now,
          limit: 50
        }),
        metadata: encodeRoute('loadHistory')
      })
      .subscribe({
        onComplete: (payload: any) => {
          const parsed = parsePayload<ApiMessage[]>(payload as { data?: unknown }) || []
          const mapped = parsed.map(item => {
            const author: Message['author'] =
              item.senderId === userId ? 'me' : 'manager'
            return {
              id: item.id,
              text: item.content,
              author,
              createdAt: new Date(item.createdAt)
            }
          })
            .sort((left, right) => left.createdAt.getTime() - right.createdAt.getTime())
          setMessages(mapped)
        },
        onError: () => {
          setError('Не удалось загрузить историю чата.')
        }
      })
  }

  function subscribeToMessages(
    socket: { requestStream: (input: { data: unknown; metadata: unknown }) => Flowable<unknown> },
    chatId: string,
    userId: string
  ) {
    const stream = socket.requestStream({
      data: JSON.stringify({ chatId, userId }),
      metadata: encodeRoute('subscribe')
    })
    const subscription = stream.subscribe({
      onNext: (payload: any) => {
        const parsed = parsePayload<ApiMessage>(payload as { data?: unknown })
        if (!parsed) return
        const author: Message['author'] =
          parsed.senderId === userId ? 'me' : 'manager'
        setMessages(prev => [
          ...prev,
          {
            id: parsed.id,
            text: parsed.content,
            author,
            createdAt: new Date(parsed.createdAt)
          }
        ])
      },
      onError: () => {
        setError('Не удалось получить сообщения чата.')
      }
    })
    streamCancelRef.current = () => {
      if (typeof subscription?.cancel === 'function') {
        subscription.cancel()
      }
    }
  }

  return (
    <Card className='w-full max-w-xl flex flex-col shadow-xl rounded-2xl'>
      <CardHeader className='border-b flex-row items-center justify-between'>
        <CardTitle>Чат с менеджером</CardTitle>
        <Button
          type='button'
          size='sm'
          variant='outline'
          onClick={() => setCollapsed(prev => !prev)}
        >
          {collapsed ? 'Развернуть' : 'Свернуть'}
        </Button>
      </CardHeader>
      {!collapsed && (
        <CardContent className='flex-1 p-0 flex flex-col min-h-0'>
          <ScrollArea className='h-full px-4'>
            <div className='space-y-4 py-4'>
              {loading && (
                <div className='text-sm text-muted-foreground'>Загружаем чат...</div>
              )}
              {error && <div className='text-sm text-destructive'>{error}</div>}
              {!loading && !messages.length && (
                <div className='text-sm text-muted-foreground'>
                  Начните диалог с менеджером проекта.
                </div>
              )}
              {messages.map(m => (
                <div
                  key={m.id}
                  className={`flex items-end gap-2 ${m.author === 'me' ? 'justify-end' : 'justify-start'}`}
                >
                  {m.author === 'manager' && (
                    <Avatar className='h-8 w-8'>
                      <AvatarFallback>М</AvatarFallback>
                    </Avatar>
                  )}

                  <div
                    className={`max-w-[70%] rounded-2xl px-4 py-2 text-sm shadow 
                    ${
                      m.author === 'me'
                        ? 'bg-primary text-primary-foreground rounded-br-sm'
                        : 'bg-muted rounded-bl-sm'
                    }`}
                  >
                    {m.text}
                  </div>

                  {m.author === 'me' && (
                    <Avatar className='h-8 w-8'>
                      <AvatarFallback>🧑</AvatarFallback>
                    </Avatar>
                  )}
                </div>
              ))}
            </div>
          </ScrollArea>

          <div className='mt-4 p-4 border-t flex gap-2'>
            <Input
              placeholder='Ваше сообщение...'
              value={value}
              onChange={e => setValue(e.target.value)}
              onKeyDown={e => e.key === 'Enter' && sendMessage()}
              disabled={!chatId || loading}
            />
            <Button onClick={sendMessage} disabled={!chatId || loading}>
              Отправить
            </Button>
          </div>
        </CardContent>
      )}
    </Card>
  )
}
