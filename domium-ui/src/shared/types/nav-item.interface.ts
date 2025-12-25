import { LucideIcon } from 'lucide-react'

export interface INavItem {
  title: string
  url: string
  icon: LucideIcon
  count?: number
  requireRole?: string
}
