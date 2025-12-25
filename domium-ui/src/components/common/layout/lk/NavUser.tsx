import { Avatar, AvatarFallback } from '@/components/ui/Avatar'
import { SidebarMenu, SidebarMenuItem } from '@/components/ui/Sidebar'

import { ChangeThemeButton } from './ChangeThemeButton'
import { IUser } from '@/shared/types/user.interface'

interface NavUserProps {
  user: IUser
}

export function NavUser({ user }: NavUserProps) {
  return (
    <SidebarMenu>
      <SidebarMenuItem>
        <div className='flex items-center gap-3 px-1 py-1.5 text-left text-sm'>
          <Avatar className='h-8 w-8 rounded-2xl'>
            <AvatarFallback>{user.name.toUpperCase().charAt(0)}</AvatarFallback>
          </Avatar>
          <div className='grid flex-1 text-left text-sm leading-tight'>
            <span className='truncate font-medium'>{user.name}</span>
            <span className='text-muted-foreground truncate text-xs'>
              {user.email}
            </span>
          </div>
          <ChangeThemeButton />
        </div>
      </SidebarMenuItem>
    </SidebarMenu>
  )
}
