'use client'

import { useMemo, useState } from 'react'
import { useRouter, useSearchParams } from 'next/navigation'

import { Button } from '@/components/ui/Button'
import { Input } from '@/components/ui/Input'
import {
  Field,
  FieldContent,
  FieldLabel,
  FieldSet,
  FieldTitle
} from '@/components/ui/Field'
import { useAuth } from '@/shared/auth/AuthProvider'
import {
  addProjectFromPublic,
  clearPendingOrder,
  readPendingOrder
} from '@/shared/projects/projects.store'
import {
  decodeJwtPayload,
  extractRoles,
  fetchUserProfile,
  requestToken
} from '@/shared/api/keycloak'
import { getPublicProjectById } from '@/shared/api/projects'

export default function AuthClient() {
  const router = useRouter()
  const searchParams = useSearchParams()
  const redirect = searchParams.get('redirect') || '/lk/projects'

  const { login } = useAuth()
  const [username, setUsername] = useState('test-client')
  const [password, setPassword] = useState('test-client')
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')

  const pendingOrder = useMemo(() => readPendingOrder(), [])

  async function handleSubmit(event: React.FormEvent) {
    event.preventDefault()
    setError('')
    if (!username.trim() || !password.trim()) return

    setLoading(true)
    try {
      const tokens = await requestToken(username.trim(), password.trim())
      const profile =
        (await fetchUserProfile(tokens.access_token)) ||
        decodeJwtPayload(tokens.access_token)

      const user = {
        name: profile?.name || profile?.preferred_username || username.trim(),
        email: profile?.email || `${username.trim()}@domium.test`,
        avatar: '',
        roles: extractRoles(tokens.access_token)
      }

      login(user, {
        accessToken: tokens.access_token,
        refreshToken: tokens.refresh_token,
        expiresAt: Date.now() + tokens.expires_in * 1000
      })

      if (pendingOrder) {
        const project = await getPublicProjectById(pendingOrder.projectId)
        if (project) {
          const created = await addProjectFromPublic(project, {
            name: pendingOrder.name || user.name,
            phone: pendingOrder.phone || ''
          })
          clearPendingOrder()
          router.push(`/lk/projects/${created.id}`)
          return
        }
        clearPendingOrder()
      }

      router.push(redirect)
    } catch (err) {
      setError('Не удалось войти. Проверьте логин и пароль.')
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className='mx-auto w-full max-w-md mt-10'>
      <h1 className='text-2xl font-semibold mb-2'>Вход в личный кабинет</h1>
      <p className='text-muted-foreground text-sm mb-6'>
        Авторизуйтесь, чтобы отправить заявку и отслеживать строительство.
      </p>

      <form onSubmit={handleSubmit} className='space-y-5'>
        <FieldSet>
          <Field>
            <FieldLabel>
              <FieldTitle>Логин</FieldTitle>
            </FieldLabel>
            <FieldContent>
              <Input
                placeholder='test-client'
                value={username}
                onChange={event => setUsername(event.target.value)}
              />
            </FieldContent>
          </Field>
          <Field>
            <FieldLabel>
              <FieldTitle>Пароль</FieldTitle>
            </FieldLabel>
            <FieldContent>
              <Input
                type='password'
                placeholder='test-client'
                value={password}
                onChange={event => setPassword(event.target.value)}
              />
            </FieldContent>
          </Field>
        </FieldSet>
        {error && <p className='text-sm text-destructive'>{error}</p>}
        <Button type='submit' className='w-full' disabled={loading}>
          Войти
        </Button>
        <p className='text-xs text-muted-foreground'>
          Демо-доступ: test-client / test-client, test-manager / test-manager
        </p>
      </form>
    </div>
  )
}
