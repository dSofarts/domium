'use client'

import Image from 'next/image'
import Link from 'next/link'
import { useRouter } from 'next/navigation'
import { useState } from 'react'

import { Button } from '../ui/Button'

import { IPublicProject } from '@/shared/types/public-project.interface'
import { useAuth } from '@/shared/auth/AuthProvider'
import { extractRoles } from '@/shared/api/keycloak'
import { getAccessToken } from '@/shared/auth/auth'
import {
  addProjectFromPublic,
  storePendingOrder
} from '@/shared/projects/projects.store'
import { PAGES } from '@/config/pages.config'

interface ProjectProps {
  project: IPublicProject
}

export function PublicProjectCard({ project }: ProjectProps) {
  const router = useRouter()
  const { user } = useAuth()
  const isLocalImage = project.image.startsWith('http://localhost:9000')
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)
  const tokenRoles = useState(() => {
    const token = getAccessToken()
    return token ? extractRoles(token) : []
  })[0]
  const roles = user?.roles && user.roles.length > 0 ? user.roles : tokenRoles
  const canOrder =
    !user ||
    Boolean(
      roles.some(role => {
        const normalized = role.toUpperCase()
        return normalized === 'CLIENT' || normalized === 'ROLE_CLIENT'
      })
    )

  async function handleOrder() {
    setError('')
    if (user && !canOrder) {
      setError('Заказ доступен только клиентам.')
      return
    }

    if (!user) {
      storePendingOrder({
        projectId: project.id,
        name: '',
        phone: ''
      })
      router.push(`${PAGES.AUTH}?redirect=${PAGES.LK}`)
      return
    }

    setLoading(true)
    try {
      const created = await addProjectFromPublic(project, {
        name: user.name || 'Клиент',
        phone: user.phone || ''
      })
      router.push(PAGES.LK_PROJECT(created.id))
    } catch (err) {
      setError('Не удалось отправить заявку. Попробуйте позже.')
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className='bg-accent rounded-md overflow-hidden'>
      <div className='relative w-full aspect-video'>
        <Image
          src={project.image}
          alt={project.name}
          fill
          className='object-cover rounded-md'
          loading='lazy'
          unoptimized={isLocalImage}
        />
      </div>
      <div className='p-3'>
        <h2 className='font-bold'>{project.name}</h2>
        <div className='flex flex-wrap items-center justify-between gap-3 mt-5'>
          <div className='flex flex-wrap gap-2'>
            {canOrder ? (
              <Button onClick={handleOrder} disabled={loading}>
                Заказать
              </Button>
            ) : null}
            <Button asChild variant='outline'>
              <Link href={PAGES.PROJECT(project.id)}>Подробнее</Link>
            </Button>
          </div>
          {project.price && (
            <span className='font-medium text-sm block'>
              {project.price.toLocaleString('ru-RU')} ₽
            </span>
          )}
        </div>
        {error && <p className='text-sm text-destructive mt-2'>{error}</p>}
      </div>
    </div>
  )
}
