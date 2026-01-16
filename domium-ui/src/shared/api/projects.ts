import { apiFetch } from './client'
import type { IPublicProject } from '@/shared/types/public-project.interface'

export interface ApiProjectResponse {
  id: string
  managerUserId: string
  name: string
  workflowId?: string
  type?: string
  category?: string
  description?: string
  price?: number
  material?: string
  location?: string
  floors?: Array<{
    id: string
    floorNumber?: number
    rooms?: Array<{
      id: string
      roomType?: string
      area?: number
    }>
  }>
  imageUrls?: Array<{
    id: string
    imageUrl: string
    position?: number
  }>
}

export interface CreateProjectRequest {
  name: string
  type: 'SERIAL' | 'INDIVIDUAL' | 'BATHHOUSE'
  category: string
  price: number
  material: string
  location: string
  description?: string
  workflowId?: string
  floors: Array<{
    floorNumber: number
    rooms: Array<{
      type: string
      area: number
    }>
  }>
}

function mapProjectToPublic(project: ApiProjectResponse): IPublicProject {
  const images =
    project.imageUrls
      ?.slice()
      .sort((left, right) => (left.position ?? 0) - (right.position ?? 0))
      .map(item => normalizeProjectImageUrl(item.imageUrl))
      .filter((value): value is string => Boolean(value)) || []
  const cover = images[0]
  return {
    id: project.id,
    name: project.name,
    description: project.description || 'Описание проекта',
    image:
      cover ||
      'https://static.tildacdn.com/tild3133-6139-4834-a236-333936663239/default.jpg',
    images,
    price: project.price ? Number(project.price) : 0,
    type: project.type,
    category: project.category,
    material: project.material,
    location: project.location,
    floors: project.floors?.map(floor => ({
      id: floor.id,
      floorNumber: floor.floorNumber ?? undefined,
      rooms: floor.rooms?.map(room => ({
        id: room.id,
        roomType: room.roomType ?? undefined,
        area: room.area ? Number(room.area) : undefined
      }))
    })),
    managerId: project.managerUserId,
    managerName: undefined,
    workflowId: project.workflowId,
    videoUrl: undefined
  }
}

function normalizeProjectImageUrl(url?: string) {
  if (!url) return url
  if (url.startsWith('http://minio:9000')) {
    return url.replace('http://minio:9000', 'http://localhost:9000')
  }
  return url
}

export async function getPublicProjects(): Promise<IPublicProject[]> {
  const projects = await apiFetch<ApiProjectResponse[]>('/api/projects/')
  return projects.map(mapProjectToPublic)
}

export async function getPublicProjectById(
  projectId: string
): Promise<IPublicProject | null> {
  try {
    const project = await apiFetch<ApiProjectResponse>(`/api/projects/${projectId}`)
    return mapProjectToPublic(project)
  } catch {
    return null
  }
}

export async function createProject(
  request: CreateProjectRequest
): Promise<IPublicProject> {
  const created = await apiFetch<ApiProjectResponse>('/api/projects', {
    method: 'POST',
    body: JSON.stringify(request)
  })
  return mapProjectToPublic(created)
}

export async function uploadProjectImages(projectId: string, files: File[]) {
  const formData = new FormData()
  files.forEach(file => formData.append('images', file))
  return apiFetch<ApiProjectResponse[]>(`/api/projects/${projectId}/images`, {
    method: 'POST',
    body: formData
  })
}

export async function createProjectOrder(projectId: string) {
  return apiFetch(`/api/projects/${projectId}/orders`, {
    method: 'POST'
  })
}
