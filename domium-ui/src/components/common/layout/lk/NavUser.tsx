'use client'

import { Avatar, AvatarFallback } from '@/components/ui/Avatar'
import { SidebarMenu, SidebarMenuItem } from '@/components/ui/Sidebar'

import { ChangeThemeButton } from './ChangeThemeButton'
import { useAuth } from '@/shared/auth/AuthProvider'

export function NavUser() {
  const { user } = useAuth()
  const fallbackName = user?.name || 'Гость'
  const fallbackEmail = user?.email || 'Нет данных'

  return (
    <SidebarMenu>
      <SidebarMenuItem>
        <div className='flex items-center gap-3 px-1 py-1.5 text-left text-sm'>
          <Avatar className='h-8 w-8 rounded-2xl'>
            <AvatarFallback>{fallbackName.toUpperCase().charAt(0)}</AvatarFallback>
          </Avatar>
          <div className='grid flex-1 text-left text-sm leading-tight'>
            <span className='truncate font-medium'>{fallbackName}</span>
            <span className='text-muted-foreground truncate text-xs'>
              {fallbackEmail}
            </span>
          </div>
          <ChangeThemeButton />
        </div>
      </SidebarMenuItem>
    </SidebarMenu>
  )
}
