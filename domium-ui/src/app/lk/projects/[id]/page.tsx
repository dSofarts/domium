'use client'

import {
  Camera,
  CheckCircle2,
  ChevronDown,
  ChevronRight,
  File,
  ImagePlus
} from 'lucide-react'
import { useEffect, useMemo, useState } from 'react'
import { useParams, usePathname, useRouter, useSearchParams } from 'next/navigation'

import ChatUI from '@/components/common/ChatUI'
import { HlsPlayer } from '@/components/common/HlsPlayer'
import { Badge } from '@/components/ui/Badge'
import { Button } from '@/components/ui/Button'
import { Input } from '@/components/ui/Input'

import { useAuth } from '@/shared/auth/AuthProvider'
import { getAccessToken, getAuthUserId } from '@/shared/auth/auth'
import { VIDEO_HLS_BASE_URL } from '@/constants/site.constants'
import { ProjectStatusLabel } from '@/shared/enums.type'
import {
  advanceProjectStage,
  loadProject
} from '@/shared/projects/projects.store'
import {
  completeWorkItem,
  createBuildingCamera,
  getBuildingDetails,
  getBuildingCameras,
  getBuildingStages,
  getStageSubStages,
  startCameraStream,
  type ApiCameraDto,
  type ApiStageDto,
  type ApiSubStageStatusDto
} from '@/shared/api/buildings'
import {
  downloadDocumentFile,
  fetchDocumentImageObjectUrl,
  getDocumentDetails,
  listProjectDocuments,
  signDocument,
  uploadStageDocument,
  type ApiDocumentInstance
} from '@/shared/api/documents'
import type { IProject } from '@/shared/types/project.interface'

const DOC_STATUS_LABEL: Record<string, string> = {
  SENT_TO_USER: 'Ожидает подписи',
  VIEWED: 'Просмотрен',
  SIGNED: 'Подписан',
  REJECTED: 'Отклонён',
  DELETE: 'Удалён'
}

