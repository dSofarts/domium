import { apiFetch } from './client'

export interface ApiProjectDto {
  buildingId: string
  id: string
  clientId?: string
  manager?: {
    id: string
    name?: string
  } | null
  objectInfo?: {
    projectName?: string
    attributes?: Record<string, unknown>
    stage?: string
    progress?: number
  } | null
}

export interface ApiBuildingDetailsDto {
  building: Record<string, unknown>
  project: ApiProjectDto
}

export interface ApiCameraDto {
  id: string
  buildingId: string
  name: string
  enabled: boolean
  transcode: boolean
  hlsUrl?: string
  running?: boolean
}

export interface ApiWorkItemStatusDto {
  id: string
  name: string
  description?: string
  position: number
  completed: boolean
}

export interface ApiSubStageStatusDto {
  id: string
  name: string
  description?: string
  position: number
  completed: boolean
  completedAt?: string
  workItems?: ApiWorkItemStatusDto[]
}

export interface ApiStageDto {
  id: string
  name: string
  description?: string
  plannedDays?: number
  position: number
}

export interface ApiWorkflowDto {
  id: string
  managerId: string
  name: string
  active: boolean
  createdAt?: string
  stages?: ApiStageDto[]
}

export interface CreateBuildingInput {
  projectId: string
  projectName: string
  managerId: string
  managerName?: string
  attributes?: Record<string, unknown>
  workflowId?: string
}

export async function getMyBuildings() {
  return apiFetch<ApiProjectDto[]>('/api/buildings')
}

export async function getBuildingDetails(buildingId: string) {
  return apiFetch<ApiBuildingDetailsDto>(`/api/buildings/${buildingId}`)
}

export async function createBuilding(input: CreateBuildingInput) {
  const body = {
    project: {
      id: input.projectId,
      manager: {
        id: input.managerId,
        name: input.managerName
      },
      objectInfo: {
        projectName: input.projectName,
        attributes: input.attributes || {},
        stage: 'Заявка отправлена',
        progress: 0
      }
    },
    workflowId: input.workflowId
  }

  return apiFetch<ApiProjectDto>('/api/buildings', {
    method: 'POST',
    body: JSON.stringify(body)
  })
}

export async function getBuildingCameras(buildingId: string) {
  return apiFetch<ApiCameraDto[]>(
    `/api/buildings/${buildingId}/cameras`
  )
}

export async function createBuildingCamera(
  buildingId: string,
  input: { name: string; rtspUrl: string; transcode?: boolean }
) {
  return apiFetch<ApiCameraDto>(
    `/api/buildings/${buildingId}/cameras`,
    {
      method: 'POST',
      body: JSON.stringify({
        name: input.name,
        rtspUrl: input.rtspUrl,
        transcode: input.transcode ?? false
      })
    }
  )
}

export async function startCameraStream(
  buildingId: string,
  cameraId: string
) {
  return apiFetch<ApiCameraDto>(
    `/api/buildings/${buildingId}/cameras/${cameraId}/stream`
  )
}

export async function getBuildingStages(buildingId: string) {
  return apiFetch<ApiStageDto[]>(`/api/buildings/${buildingId}/stages`)
}

export async function getDefaultWorkflow() {
  return apiFetch<ApiWorkflowDto>(`/api/buildings/workflows/default`)
}

export async function createWorkflow(input: {
  name: string
  stages: Array<{
    id?: string
    name: string
    description?: string
    plannedDays?: number
    position: number
  }>
}) {
  return apiFetch<ApiWorkflowDto>(`/api/buildings/workflows`, {
    method: 'POST',
    body: JSON.stringify({
      name: input.name,
      active: false,
      stages: input.stages
    })
  })
}

export async function getStageSubStages(
  buildingId: string,
  stageId: string
) {
  return apiFetch<ApiSubStageStatusDto[]>(
    `/api/buildings/${buildingId}/stages/${stageId}/substages`
  )
}

export async function completeWorkItem(
  buildingId: string,
  workItemId: string
) {
  return apiFetch(
    `/api/buildings/${buildingId}/work-items/${workItemId}/complete`,
    {
      method: 'POST'
    }
  )
}

export async function advanceBuildingStage(buildingId: string) {
  return apiFetch<ApiProjectDto>(
    `/api/buildings/${buildingId}/stage/next`,
    {
    method: 'POST'
    }
  )
}
