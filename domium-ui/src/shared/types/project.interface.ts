import { ProjectStatus } from '../enums.type'

export interface IProject {
  id: string
  projectId: string
  clientId: string
  managerId: string
  name: string
  stage: ProjectStatus
  progress: number
  videoUrl?: string
  updatedAt: string
}
