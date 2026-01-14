'use client'

import { useState } from 'react'

import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { ScrollArea } from '@/components/ui/scroll-area'

import { Avatar, AvatarFallback } from '../ui/Avatar'
import { Button } from '../ui/Button'
import { Input } from '../ui/Input'

interface Message {
  id: string
  text: string
  author: 'me' | 'manager'
  createdAt: Date
}

export default function ChatUI() {
  const [messages, setMessages] = useState<Message[]>([
    {
      id: '1',
      text: 'Задайте вопрос по проекту',
      author: 'manager',
      createdAt: new Date()
    }
  ])

  const [value, setValue] = useState('')

  function sendMessage() {
    if (!value.trim()) return

    const myMessage: Message = {
      id: crypto.randomUUID(),
      text: value,
      author: 'me',
      createdAt: new Date()
    }

    setMessages(prev => [...prev, myMessage])
    setValue('')

    // фейковый ответ бота
    setTimeout(() => {
      setMessages(prev => [
        ...prev,
        {
          id: crypto.randomUUID(),
          text: 'Менеджер получил ваше сообщение и скоро ответит.',
          author: 'manager',
          createdAt: new Date()
        }
      ])
    }, 900)
  }

  return (
    <Card className='w-full max-w-xl h-150 flex flex-col shadow-xl rounded-2xl'>
      <CardHeader className='border-b'>
        <CardTitle>Чат с менеджером</CardTitle>
      </CardHeader>
      <CardContent className='flex-1 p-0 flex flex-col min-h-0'>
        <ScrollArea className='h-full px-4'>
          <div className='space-y-4'>
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
          />
          <Button onClick={sendMessage}>Отправить</Button>
        </div>
      </CardContent>
    </Card>
  )
}
