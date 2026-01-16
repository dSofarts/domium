'use client'

import { useEffect, useMemo, useState } from 'react'
import { useRouter } from 'next/navigation'

import { Button } from '@/components/ui/Button'
import { Input } from '@/components/ui/Input'
import { PAGES } from '@/config/pages.config'
import {
  createProject,
  uploadProjectImages
} from '@/shared/api/projects'
import { createWorkflow, getDefaultWorkflow } from '@/shared/api/buildings'
import { useAuth } from '@/shared/auth/AuthProvider'
import { extractRoles } from '@/shared/api/keycloak'
import { getAccessToken } from '@/shared/auth/auth'

const PROJECT_TYPES = [
  { value: 'SERIAL', label: 'Серийный' },
  { value: 'INDIVIDUAL', label: 'Индивидуальный' },
  { value: 'BATHHOUSE', label: 'Баня' }
]

const ROOM_TYPES = [
  { value: 'BEDROOM', label: 'Спальня' },
  { value: 'BATHROOM', label: 'Ванная' },
  { value: 'KITCHEN', label: 'Кухня' },
  { value: 'LIVING_ROOM', label: 'Гостиная' },
  { value: 'DINING_ROOM', label: 'Столовая' },
  { value: 'OFFICE', label: 'Кабинет' },
  { value: 'GARAGE', label: 'Гараж' },
  { value: 'LAVATORY', label: 'Туалет' },
  { value: 'WARDROBE', label: 'Гардероб' },
  { value: 'OTHER', label: 'Другое' }
]

const inputClassName =
  'file:text-foreground placeholder:text-muted-foreground selection:bg-primary selection:text-primary-foreground dark:bg-input/30 border-input h-9 w-full min-w-0 rounded-md border bg-transparent px-3 py-1 text-base shadow-xs transition-[color,box-shadow] outline-none file:inline-flex file:h-7 file:border-0 file:bg-transparent file:text-sm file:font-medium disabled:pointer-events-none disabled:cursor-not-allowed disabled:opacity-50 md:text-sm focus-visible:border-ring focus-visible:ring-ring/50 focus-visible:ring-[3px] aria-invalid:ring-destructive/20 dark:aria-invalid:ring-destructive/40 aria-invalid:border-destructive'

