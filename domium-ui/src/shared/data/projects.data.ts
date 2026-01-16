import { ProjectFlowStage, ProjectStatus } from '../enums.type'
import { IProject } from '../types/project.interface'

export const PROJECTS: IProject[] = [
  {
    id: 'ff7b6ef1-beb4-4e45-82a6-9bee2bbd58b2',
    projectId: 'efec750d-5f1f-412f-95fd-28f6e324942f',
    clientId: '75dcde26-ef73-4430-8d45-1a9fabda4eab',
    managerId: 'Михаил Иванов',
    name: 'Деревянный дом',
    stage: ProjectStatus.IN_PROGRESS,
    flowStage: ProjectFlowStage.BUILD,
    progress: 19,
    videoUrl: 'https://www.youtube.com/embed/cPqNOWC63mI',
    documents: [
      {
        id: 'doc-1',
        name: 'Договор подряда.pdf',
        status: 'APPROVED',
        category: 'DOCS'
      },
      {
        id: 'doc-2',
        name: 'Техническое задание.pdf',
        status: 'APPROVED',
        category: 'DOCS'
      },
      {
        id: 'doc-3',
        name: 'Акт выполненных работ.pdf',
        status: 'PENDING',
        category: 'FINAL'
      },
      {
        id: 'doc-4',
        name: 'Акт приема-передачи.pdf',
        status: 'PENDING',
        category: 'FINAL'
      }
    ],
    updatedAt: '2024-06-01T12:00:00Z'
  }
]
