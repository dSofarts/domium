'use client'

import { useEffect, useMemo, useState } from 'react'

import { ProjectCard } from '@/components/common/ProjectCard'
import { loadProjects } from '@/shared/projects/projects.store'
import { useAuth } from '@/shared/auth/AuthProvider'
import { extractRoles } from '@/shared/api/keycloak'
import { getAccessToken, getAuthUserId } from '@/shared/auth/auth'
import type { IProject } from '@/shared/types/project.interface'

export default function ProjectsPage() {
  const [mounted, setMounted] = useState(false)
  const [projects, setProjects] = useState<IProject[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const { user } = useAuth()
  const tokenRoles = useMemo(() => {
    const token = getAccessToken()
    return token ? extractRoles(token) : []
  }, [user])
  const roles = user?.roles && user.roles.length > 0 ? user.roles : tokenRoles
  const userId = useMemo(() => getAuthUserId(), [user])
  const isManager = roles.some(role => {
    const normalized = role.toUpperCase()
    return normalized === 'MANAGER' || normalized === 'ROLE_MANAGER'
  })
  const isClient = roles.some(role => {
    const normalized = role.toUpperCase()
    return normalized === 'CLIENT' || normalized === 'ROLE_CLIENT'
  })
  const filteredProjects = useMemo(() => {
    if (!userId) return projects
    if (isManager) {
      return projects.filter(project => project.managerId === userId)
    }
    if (isClient) {
      return projects.filter(project => project.clientId === userId)
    }
    return projects
  }, [projects, userId, isManager, isClient])

  useEffect(() => {
    setMounted(true)
  }, [])

  useEffect(() => {
    let mounted = true
    async function fetchProjects() {
      try {
        const data = await loadProjects()
        if (mounted) setProjects(data)
      } catch (err) {
        if (mounted) setError('Не удалось загрузить проекты.')
      } finally {
        if (mounted) setLoading(false)
      }
    }

    fetchProjects()
    return () => {
      mounted = false
    }
  }, [])

  if (!mounted || loading) {
    return (
      <div className='rounded-xl bg-muted p-6 text-sm text-muted-foreground'>
        Загружаем проекты...
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

  if (!filteredProjects.length) {
    return (
      <div className='rounded-xl bg-muted p-6 text-sm text-muted-foreground'>
        У вас пока нет проектов. Выберите проект на главной странице и
        отправьте заявку.
      </div>
    )
  }

  return (
    <div className='grid grid-cols-1 lg:grid-cols-2 2xl:grid-cols-4 gap-3 w-100%'>
      {filteredProjects.map(project => {
        return (
          <ProjectCard key={project.id} project={project} />
        )
      })}
    </div>
  )
}
