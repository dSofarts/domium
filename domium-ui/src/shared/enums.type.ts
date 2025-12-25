export const ProjectStatus = {
  DONE: 'DONE',
  IN_PROGRESS: 'IN_PROGRESS',
  PLANNED: 'PLANNED'
} as const

export type ProjectStatus = (typeof ProjectStatus)[keyof typeof ProjectStatus]
