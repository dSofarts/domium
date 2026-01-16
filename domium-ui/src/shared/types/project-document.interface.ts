import type { DocumentCategory, DocumentStatus } from '@/shared/enums.type'

export interface ProjectDocument {
  id: string
  name: string
  status: DocumentStatus
  category: DocumentCategory
  url?: string
}
