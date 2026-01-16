'use client'

import { useEffect, useMemo, useState } from 'react'
import { useParams, usePathname, useRouter, useSearchParams } from 'next/navigation'

import { Badge } from '@/components/ui/Badge'
import { Button } from '@/components/ui/Button'
import { ScrollArea } from '@/components/ui/scroll-area'
import {
  SidebarGroup,
  SidebarGroupContent,
  SidebarGroupLabel
} from '@/components/ui/Sidebar'
import {
  getBuildingDetails,
  getBuildingStages,
  type ApiStageDto
} from '@/shared/api/buildings'
import { loadProject } from '@/shared/projects/projects.store'
import { cn } from '@/utils/cn'

export function ProjectSidebarDetails() {
  const params = useParams()
  const pathname = usePathname()
  const router = useRouter()
  const searchParams = useSearchParams()
  const buildingId = typeof params?.id === 'string' ? params.id : null
  const stageIdParam = searchParams.get('stageId')
  const cameraIdParam = searchParams.get('cameraId')
  const showDetails = useMemo(() => {
    return Boolean(buildingId && pathname?.startsWith('/lk/projects/'))
  }, [buildingId, pathname])

  const [projectName, setProjectName] = useState('')
  const [stages, setStages] = useState<ApiStageDto[]>([])
  const [stageLoading, setStageLoading] = useState(false)
  const [currentStageName, setCurrentStageName] = useState<string | null>(null)
  const selectedStageId = stageIdParam || stages[0]?.id || null

  useEffect(() => {
    if (!showDetails || !buildingId) return
    const id = buildingId
    let mounted = true
    async function fetchProject() {
      try {
        const project = await loadProject(id)
        if (mounted) setProjectName(project?.name || '')
      } catch {
        if (mounted) setProjectName('')
      }
    }
    fetchProject()
    return () => {
      mounted = false
    }
  }, [buildingId, showDetails])

  useEffect(() => {
    if (!showDetails || !buildingId) return
    const id = buildingId
    let mounted = true
    async function fetchBuilding() {
      try {
        const details = await getBuildingDetails(id)
        const stageName =
          typeof details?.building?.currentStageName === 'string'
            ? details.building.currentStageName
            : null
        if (mounted) setCurrentStageName(stageName)
      } catch {
        if (mounted) setCurrentStageName(null)
      }
    }
    fetchBuilding()
    return () => {
      mounted = false
    }
  }, [buildingId, showDetails])

  useEffect(() => {
    if (!showDetails || !buildingId) return
    const id = buildingId
    let mounted = true
    async function fetchStages() {
      setStageLoading(true)
      try {
        const items = await getBuildingStages(id)
        if (mounted) setStages(items)
      } catch {
        if (mounted) setStages([])
      } finally {
        if (mounted) setStageLoading(false)
      }
    }
    fetchStages()
    return () => {
      mounted = false
    }
  }, [buildingId, showDetails])

  function handleSelectStage(stageId: string) {
    if (!pathname) return
    const next = new URLSearchParams(searchParams.toString())
    next.set('stageId', stageId)
    router.replace(`${pathname}?${next.toString()}`, { scroll: false })
  }

  const currentStagePosition = useMemo(() => {
    if (!currentStageName) return null
    const matched = stages.find(
      stage => stage.name.toLowerCase() === currentStageName.toLowerCase()
    )
    return matched?.position ?? null
  }, [currentStageName, stages])

  if (!showDetails) return null

  return (
    <SidebarGroup>
      <SidebarGroupLabel>Проект</SidebarGroupLabel>
      <SidebarGroupContent className='space-y-4'>
        <div className='rounded-xl border px-3 py-2 text-sm'>
          <div className='font-medium'>{projectName || 'Проект'}</div>
          {selectedStageId && (
            <div className='mt-1 text-xs text-muted-foreground'>
              Выбранный этап
            </div>
          )}
        </div>

        <div>
          <div className='mb-2 flex items-center gap-2 text-sm font-medium'>
            Этапы
            {stageLoading && <Badge variant='secondary'>...</Badge>}
          </div>
          <ScrollArea className='max-h-64 pr-2'>
            <div className='flex flex-col gap-2'>
              {stages.map(stage => {
                const isSelected = stage.id === selectedStageId
                const status =
                  currentStagePosition === null
                    ? 'upcoming'
                    : stage.position < currentStagePosition
                      ? 'completed'
                      : stage.position === currentStagePosition
                        ? 'current'
                        : 'upcoming'
                return (
                  <Button
                    key={stage.id}
                    variant='outline'
                    size='sm'
                    className={cn(
                      'justify-start',
                      status === 'completed' &&
                        'border-emerald-200 bg-emerald-50 text-emerald-700',
                      status === 'current' &&
                        'border-blue-200 bg-blue-50 text-blue-700',
                      status === 'upcoming' &&
                        'border-muted-foreground/20 text-muted-foreground',
                      isSelected && 'ring-2 ring-offset-1 ring-primary'
                    )}
                    onClick={() => handleSelectStage(stage.id)}
                  >
                    {stage.name}
                  </Button>
                )
              })}
              {!stageLoading && stages.length === 0 && (
                <div className='text-xs text-muted-foreground'>
                  Этапы не найдены
                </div>
              )}
            </div>
          </ScrollArea>
        </div>

      </SidebarGroupContent>
    </SidebarGroup>
  )
}
