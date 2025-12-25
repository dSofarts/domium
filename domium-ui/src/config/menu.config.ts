import { FolderDot, LogOut } from 'lucide-react'

import { PAGES } from './pages.config'
import { INavItem } from '@/shared/types/nav-item.interface'

export const NAV_MENU: INavItem[] = [
  {
    title: 'Мои проекты',
    url: PAGES.LK,
    icon: FolderDot,
    count: 0
  }
]

export const SECONDARY_MENU: INavItem[] = [
  {
    title: 'Выйти из профиля',
    url: PAGES.HOME,
    icon: LogOut
  }
]
