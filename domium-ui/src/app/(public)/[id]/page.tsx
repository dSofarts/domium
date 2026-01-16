'use client'

import Image from 'next/image'
import Link from 'next/link'
import { useParams, useRouter } from 'next/navigation'
import { useEffect, useMemo, useState } from 'react'

import { Button } from '@/components/ui/Button'
import { PAGES } from '@/config/pages.config'
import { getPublicProjectById } from '@/shared/api/projects'
import { useAuth } from '@/shared/auth/AuthProvider'
import { extractRoles } from '@/shared/api/keycloak'
import { getAccessToken } from '@/shared/auth/auth'
import {
  addProjectFromPublic,
  storePendingOrder
} from '@/shared/projects/projects.store'
import type { IPublicProject } from '@/shared/types/public-project.interface'

export default function PublicProjectPage() {
  const params = useParams()
  const projectId = params?.id as string
  const router = useRouter()
  const { user } = useAuth()
  const [project, setProject] = useState<IPublicProject | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [orderError, setOrderError] = useState('')
  const [orderLoading, setOrderLoading] = useState(false)

  useEffect(() => {
    let mounted = true
    async function load() {
      try {
        const data = await getPublicProjectById(projectId)
        if (mounted) setProject(data)
      } catch (err) {
        if (mounted) setError('Не удалось загрузить проект.')
      } finally {
        if (mounted) setLoading(false)
      }
    }

    if (projectId) {
      load()
    }

    return () => {
      mounted = false
    }
  }, [projectId])

  const tokenRoles = useMemo(() => {
    const token = getAccessToken()
    return token ? extractRoles(token) : []
  }, [user])
  const roles = user?.roles && user.roles.length > 0 ? user.roles : tokenRoles
  const canOrder =
    !user ||
    Boolean(
      roles.some(role => {
        const normalized = role.toUpperCase()
        return normalized === 'CLIENT' || normalized === 'ROLE_CLIENT'
      })
    )

  if (loading) {
    return (
      <div className='rounded-xl bg-muted p-6 text-sm text-muted-foreground'>
        Загружаем проект...
      </div>
    )
  }

  if (error) {
    return (
      <div className='rounded-xl bg-muted p-6 text-sm text-destructive'>
        {error}
      </div>
    )
  }

  if (!project) {
    return (
      <div className='rounded-xl bg-muted p-6 text-sm text-muted-foreground'>
        Проект не найден.
      </div>
    )
  }

  const isLocalImage = project.image.startsWith('http://localhost:9000')
  const floorsCount = project.floors?.length || 0
  const roomsCount =
    project.floors?.reduce((total, floor) => total + (floor.rooms?.length || 0), 0) || 0
  const totalArea =
    project.floors?.reduce((total, floor) => {
      const floorArea =
        floor.rooms?.reduce((roomTotal, room) => roomTotal + (room.area || 0), 0) || 0
      return total + floorArea
    }, 0) || 0
  const gallery = project.images && project.images.length > 0 ? project.images : [project.image]

  async function handleOrder() {
    setOrderError('')
    if (!project) return
    if (user && !canOrder) {
      setOrderError('Заказ доступен только клиентам.')
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

    setOrderLoading(true)
    try {
      const created = await addProjectFromPublic(project, {
        name: user.name || 'Клиент',
        phone: user.phone || ''
      })
      router.push(PAGES.LK_PROJECT(created.id))
    } catch {
      setOrderError('Не удалось отправить заявку. Попробуйте позже.')
    } finally {
      setOrderLoading(false)
    }
  }

  return (
    <div className='grid gap-6'>
      <div>
        <Button asChild variant='outline'>
          <Link href={PAGES.HOME}>Вернуться к списку</Link>
        </Button>
      </div>
      <div className='grid gap-6 lg:grid-cols-[1.4fr_1fr]'>
        <div className='grid gap-3'>
          <div className='relative w-full overflow-hidden rounded-2xl border bg-background'>
            <div className='relative w-full aspect-video'>
              <Image
                src={project.image}
                alt={project.name}
                fill
                className='object-cover'
                loading='lazy'
                unoptimized={isLocalImage}
              />
            </div>
          </div>
          {gallery.length > 1 ? (
            <div className='grid grid-cols-3 gap-2'>
              {gallery.map((imageUrl, index) => (
                <div
                  key={`${imageUrl}-${index}`}
                  className='relative aspect-video overflow-hidden rounded-xl border bg-background'
                >
                  <Image
                    src={imageUrl}
                    alt={`${project.name} фото ${index + 1}`}
                    fill
                    className='object-cover'
                    loading='lazy'
                    unoptimized={imageUrl.startsWith('http://localhost:9000')}
                  />
                </div>
              ))}
            </div>
          ) : null}
        </div>
        <div className='grid gap-4'>
          <div>
            <h1 className='text-2xl font-semibold'>{project.name}</h1>
            <p className='mt-2 text-sm text-muted-foreground'>
              {project.description}
            </p>
          </div>
          {project.price ? (
            <div className='rounded-xl border p-4'>
              <div className='text-sm text-muted-foreground'>Стоимость</div>
              <div className='text-2xl font-semibold'>
                {project.price.toLocaleString('ru-RU')} ₽
              </div>
            </div>
          ) : null}
          {canOrder ? (
            <Button onClick={handleOrder} disabled={orderLoading}>
              Заказать
            </Button>
          ) : null}
          {orderError ? (
            <p className='text-sm text-destructive'>{orderError}</p>
          ) : null}
          <div className='rounded-xl border p-4 text-sm text-muted-foreground'>
            <div>Тип: {project.type || '—'}</div>
            <div>Категория: {project.category || '—'}</div>
            <div>Материал: {project.material || '—'}</div>
            <div>Локация: {project.location || '—'}</div>
            <div>Этажей: {floorsCount || '—'}</div>
            <div>Комнат: {roomsCount || '—'}</div>
            <div>
              Общая площадь:{' '}
              {totalArea > 0 ? `${totalArea.toLocaleString('ru-RU')} м²` : '—'}
            </div>
          </div>
          {project.floors && project.floors.length > 0 ? (
            <div className='rounded-xl border p-4'>
              <div className='text-sm font-medium'>Планировка</div>
              <div className='mt-3 grid gap-3'>
                {project.floors.map(floor => (
                  <div key={floor.id} className='rounded-lg border p-3'>
                    <div className='text-sm font-medium'>
                      Этаж {floor.floorNumber ?? '—'}
                    </div>
                    {floor.rooms && floor.rooms.length > 0 ? (
                      <div className='mt-2 grid gap-2 text-sm text-muted-foreground'>
                        {floor.rooms.map(room => (
                          <div
                            key={room.id}
                            className='flex flex-wrap items-center justify-between gap-2'
                          >
                            <span>{room.roomType || 'Комната'}</span>
                            <span>
                              {room.area ? `${room.area.toLocaleString('ru-RU')} м²` : '—'}
                            </span>
                          </div>
                        ))}
                      </div>
                    ) : (
                      <div className='mt-2 text-sm text-muted-foreground'>
                        Комнаты не указаны
                      </div>
                    )}
                  </div>
                ))}
              </div>
            </div>
          ) : null}
        </div>
      </div>
    </div>
  )
}
