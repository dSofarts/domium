import { ProjectStatus } from '../enums.type'
import { IProject } from '../types/project.interface'

export const PROJECTS: IProject[] = [
  {
    id: 'ff7b6ef1-beb4-4e45-82a6-9bee2bbd58b2',
    projectId: 'efec750d-5f1f-412f-95fd-28f6e324942f',
    clientId: '75dcde26-ef73-4430-8d45-1a9fabda4eab',
    managerId: 'ceb58b4d-5e25-4dfd-8db4-ac719cd66c87',
    name: 'Деревянный дом',
    stage: ProjectStatus.IN_PROGRESS,
    progress: 45,
    videoUrl: 'https://example.com/video1',
    updatedAt: '2024-06-01T12:00:00Z'
  },
  {
    id: '84615b20-6477-4be2-acd2-23bc8d120fc2',
    projectId: '53059e1e-0a9f-4034-a65d-c69864add535',
    clientId: '21c2494f-39aa-464f-8c41-891d189a86e9',
    managerId: 'b713ddb3-de0c-4bbf-965d-de199487c87c',
    name: 'Каркасный дом',
    stage: ProjectStatus.DONE,
    progress: 100,
    videoUrl: 'https://example.com/video1',
    updatedAt: '2024-06-01T12:00:00Z'
  }
]
