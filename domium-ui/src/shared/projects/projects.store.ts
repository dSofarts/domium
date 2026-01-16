import { API_URL } from '@/constants/site.constants'
import { ProjectFlowStage, ProjectStatus } from '@/shared/enums.type'
import type { IPublicProject } from '@/shared/types/public-project.interface'
import type { IProject } from '@/shared/types/project.interface'
import {
  advanceBuildingStage,
  createBuilding,
  getBuildingDetails,
  getMyBuildings,
  type ApiProjectDto
} from '@/shared/api/buildings'
import { createProjectOrder } from '@/shared/api/projects'
import { getProjectUiState, updateProjectFlowStage } from './project-ui.store'

const PENDING_ORDER_KEY = 'domium_pending_order'

interface PendingOrder {
  projectId: string
  name: string
  phone: string
}

function mapProjectDtoToUi(project: ApiProjectDto): IProject {
  const uiState = getProjectUiState(project.buildingId)
  const progress = project.objectInfo?.progress ?? 0
  const stage =
    progress >= 100
      ? ProjectStatus.DONE
      : progress > 0
        ? ProjectStatus.IN_PROGRESS
        : ProjectStatus.PLANNED
  const resolvedFlowStage =
    progress >= 100
      ? ProjectFlowStage.DONE
      : progress > 0 && uiState.flowStage === ProjectFlowStage.REQUESTED
        ? ProjectFlowStage.BUILD
        : uiState.flowStage

  return {
    id: project.buildingId,
    projectId: project.id,
    clientId: project.clientId || '',
    managerId: project.manager?.id || '',
    managerName: project.manager?.name || 'Менеджер',
    name: project.objectInfo?.projectName || 'Проект',
    stage,
    flowStage: resolvedFlowStage,
    progress,
    videoUrl: `${API_URL}/api/buildings/${project.buildingId}/video`,
    documents: [],
    updatedAt: new Date().toISOString()
  }
}

export async function loadProjects(): Promise<IProject[]> {
  const projects = await getMyBuildings()
  return projects.map(mapProjectDtoToUi)
}

export async function loadProject(buildingId: string): Promise<IProject | null> {
  const details = await getBuildingDetails(buildingId)
  if (!details?.project) return null
  return mapProjectDtoToUi(details.project)
}

export async function addProjectFromPublic(
  publicProject: IPublicProject,
  contact: { name: string; phone: string }
) {
  await createProjectOrder(publicProject.id)
  const created = await createBuilding({
    projectId: publicProject.id,
    projectName: publicProject.name,
    managerId: publicProject.managerId,
    managerName: publicProject.managerName,
    workflowId: publicProject.workflowId,
    attributes: {
      contactName: contact.name,
      contactPhone: contact.phone
    }
  })
  updateProjectFlowStage(created.buildingId, ProjectFlowStage.REQUESTED)
  return mapProjectDtoToUi(created)
}

export function updateProjectFlowStageUi(
  projectId: string,
  flowStage: ProjectFlowStage
) {
  updateProjectFlowStage(projectId, flowStage)
}

export async function advanceProjectStage(projectId: string) {
  return advanceBuildingStage(projectId)
}

export function storePendingOrder(order: PendingOrder) {
  if (typeof window === 'undefined') return
  window.localStorage.setItem(PENDING_ORDER_KEY, JSON.stringify(order))
}

export function readPendingOrder() {
  if (typeof window === 'undefined') return null
  const raw = window.localStorage.getItem(PENDING_ORDER_KEY)
  if (!raw) return null

  try {
    return JSON.parse(raw) as PendingOrder
  } catch {
    return null
  }
}

export function clearPendingOrder() {
  if (typeof window === 'undefined') return
  window.localStorage.removeItem(PENDING_ORDER_KEY)
}
