export const ProjectStatus = {
  DONE: 'DONE',
  IN_PROGRESS: 'IN_PROGRESS',
  PLANNED: 'PLANNED'
} as const

export type ProjectStatus = (typeof ProjectStatus)[keyof typeof ProjectStatus]

export const ProjectStatusLabel: Record<ProjectStatus, string> = {
  DONE: 'Завершён',
  IN_PROGRESS: 'В работе',
  PLANNED: 'Запланирован'
}

export const ProjectFlowStage = {
  REQUESTED: 'REQUESTED',
  DOCS: 'DOCS',
  BUILD: 'BUILD',
  SIGN: 'SIGN',
  DONE: 'DONE'
} as const

export type ProjectFlowStage =
  (typeof ProjectFlowStage)[keyof typeof ProjectFlowStage]

export const ProjectFlowStageLabel: Record<ProjectFlowStage, string> = {
  REQUESTED: 'Заявка отправлена',
  DOCS: 'Согласование документов',
  BUILD: 'Строительство',
  SIGN: 'Подписание документов',
  DONE: 'Проект завершён'
}

export const DocumentStatus = {
  PENDING: 'PENDING',
  APPROVED: 'APPROVED',
  SIGNED: 'SIGNED',
  REJECTED: 'REJECTED'
} as const

export type DocumentStatus =
  (typeof DocumentStatus)[keyof typeof DocumentStatus]

export const DocumentStatusLabel: Record<DocumentStatus, string> = {
  PENDING: 'Ожидает',
  APPROVED: 'Согласован',
  SIGNED: 'Подписан',
  REJECTED: 'Отклонён'
}

export const DocumentCategory = {
  DOCS: 'DOCS',
  FINAL: 'FINAL'
} as const

export type DocumentCategory =
  (typeof DocumentCategory)[keyof typeof DocumentCategory]

export const DocumentCategoryLabel: Record<DocumentCategory, string> = {
  DOCS: 'Документы на согласование',
  FINAL: 'Финальные документы'
}