export default function ProjectPage() {
  const params = useParams()
  const router = useRouter()
  const pathname = usePathname()
  const searchParams = useSearchParams()
  const buildingId = params?.id as string
  const stageIdParam = searchParams.get('stageId')
  const subStageIdParam = searchParams.get('subStageId')

  const { user } = useAuth()
  const userId = useMemo(() => getAuthUserId(), [user])
  const accessToken = useMemo(() => getAccessToken(), [user])
  const roles = user?.roles || []
  const isManager = roles.includes('MANAGER') || roles.includes('ROLE_MANAGER')

  const [mounted, setMounted] = useState(false)
  const [project, setProject] = useState<IProject | null>(null)
  const [loading, setLoading] = useState(true)
  const [loadError, setLoadError] = useState('')
  const [currentStageName, setCurrentStageName] = useState<string | null>(null)

  const [stages, setStages] = useState<ApiStageDto[]>([])
  const [stagesLoading, setStagesLoading] = useState(false)
  const [subStages, setSubStages] = useState<ApiSubStageStatusDto[]>([])
  const [subStagesLoading, setSubStagesLoading] = useState(false)
  const [subStagesError, setSubStagesError] = useState('')

  const [documents, setDocuments] = useState<ApiDocumentInstance[]>([])
  const [documentsLoading, setDocumentsLoading] = useState(false)
  const [documentsError, setDocumentsError] = useState('')
  const [documentSignatures, setDocumentSignatures] = useState<
    Record<string, { client: boolean; manager: boolean }>
  >({})
  const [stageError, setStageError] = useState('')
  const [stageLoading, setStageLoading] = useState(false)

  const [photoUrls, setPhotoUrls] = useState<Record<string, string>>({})
  const [photoError, setPhotoError] = useState('')
  const [expandedSubStages, setExpandedSubStages] = useState<Set<string>>(
    () => new Set()
  )
  const [subStageDocuments, setSubStageDocuments] = useState<
    Record<string, ApiDocumentInstance[]>
  >({})
  const [subStageDocsLoading, setSubStageDocsLoading] = useState<
    Record<string, boolean>
  >({})
  const [subStageDocsError, setSubStageDocsError] = useState<
    Record<string, string>
  >({})
  const [subStagePhotos, setSubStagePhotos] = useState<
    Record<string, ApiDocumentInstance[]>
  >({})
  const [subStagePhotoLoading, setSubStagePhotoLoading] = useState<
    Record<string, boolean>
  >({})
  const [subStagePhotoError, setSubStagePhotoError] = useState<
    Record<string, string>
  >({})

  const [cameras, setCameras] = useState<ApiCameraDto[]>([])
  const [camerasLoading, setCamerasLoading] = useState(false)
  const [camerasError, setCamerasError] = useState('')
  const [streamUrls, setStreamUrls] = useState<Record<string, string | null>>({})
  const [streamLoading, setStreamLoading] = useState<Record<string, boolean>>({})
  const [streamError, setStreamError] = useState<Record<string, string>>({})
  const [cameraName, setCameraName] = useState('')
  const [cameraRtsp, setCameraRtsp] = useState('')
  const [cameraTranscode, setCameraTranscode] = useState(false)

  const [uploadFiles, setUploadFiles] = useState<Record<string, File | null>>(
    {}
  )
  const [uploadTitles, setUploadTitles] = useState<Record<string, string>>({})
  const [photoFiles, setPhotoFiles] = useState<Record<string, File[]>>({})

  const selectedStageId = stageIdParam || stages[0]?.id || null
  const selectedSubStageId = subStageIdParam || subStages[0]?.id || null

  useEffect(() => {
    setMounted(true)
  }, [])

  useEffect(() => {
    let mounted = true
    async function fetchProject() {
      try {
        const found = await loadProject(buildingId)
        if (mounted) setProject(found)
      } catch {
        if (mounted) setLoadError('Не удалось загрузить проект.')
      } finally {
        if (mounted) setLoading(false)
      }
    }

    fetchProject()
    return () => {
      mounted = false
    }
  }, [buildingId])

  useEffect(() => {
    let mounted = true
    async function fetchBuilding() {
      try {
        const details = await getBuildingDetails(buildingId)
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
  }, [buildingId])

  async function handleStartStream(cameraId: string) {
    setStreamLoading(prev => ({ ...prev, [cameraId]: true }))
    setStreamError(prev => ({ ...prev, [cameraId]: '' }))
    try {
      const camera = await startCameraStream(buildingId, cameraId)
      const resolvedUrl = camera.hlsUrl
        ? camera.hlsUrl.startsWith('/')
          ? `${VIDEO_HLS_BASE_URL}${camera.hlsUrl}`
          : camera.hlsUrl
        : null
      if (resolvedUrl) {
        const ready = await waitForHlsManifest(resolvedUrl, accessToken)
        setStreamUrls(prev => ({ ...prev, [cameraId]: ready ? resolvedUrl : null }))
      } else {
        setStreamUrls(prev => ({ ...prev, [cameraId]: null }))
      }
    } catch {
      setStreamError(prev => ({
        ...prev,
        [cameraId]: 'Не удалось запустить трансляцию.'
      }))
    } finally {
      setStreamLoading(prev => ({ ...prev, [cameraId]: false }))
    }
  }

  async function waitForHlsManifest(url: string, token: string | null) {
    const attempts = 10
    for (let i = 0; i < attempts; i += 1) {
      try {
        const response = await fetch(url, {
          method: 'GET',
          headers: token ? { Authorization: `Bearer ${token}` } : undefined,
          cache: 'no-store'
        })
        if (response.ok) return true
      } catch {
        // ignore and retry
      }
      await new Promise(resolve => setTimeout(resolve, 1000))
    }
    return false
  }

  useEffect(() => {
    let mounted = true
    async function fetchStages() {
      setStagesLoading(true)
      try {
        const items = await getBuildingStages(buildingId)
        if (mounted) setStages(items)
      } catch {
        if (mounted) setStages([])
      } finally {
        if (mounted) setStagesLoading(false)
      }
    }

    fetchStages()
    return () => {
      mounted = false
    }
  }, [buildingId])

  useEffect(() => {
    if (!selectedStageId) return
    const stageId = selectedStageId
    let mounted = true
    async function fetchSubStages() {
      setSubStagesLoading(true)
      setSubStagesError('')
      try {
        const items = await getStageSubStages(buildingId, stageId)
        if (mounted) setSubStages(items)
      } catch {
        if (mounted) setSubStagesError('Не удалось загрузить подэтапы.')
      } finally {
        if (mounted) setSubStagesLoading(false)
      }
    }

    fetchSubStages()
    return () => {
      mounted = false
    }
  }, [buildingId, selectedStageId])

  useEffect(() => {
    setSubStageDocuments({})
    setSubStagePhotos({})
    setDocumentSignatures({})
    setDocuments([])
    setExpandedSubStages(new Set())
  }, [selectedStageId])

  useEffect(() => {
    if (!project?.projectId || subStages.length === 0) return
    const projectId = project.projectId
    let mounted = true
    async function fetchDocumentsForSubStages() {
      setDocumentsLoading(true)
      setDocumentsError('')
      const initialLoading: Record<string, boolean> = {}
      subStages.forEach(subStage => {
        initialLoading[subStage.id] = true
      })
      if (mounted) setSubStageDocsLoading(initialLoading)
      try {
        const entries = await Promise.all(
          subStages.map(async subStage => {
            const items = await listProjectDocuments(
              projectId,
              'STAGE_DOCS',
              subStage.id
            )
            return [subStage.id, items] as const
          })
        )
        if (mounted) {
          const map = Object.fromEntries(entries)
          setSubStageDocuments(map)
          setDocuments(entries.flatMap(([, items]) => items))
        }
      } catch {
        if (mounted) {
          setDocumentsError('Не удалось загрузить документы.')
          setSubStageDocsError(prev => {
            const next = { ...prev }
            subStages.forEach(subStage => {
              next[subStage.id] = 'Не удалось загрузить документы.'
            })
            return next
          })
        }
      } finally {
        if (mounted) {
          setDocumentsLoading(false)
          setSubStageDocsLoading(prev => {
            const updated = { ...prev }
            subStages.forEach(subStage => {
              updated[subStage.id] = false
            })
            return updated
          })
        }
      }
    }
    fetchDocumentsForSubStages()
    return () => {
      mounted = false
    }
  }, [project?.projectId, subStages])

  async function loadDocumentSignatures(documentId: string) {
    const details = await getDocumentDetails(documentId)
    const signatures = details.signatures || []
    const client = signatures.some(
      signature => signature.signerType?.toUpperCase() === 'CLIENT'
    )
    const manager = signatures.some(
      signature => signature.signerType?.toUpperCase() === 'MANAGER'
    )
    return { client, manager }
  }

  useEffect(() => {
    if (documents.length === 0) {
      setDocumentSignatures({})
      return
    }
    let mounted = true
    async function fetchSignatures() {
      const entries = await Promise.all(
        documents.map(async doc => {
          try {
            const signatures = await loadDocumentSignatures(doc.id)
            return [doc.id, signatures] as const
          } catch {
            return [doc.id, { client: false, manager: false }] as const
          }
        })
      )
      if (mounted) {
        setDocumentSignatures(Object.fromEntries(entries))
      }
    }
    fetchSignatures()
    return () => {
      mounted = false
    }
  }, [documents])

  useEffect(() => {
    if (!selectedSubStageId) return
    setExpandedSubStages(prev => {
      if (prev.has(selectedSubStageId)) return prev
      const next = new Set(prev)
      next.add(selectedSubStageId)
      return next
    })
  }, [selectedSubStageId])

  useEffect(() => {
    const reports = Object.values(subStagePhotos).flat()
    if (reports.length === 0) {
      setPhotoUrls({})
      return
    }
    let mounted = true
    async function fetchUrls() {
      Object.values(photoUrls).forEach(url => URL.revokeObjectURL(url))
      const entries = await Promise.all(
        reports.map(async report => {
          try {
            const url = await fetchDocumentImageObjectUrl(report.id)
            return [report.id, url] as const
          } catch {
            return [report.id, ''] as const
          }
        })
      )
      if (mounted) {
        setPhotoUrls(Object.fromEntries(entries.filter(([, url]) => url)))
      }
    }
    fetchUrls()
    return () => {
      mounted = false
    }
  }, [subStagePhotos])

  useEffect(() => {
    let mounted = true
    async function fetchCameras() {
      setCamerasLoading(true)
      setCamerasError('')
      try {
        const items = await getBuildingCameras(buildingId)
        if (mounted) setCameras(items)
      } catch {
        if (mounted) setCamerasError('Не удалось загрузить камеры.')
      } finally {
        if (mounted) setCamerasLoading(false)
      }
    }
    fetchCameras()
    return () => {
      mounted = false
    }
  }, [buildingId])

  function updateQueryParam(key: string, value: string) {
    if (!pathname) return
    const next = new URLSearchParams(searchParams.toString())
    next.set(key, value)
    router.replace(`${pathname}?${next.toString()}`, { scroll: false })
  }

  function isDocSigned(status?: string) {
    return status === 'SIGNED'
  }

  const canAdvanceStage = useMemo(() => {
    if (!isManager || !selectedStageId) return false
    if (documents.length === 0) return true
    return documents.every(doc => isDocSigned(doc.status))
  }, [documents, isManager, selectedStageId])

  const currentStageId = useMemo(() => {
    if (!currentStageName) return null
    const matched = stages.find(
      stage => stage.name.toLowerCase() === currentStageName.toLowerCase()
    )
    return matched?.id || null
  }, [currentStageName, stages])

  const isCurrentStageSelected =
    Boolean(selectedStageId && currentStageId && selectedStageId === currentStageId)

  const photoLoading = useMemo(() => {
    return Object.values(subStagePhotoLoading).some(Boolean)
  }, [subStagePhotoLoading])

  const anyStreamLoading = useMemo(() => {
    return Object.values(streamLoading).some(Boolean)
  }, [streamLoading])

  async function handleSignDocument(documentId: string, subStageId?: string) {
    setDocumentsError('')
    try {
      await signDocument(documentId)
      if (project?.projectId && subStageId) {
        const items = await listProjectDocuments(
          project.projectId,
          'STAGE_DOCS',
          subStageId
        )
        setSubStageDocuments(prev => {
          const next = { ...prev, [subStageId]: items }
          setDocuments(Object.values(next).flat())
          return next
        })
      }
      const signatures = await loadDocumentSignatures(documentId)
      setDocumentSignatures(prev => ({
        ...prev,
        [documentId]: signatures
      }))
    } catch {
      setDocumentsError('Не удалось подписать документ.')
    }
  }

  async function handleUploadDocument(subStageId: string) {
    if (!project?.projectId || !subStageId || !userId) return
    const uploadFile = uploadFiles[subStageId]
    if (!uploadFile) return
    const recipientId = isManager ? project.clientId : project.managerId
    if (!recipientId) return
    setDocumentsError('')
    try {
      await uploadStageDocument({
        projectId: project.projectId,
        stageId: subStageId,
        recipientId,
        file: uploadFile,
        title: uploadTitles[subStageId],
        kind: 'STAGE'
      })
      setUploadFiles(prev => ({ ...prev, [subStageId]: null }))
      setUploadTitles(prev => ({ ...prev, [subStageId]: '' }))
      const items = await listProjectDocuments(
        project.projectId,
        'STAGE_DOCS',
        subStageId
      )
      setSubStageDocuments(prev => {
        const next = { ...prev, [subStageId]: items }
        setDocuments(Object.values(next).flat())
        return next
      })
    } catch {
      setDocumentsError('Не удалось загрузить документ.')
    }
  }

  async function handleDownloadDocument(docId: string) {
    try {
      const { blob, contentType } = await downloadDocumentFile(docId)
      const extension = (() => {
        if (contentType.includes('pdf')) return 'pdf'
        if (contentType.includes('png')) return 'png'
        if (contentType.includes('jpeg')) return 'jpg'
        if (contentType.includes('text/plain')) return 'txt'
        return 'file'
      })()
      const url = URL.createObjectURL(blob)
      const link = document.createElement('a')
      link.href = url
      link.download = `document-${docId}.${extension}`
      document.body.appendChild(link)
      link.click()
      link.remove()
      URL.revokeObjectURL(url)
    } catch {
      setDocumentsError('Не удалось скачать документ.')
    }
  }

  async function handleUploadPhotoReports(subStageId: string) {
    if (!project?.projectId || !subStageId || !userId) return
    const recipientId = project.clientId || project.managerId
    if (!recipientId) return
    setPhotoError('')
    try {
      const files = photoFiles[subStageId] || []
      for (const file of files) {
        await uploadStageDocument({
          projectId: project.projectId,
          stageId: subStageId,
          recipientId,
          file,
          kind: 'PHOTO'
        })
      }
      setPhotoFiles(prev => ({ ...prev, [subStageId]: [] }))
      const items = await listProjectDocuments(
        project.projectId,
        'PHOTO_REPORTS',
        subStageId
      )
      setSubStagePhotos(prev => ({ ...prev, [subStageId]: items }))
    } catch {
      setPhotoError('Не удалось загрузить фотоотчёты.')
    }
  }

  async function handleCompleteWorkItem(workItemId: string) {
    setSubStagesError('')
    try {
      await completeWorkItem(buildingId, workItemId)
      if (selectedStageId) {
        const items = await getStageSubStages(buildingId, selectedStageId)
        setSubStages(items)
      }
    } catch {
      setSubStagesError('Не удалось отметить выполнение.')
    }
  }

  async function handleCreateCamera() {
    if (!cameraName.trim() || !cameraRtsp.trim()) return
    setCamerasError('')
    try {
      const created = await createBuildingCamera(buildingId, {
        name: cameraName.trim(),
        rtspUrl: cameraRtsp.trim(),
        transcode: cameraTranscode
      })
      setCameras(prev => [...prev, created])
      setCameraName('')
      setCameraRtsp('')
      setCameraTranscode(false)
    } catch {
      setCamerasError('Не удалось добавить камеру.')
    }
  }

  async function handleAdvanceStage() {
    if (!project || !canAdvanceStage) return
    setStageLoading(true)
    setStageError('')
    try {
      await advanceProjectStage(project.id)
    } catch {
      setStageError('Не удалось завершить этап.')
    } finally {
      setStageLoading(false)
    }
  }

  if (!mounted || loading) {
    return (
      <div className='rounded-xl bg-muted p-6 text-sm text-muted-foreground'>
        Загружаем проект...
      </div>
    )
  }

  if (loadError) {
    return (
      <div className='rounded-xl bg-muted p-6 text-sm text-destructive'>
        {loadError}
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

  return (
    <div className='space-y-6'>
      <div className='flex flex-wrap items-center gap-2'>
        <h1 className='text-xl font-bold'>{project.name}</h1>
        <Badge>{ProjectStatusLabel[project.stage] || project.stage}</Badge>
      </div>

      <section className='rounded-2xl border p-4'>
        <h3 className='mb-3 text-base font-semibold'>Подэтапы выбранного этапа</h3>
        {stagesLoading && (
          <div className='text-sm text-muted-foreground'>Загружаем этапы...</div>
        )}
        {subStagesLoading && (
          <div className='text-sm text-muted-foreground'>Загружаем подэтапы...</div>
        )}
        {subStagesError && (
          <div className='text-sm text-destructive'>{subStagesError}</div>
        )}
        {!subStagesLoading && subStages.length === 0 && (
          <div className='text-sm text-muted-foreground'>
            Выберите этап, чтобы увидеть подэтапы.
          </div>
        )}
        <div className='grid gap-4'>
          {subStages.map(subStage => {
            const isExpanded = expandedSubStages.has(subStage.id)
            const stageDocuments = subStageDocuments[subStage.id] || []
            const stagePhotos = subStagePhotos[subStage.id] || []
            return (
              <div key={subStage.id} className='rounded-xl border p-4'>
                <div className='flex flex-wrap items-center justify-between gap-2'>
                  <div className='flex items-center gap-2'>
                    <Button
                      variant='ghost'
                      size='sm'
                      className='px-2'
                      onClick={() => {
                        updateQueryParam('subStageId', subStage.id)
                        setExpandedSubStages(prev => {
                          const next = new Set(prev)
                          if (next.has(subStage.id)) {
                            next.delete(subStage.id)
                          } else {
                            next.add(subStage.id)
                          }
                          return next
                        })
                        if (!subStagePhotos[subStage.id] && project?.projectId) {
                          setSubStagePhotoLoading(prev => ({
                            ...prev,
                            [subStage.id]: true
                          }))
                          listProjectDocuments(
                            project.projectId,
                            'PHOTO_REPORTS',
                            subStage.id
                          )
                            .then(items =>
                              setSubStagePhotos(prev => ({
                                ...prev,
                                [subStage.id]: items
                              }))
                            )
                            .catch(() =>
                              setSubStagePhotoError(prev => ({
                                ...prev,
                                [subStage.id]: 'Не удалось загрузить фотоотчёты.'
                              }))
                            )
                            .finally(() =>
                              setSubStagePhotoLoading(prev => ({
                                ...prev,
                                [subStage.id]: false
                              }))
                            )
                        }
                      }}
                    >
                      {isExpanded ? (
                        <ChevronDown className='h-4 w-4' />
                      ) : (
                        <ChevronRight className='h-4 w-4' />
                      )}
                    </Button>
                    <div className='font-medium'>{subStage.name}</div>
                  </div>
                  {subStage.completed && (
                    <span className='flex items-center gap-1 text-xs text-emerald-600'>
                      <CheckCircle2 className='h-4 w-4' />
                      Завершён
                    </span>
                  )}
                </div>

                {isExpanded && (
                  <div className='mt-3 space-y-4'>
                    {subStage.workItems && subStage.workItems.length > 0 && (
                      <div className='space-y-2 text-sm'>
                        {subStage.workItems.map(item => (
                          <div
                            key={item.id}
                            className='flex items-center justify-between rounded-lg border px-3 py-2'
                          >
                            <div>
                              <div className='font-medium'>{item.name}</div>
                              {item.description && (
                                <div className='text-xs text-muted-foreground'>
                                  {item.description}
                                </div>
                              )}
                            </div>
                            {item.completed ? (
                              <Badge variant='secondary'>Готово</Badge>
                            ) : (
                              isManager && (
                                <Button
                                  size='sm'
                                  variant='outline'
                                  onClick={() => handleCompleteWorkItem(item.id)}
                                >
                                  Завершить
                                </Button>
                              )
                            )}
                          </div>
                        ))}
                      </div>
                    )}

                    <div className='rounded-xl border p-3'>
                      <div className='mb-2 flex items-center justify-between'>
                        <div className='font-medium'>Документы подэтапа</div>
                        <Badge variant='secondary'>{stageDocuments.length} шт.</Badge>
                      </div>
                      {subStageDocsLoading[subStage.id] && (
                        <div className='text-xs text-muted-foreground'>
                          Загружаем документы...
                        </div>
                      )}
                      {subStageDocsError[subStage.id] && (
                        <div className='text-xs text-destructive'>
                          {subStageDocsError[subStage.id]}
                        </div>
                      )}
                      <div className='grid gap-2'>
                        {stageDocuments.map(doc => {
                          const signatures = documentSignatures[doc.id] || {
                            client: false,
                            manager: false
                          }
                          const canSign =
                            !isDocSigned(doc.status) &&
                            (isManager ? !signatures.manager : !signatures.client)
                          const signedByBoth = signatures.client && signatures.manager
                          return (
                            <div
                              key={doc.id}
                              className='flex flex-wrap items-center gap-3 rounded-lg border px-3 py-2'
                            >
                              <File className='h-4 w-4 text-muted-foreground' />
                              <div className='flex-1'>
                                <div className='font-medium'>
                                  {doc.title || doc.template?.name || 'Документ'}
                                </div>
                                <div className='text-xs text-muted-foreground'>
                                  {DOC_STATUS_LABEL[doc.status] || doc.status}
                                </div>
                                <div className='mt-1 flex flex-wrap items-center gap-3 text-xs'>
                                  <span className='flex items-center gap-1 text-muted-foreground'>
                                    <CheckCircle2
                                      className={`h-3.5 w-3.5 ${
                                        signatures.client
                                          ? 'text-emerald-500'
                                          : 'text-rose-500'
                                      }`}
                                    />
                                    Клиент
                                  </span>
                                  <span className='flex items-center gap-1 text-muted-foreground'>
                                    <CheckCircle2
                                      className={`h-3.5 w-3.5 ${
                                        signatures.manager
                                          ? 'text-emerald-500'
                                          : 'text-rose-500'
                                      }`}
                                    />
                                    Менеджер
                                  </span>
                                  {signedByBoth && (
                                    <span className='flex items-center gap-1 text-emerald-600'>
                                      <CheckCircle2 className='h-3.5 w-3.5' />
                                      Документ подписан
                                    </span>
                                  )}
                                </div>
                              </div>
                              <div className='flex gap-2'>
                                <Button
                                  size='sm'
                                  variant='outline'
                                  onClick={() => handleDownloadDocument(doc.id)}
                                >
                                  Скачать
                                </Button>
                                {canSign && (
                                  <Button
                                    size='sm'
                                    onClick={() =>
                                      handleSignDocument(doc.id, subStage.id)
                                    }
                                  >
                                    Подписать
                                  </Button>
                                )}
                              </div>
                            </div>
                          )
                        })}
                        {!subStageDocsLoading[subStage.id] &&
                          stageDocuments.length === 0 && (
                            <div className='text-xs text-muted-foreground'>
                              Документы не загружены.
                            </div>
                          )}
                      </div>
                      {isCurrentStageSelected && (
                        <div className='mt-3 grid gap-2'>
                          <Input
                            type='file'
                            onChange={event => {
                              const file = event.target.files?.[0]
                              setUploadFiles(prev => ({
                                ...prev,
                                [subStage.id]: file || null
                              }))
                            }}
                          />
                          <Input
                            placeholder='Название документа (необязательно)'
                            value={uploadTitles[subStage.id] || ''}
                            onChange={event =>
                              setUploadTitles(prev => ({
                                ...prev,
                                [subStage.id]: event.target.value
                              }))
                            }
                          />
                          <Button
                            variant='secondary'
                            onClick={() => handleUploadDocument(subStage.id)}
                            disabled={!uploadFiles[subStage.id]}
                          >
                            Загрузить документ
                          </Button>
                        </div>
                      )}
                    </div>

                    <div className='rounded-xl border p-3'>
                      <div className='mb-2 flex items-center justify-between'>
                        <div className='font-medium'>Фотоотчёты подэтапа</div>
                        <Badge variant='secondary'>{stagePhotos.length} фото</Badge>
                      </div>
                      {subStagePhotoLoading[subStage.id] && (
                        <div className='text-xs text-muted-foreground'>
                          Загружаем фото...
                        </div>
                      )}
                      {subStagePhotoError[subStage.id] && (
                        <div className='text-xs text-destructive'>
                          {subStagePhotoError[subStage.id]}
                        </div>
                      )}
                      <div className='grid gap-2 sm:grid-cols-2 xl:grid-cols-3'>
                        {stagePhotos.map(report => {
                          const url = photoUrls[report.id]
                          return (
                            <div
                              key={report.id}
                              className='overflow-hidden rounded-lg border'
                            >
                              {url ? (
                                <img
                                  src={url}
                                  alt='Фотоотчёт'
                                  className='h-36 w-full object-cover'
                                />
                              ) : (
                                <div className='flex h-36 items-center justify-center text-xs text-muted-foreground'>
                                  Нет изображения
                                </div>
                              )}
                            </div>
                          )
                        })}
                      </div>
                      {isManager && (
                        <div className='mt-3 grid gap-2'>
                          <Input
                            type='file'
                            multiple
                            accept='image/*'
                            onChange={event => {
                              const files = Array.from(
                                event.target.files || []
                              )
                              setPhotoFiles(prev => ({
                                ...prev,
                                [subStage.id]: files
                              }))
                            }}
                          />
                          <Button
                            variant='secondary'
                            onClick={() => handleUploadPhotoReports(subStage.id)}
                            disabled={
                              !photoFiles[subStage.id] ||
                              photoFiles[subStage.id].length === 0
                            }
                          >
                            <ImagePlus className='mr-2 h-4 w-4' />
                            Загрузить фотоотчёт
                          </Button>
                        </div>
                      )}
                    </div>
                  </div>
                )}
              </div>
            )
          })}
        </div>
      </section>

      <div className='rounded-2xl border p-4'>
        {isManager && isCurrentStageSelected && (
          <Button
            onClick={handleAdvanceStage}
            disabled={!canAdvanceStage || stageLoading}
          >
            Завершить этап
          </Button>
        )}
        {stageError && (
          <div className='text-xs text-destructive'>{stageError}</div>
        )}
        {documentsLoading && (
          <div className='mt-2 text-xs text-muted-foreground'>
            Загружаем документы...
          </div>
        )}
        {documentsError && (
          <div className='mt-2 text-xs text-destructive'>{documentsError}</div>
        )}
        {photoLoading && (
          <div className='mt-2 text-xs text-muted-foreground'>Загружаем фото...</div>
        )}
        {photoError && (
          <div className='mt-2 text-xs text-destructive'>{photoError}</div>
        )}
      </div>

      <section className='rounded-2xl border p-4'>
        <div className='mb-3 flex items-center justify-between'>
          <h3 className='text-base font-semibold'>Трансляция камеры</h3>
          {anyStreamLoading && <Badge variant='secondary'>...</Badge>}
        </div>
        {camerasError && (
          <div className='text-sm text-destructive'>{camerasError}</div>
        )}
        <div className='grid gap-3 sm:grid-cols-2'>
          {cameras.map(camera => (
            <div key={camera.id} className='rounded-xl border p-3'>
              <div className='flex items-center gap-3'>
                <Camera className='h-5 w-5 text-muted-foreground' />
                <div>
                  <div className='font-medium'>{camera.name}</div>
                  <div className='text-xs text-muted-foreground'>
                    {camera.running ? 'В эфире' : 'Остановлена'}
                  </div>
                </div>
              </div>
              <div className='mt-3'>
                {streamError[camera.id] && (
                  <div className='text-xs text-destructive'>
                    {streamError[camera.id]}
                  </div>
                )}
                {streamUrls[camera.id] ? (
                  <div className='overflow-hidden rounded-xl border bg-muted'>
                    <HlsPlayer
                      src={streamUrls[camera.id] || ''}
                      className='h-48 w-full object-cover'
                      token={accessToken}
                    />
                  </div>
                ) : (
                  <Button
                    size='sm'
                    variant='secondary'
                    onClick={() => handleStartStream(camera.id)}
                    disabled={streamLoading[camera.id]}
                  >
                    {streamLoading[camera.id]
                      ? 'Запускаем...'
                      : 'Запустить трансляцию'}
                  </Button>
                )}
              </div>
            </div>
          ))}
        </div>
        {!camerasLoading && cameras.length === 0 && !camerasError && (
          <div className='text-sm text-muted-foreground'>
            Камеры пока не добавлены.
          </div>
        )}
        {isManager && (
          <div className='mt-4 grid gap-2'>
            <Input
              placeholder='Название камеры'
              value={cameraName}
              onChange={event => setCameraName(event.target.value)}
            />
            <Input
              placeholder='RTSP URL'
              value={cameraRtsp}
              onChange={event => setCameraRtsp(event.target.value)}
            />
            <label className='flex items-center gap-2 text-sm text-muted-foreground'>
              <input
                type='checkbox'
                checked={cameraTranscode}
                onChange={event => setCameraTranscode(event.target.checked)}
              />
              Перекодирование в H264
            </label>
            <Button
              variant='secondary'
              onClick={handleCreateCamera}
              disabled={!cameraName || !cameraRtsp}
            >
              Добавить камеру
            </Button>
          </div>
        )}
      </section>

      <div className='fixed bottom-4 right-4 w-[360px] max-w-[calc(100vw-2rem)]'>
        <ChatUI
          projectId={project.projectId}
          managerId={project.managerId}
          managerName={project.managerName || 'Менеджер'}
        />
      </div>
    </div>
  )
}
