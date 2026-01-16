import type { ProjectFlowStage, ProjectStatus } from '../enums.type'
import type { ProjectDocument } from './project-document.interface'

export interface IProject {
  id: string
  projectId: string
  clientId: string
  managerId: string
  managerName?: string
  name: string
  stage: ProjectStatus
  flowStage: ProjectFlowStage
  progress: number
  videoUrl?: string
  documents: ProjectDocument[]
  updatedAt: string
}