export default function AddProjectPage() {
  const router = useRouter()
  const { user } = useAuth()
  const tokenRoles = useMemo(() => {
    const token = getAccessToken()
    return token ? extractRoles(token) : []
  }, [user])
  const roles = user?.roles && user.roles.length > 0 ? user.roles : tokenRoles
  const canCreate = roles.some(role => {
    const normalized = role.toUpperCase()
    return normalized === 'MANAGER' || normalized === 'ROLE_MANAGER'
  })

  const [form, setForm] = useState({
    name: '',
    type: 'SERIAL',
    category: '',
    price: '',
    material: '',
    location: '',
    description: '',
    floorNumber: '1'
  })
  const [rooms, setRooms] = useState<Array<{ type: string; area: string }>>([
    { type: 'LIVING_ROOM', area: '' }
  ])
  const [files, setFiles] = useState<File[]>([])
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')
  const [workflowName, setWorkflowName] = useState('')
  const [workflowNameTouched, setWorkflowNameTouched] = useState(false)
  const [workflowStages, setWorkflowStages] = useState<
    Array<{ name: string; description: string; plannedDays: string }>
  >([])
  const [workflowLoading, setWorkflowLoading] = useState(false)
  const [workflowError, setWorkflowError] = useState('')

  const isDisabled = useMemo(() => {
    if (!form.name.trim()) return true
    if (!form.category.trim()) return true
    if (!form.price.trim() || Number(form.price) <= 0) return true
    if (!form.material.trim()) return true
    if (!form.location.trim()) return true
    if (rooms.length === 0) return true
    if (rooms.some(room => !room.area.trim() || Number(room.area) <= 0)) return true
    return false
  }, [form, rooms])

  useEffect(() => {
    if (!canCreate) return
    let mounted = true
    async function fetchWorkflow() {
      setWorkflowLoading(true)
      setWorkflowError('')
      try {
        const wf = await getDefaultWorkflow()
        if (!mounted) return
        const stages =
          wf.stages?.map(stage => ({
            name: stage.name || '',
            description: stage.description || '',
            plannedDays:
              stage.plannedDays !== undefined ? String(stage.plannedDays) : ''
          })) || []
        setWorkflowStages(stages)
        if (!workflowNameTouched) {
          setWorkflowName(
            form.name.trim()
              ? `Workflow для "${form.name.trim()}"`
              : `Workflow для проекта`
          )
        }
      } catch {
        if (mounted) setWorkflowError('Не удалось загрузить дефолтный workflow.')
      } finally {
        if (mounted) setWorkflowLoading(false)
      }
    }
    fetchWorkflow()
    return () => {
      mounted = false
    }
  }, [canCreate])

  useEffect(() => {
    if (workflowNameTouched) return
    if (!form.name.trim()) return
    setWorkflowName(`Workflow для "${form.name.trim()}"`)
  }, [form.name, workflowNameTouched])

  async function handleSubmit(event: React.FormEvent) {
    event.preventDefault()
    setError('')
    if (isDisabled) return

    setLoading(true)
    try {
      const trimmedStages = workflowStages
        .map(stage => ({
          name: stage.name.trim(),
          description: stage.description.trim(),
          plannedDays: stage.plannedDays.trim()
        }))
        .filter(stage => stage.name)
      if (trimmedStages.length === 0) {
        setError('Добавьте хотя бы один этап workflow.')
        return
      }
      const workflow = await createWorkflow({
        name: workflowName.trim() || `Workflow для "${form.name.trim()}"`,
        stages: trimmedStages.map((stage, index) => ({
          name: stage.name,
          description: stage.description || undefined,
          plannedDays: Number(stage.plannedDays) || 0,
          position: index
        }))
      })
      const created = await createProject({
        name: form.name.trim(),
        type: form.type as 'SERIAL' | 'INDIVIDUAL' | 'BATHHOUSE',
        category: form.category.trim(),
        price: Number(form.price),
        material: form.material.trim(),
        location: form.location.trim(),
        description: form.description.trim() || undefined,
        workflowId: workflow.id,
        floors: [
          {
            floorNumber: Number(form.floorNumber),
            rooms: rooms.map(room => ({
              type: room.type,
              area: Number(room.area)
            }))
          }
        ]
      })

      if (files.length > 0) {
        await uploadProjectImages(created.id, files)
      }

      router.push(PAGES.HOME)
    } catch {
      setError('Не удалось создать проект. Проверьте данные.')
    } finally {
      setLoading(false)
    }
  }

  if (!canCreate) {
    return (
      <div className='rounded-xl bg-muted p-6 text-sm text-muted-foreground'>
        Добавление проектов доступно только для менеджеров.
      </div>
    )
  }

  return (
    <div className='grid gap-6'>
      <div className='flex flex-wrap items-center justify-between gap-3'>
        <h1 className='text-xl font-medium'>Новый проект</h1>
        <Button asChild variant='outline'>
          <a href={PAGES.HOME}>К списку проектов</a>
        </Button>
      </div>
      <form onSubmit={handleSubmit} className='grid gap-4'>
        <div className='grid gap-2'>
          <label className='text-sm font-medium' htmlFor='project-name'>
            Название
          </label>
          <Input
            id='project-name'
            value={form.name}
            onChange={event =>
              setForm(prev => ({ ...prev, name: event.target.value }))
            }
            placeholder='Дом у озера'
          />
        </div>
        <div className='grid gap-2'>
          <label className='text-sm font-medium' htmlFor='project-type'>
            Тип
          </label>
          <select
            id='project-type'
            className={inputClassName}
            value={form.type}
            onChange={event =>
              setForm(prev => ({ ...prev, type: event.target.value }))
            }
          >
            {PROJECT_TYPES.map(option => (
              <option key={option.value} value={option.value}>
                {option.label}
              </option>
            ))}
          </select>
        </div>
        <div className='grid gap-2'>
          <label className='text-sm font-medium' htmlFor='project-category'>
            Категория
          </label>
          <Input
            id='project-category'
            value={form.category}
            onChange={event =>
              setForm(prev => ({ ...prev, category: event.target.value }))
            }
            placeholder='Коттедж'
          />
        </div>
        <div className='grid gap-2'>
          <label className='text-sm font-medium' htmlFor='project-price'>
            Стоимость
          </label>
          <Input
            id='project-price'
            type='number'
            min='0'
            step='0.01'
            value={form.price}
            onChange={event =>
              setForm(prev => ({ ...prev, price: event.target.value }))
            }
            placeholder='15000000'
          />
        </div>
        <div className='grid gap-2'>
          <label className='text-sm font-medium' htmlFor='project-material'>
            Материал
          </label>
          <Input
            id='project-material'
            value={form.material}
            onChange={event =>
              setForm(prev => ({ ...prev, material: event.target.value }))
            }
            placeholder='Кирпич'
          />
        </div>
        <div className='grid gap-2'>
          <label className='text-sm font-medium' htmlFor='project-location'>
            Локация
          </label>
          <Input
            id='project-location'
            value={form.location}
            onChange={event =>
              setForm(prev => ({ ...prev, location: event.target.value }))
            }
            placeholder='Московская область'
          />
        </div>
        <div className='grid gap-2'>
          <label className='text-sm font-medium' htmlFor='project-description'>
            Описание
          </label>
          <textarea
            id='project-description'
            className={`${inputClassName} min-h-20`}
            value={form.description}
            onChange={event =>
              setForm(prev => ({ ...prev, description: event.target.value }))
            }
            placeholder='Краткое описание проекта'
          />
        </div>
        <div className='grid gap-2'>
          <label className='text-sm font-medium' htmlFor='project-floor'>
            Этаж
          </label>
          <Input
            id='project-floor'
            type='number'
            value={form.floorNumber}
            onChange={event =>
              setForm(prev => ({ ...prev, floorNumber: event.target.value }))
            }
          />
        </div>
        <div className='grid gap-3'>
          <div className='flex flex-wrap items-center justify-between gap-2'>
            <p className='text-sm font-medium'>Комнаты</p>
            <Button
              type='button'
              variant='outline'
              onClick={() =>
                setRooms(prev => [...prev, { type: 'LIVING_ROOM', area: '' }])
              }
            >
              Добавить комнату
            </Button>
          </div>
          <div className='grid gap-3'>
            {rooms.map((room, index) => (
              <div key={index} className='grid gap-3 rounded-xl border p-3'>
                <div className='flex items-center justify-between gap-2'>
                  <p className='text-sm font-medium'>Комната {index + 1}</p>
                  <Button
                    type='button'
                    variant='outline'
                    disabled={rooms.length === 1}
                    onClick={() =>
                      setRooms(prev => prev.filter((_, roomIndex) => roomIndex !== index))
                    }
                  >
                    Удалить
                  </Button>
                </div>
                <div className='grid gap-4 md:grid-cols-2'>
                  <div className='grid gap-2'>
                    <label className='text-sm font-medium' htmlFor={`room-type-${index}`}>
                      Тип комнаты
                    </label>
                    <select
                      id={`room-type-${index}`}
                      className={inputClassName}
                      value={room.type}
                      onChange={event =>
                        setRooms(prev =>
                          prev.map((item, roomIndex) =>
                            roomIndex === index
                              ? { ...item, type: event.target.value }
                              : item
                          )
                        )
                      }
                    >
                      {ROOM_TYPES.map(option => (
                        <option key={option.value} value={option.value}>
                          {option.label}
                        </option>
                      ))}
                    </select>
                  </div>
                  <div className='grid gap-2'>
                    <label className='text-sm font-medium' htmlFor={`room-area-${index}`}>
                      Площадь (м²)
                    </label>
                    <Input
                      id={`room-area-${index}`}
                      type='number'
                      min='0'
                      step='0.1'
                      value={room.area}
                      onChange={event =>
                        setRooms(prev =>
                          prev.map((item, roomIndex) =>
                            roomIndex === index
                              ? { ...item, area: event.target.value }
                              : item
                          )
                        )
                      }
                    />
                  </div>
                </div>
              </div>
            ))}
          </div>
        </div>

        <div className='rounded-xl border p-4'>
          <div className='mb-3 flex items-center justify-between'>
            <p className='text-sm font-medium'>Workflow проекта</p>
            {workflowLoading && <span className='text-xs text-muted-foreground'>Загрузка...</span>}
          </div>
          {workflowError && (
            <div className='mb-2 text-xs text-destructive'>{workflowError}</div>
          )}
          <div className='grid gap-2'>
            <label className='text-sm font-medium' htmlFor='workflow-name'>
              Название workflow
            </label>
            <Input
              id='workflow-name'
              value={workflowName}
              onChange={event => {
                setWorkflowName(event.target.value)
                setWorkflowNameTouched(true)
              }}
              placeholder='Workflow для проекта'
            />
          </div>
          <div className='mt-3 grid gap-3'>
            {workflowStages.map((stage, index) => (
              <div key={`${stage.name}-${index}`} className='grid gap-3 rounded-xl border p-3'>
                <div className='flex flex-wrap items-center justify-between gap-2'>
                  <p className='text-sm font-medium'>Этап {index + 1}</p>
                  <div className='flex gap-2'>
                    <Button
                      type='button'
                      variant='outline'
                      disabled={index === 0}
                      onClick={() =>
                        setWorkflowStages(prev => {
                          const next = [...prev]
                          const current = next[index]
                          next[index] = next[index - 1]
                          next[index - 1] = current
                          return next
                        })
                      }
                    >
                      Вверх
                    </Button>
                    <Button
                      type='button'
                      variant='outline'
                      disabled={index === workflowStages.length - 1}
                      onClick={() =>
                        setWorkflowStages(prev => {
                          const next = [...prev]
                          const current = next[index]
                          next[index] = next[index + 1]
                          next[index + 1] = current
                          return next
                        })
                      }
                    >
                      Вниз
                    </Button>
                    <Button
                      type='button'
                      variant='outline'
                      onClick={() =>
                        setWorkflowStages(prev =>
                          prev.filter((_, stageIndex) => stageIndex !== index)
                        )
                      }
                    >
                      Удалить
                    </Button>
                  </div>
                </div>
                <div className='grid gap-2'>
                  <label className='text-sm font-medium'>Название этапа</label>
                  <Input
                    value={stage.name}
                    onChange={event =>
                      setWorkflowStages(prev =>
                        prev.map((item, stageIndex) =>
                          stageIndex === index
                            ? { ...item, name: event.target.value }
                            : item
                        )
                      )
                    }
                  />
                </div>
                <div className='grid gap-2'>
                  <label className='text-sm font-medium'>Описание</label>
                  <Input
                    value={stage.description}
                    onChange={event =>
                      setWorkflowStages(prev =>
                        prev.map((item, stageIndex) =>
                          stageIndex === index
                            ? { ...item, description: event.target.value }
                            : item
                        )
                      )
                    }
                  />
                </div>
                <div className='grid gap-2'>
                  <label className='text-sm font-medium'>Плановые дни</label>
                  <Input
                    type='number'
                    min='0'
                    value={stage.plannedDays}
                    onChange={event =>
                      setWorkflowStages(prev =>
                        prev.map((item, stageIndex) =>
                          stageIndex === index
                            ? { ...item, plannedDays: event.target.value }
                            : item
                        )
                      )
                    }
                  />
                </div>
              </div>
            ))}
            <Button
              type='button'
              variant='outline'
              onClick={() =>
                setWorkflowStages(prev => [
                  ...prev,
                  { name: 'Новый этап', description: '', plannedDays: '1' }
                ])
              }
            >
              Добавить этап
            </Button>
          </div>
        </div>
        <div className='grid gap-2'>
          <label className='text-sm font-medium' htmlFor='project-images'>
            Фотографии
          </label>
          <input
            id='project-images'
            type='file'
            accept='image/*'
            multiple
            className={inputClassName}
            onChange={event => {
              const selected = Array.from(event.target.files || [])
              setFiles(selected)
            }}
          />
          {files.length > 0 ? (
            <p className='text-xs text-muted-foreground'>
              Выбрано файлов: {files.length}
            </p>
          ) : null}
        </div>
        {error ? <p className='text-sm text-destructive'>{error}</p> : null}
        <div className='flex flex-wrap gap-3'>
          <Button type='submit' disabled={loading || isDisabled}>
            Создать
          </Button>
          <Button type='button' variant='outline' onClick={() => router.push(PAGES.HOME)}>
            Отменить
          </Button>
        </div>
      </form>
    </div>
  )
}
