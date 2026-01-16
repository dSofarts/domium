'use client'

import { useEffect, useMemo, useState } from 'react'

import { PublicProjectCard } from './PublicProjectCard'
import { Button } from '../ui/Button'

import { useAuth } from '@/shared/auth/AuthProvider'
import { getPublicProjects } from '@/shared/api/projects'
import { extractRoles } from '@/shared/api/keycloak'
import { getAccessToken } from '@/shared/auth/auth'
import { PAGES } from '@/config/pages.config'
import type { IPublicProject } from '@/shared/types/public-project.interface'

export function PublicProjects() {
  const [projects, setProjects] = useState<IPublicProject[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const { user } = useAuth()
  const tokenRoles = useMemo(() => {
    const token = getAccessToken()
    return token ? extractRoles(token) : []
  }, [user])
  const roles = user?.roles && user.roles.length > 0 ? user.roles : tokenRoles
  const canCreate = Boolean(
    roles.some(role => {
      const normalized = role.toUpperCase()
      return normalized === 'MANAGER' || normalized === 'ROLE_MANAGER'
    })
  )

  useEffect(() => {
    let mounted = true
    async function load() {
      try {
        const data = await getPublicProjects()
        if (mounted) setProjects(data)
      } catch (err) {
        if (mounted) setError('Не удалось загрузить проекты.')
      } finally {
        if (mounted) setLoading(false)
      }
    }

    load()
    return () => {
      mounted = false
    }
  }, [])

  return (
    <div>
      <div className='flex flex-wrap items-center justify-between gap-3'>
        <h1 className='text-xl font-medium'>Наши проекты</h1>
        {canCreate ? (
          <Button asChild>
            <a href={PAGES.CREATE_PROJECT}>Добавить проект</a>
          </Button>
        ) : null}
      </div>
      {loading && (
        <div className='mt-4 rounded-xl bg-muted p-4 text-sm text-muted-foreground'>
          Загружаем проекты...
        </div>
      )}
      {error && (
        <div className='mt-4 rounded-xl bg-muted p-4 text-sm text-destructive'>
          {error}
        </div>
      )}
      {!loading && !error && (
        <div className='mt-4 grid gap-5 grid-cols-1 sm:grid-cols-2 xl:grid-cols-4'>
          {projects.map(project => {
            return <PublicProjectCard key={project.id} project={project} />
          })}
        </div>
      )}
    </div>
  )
}
