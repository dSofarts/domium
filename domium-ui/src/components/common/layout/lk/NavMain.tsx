import Link from 'next/link'

import {
  SidebarGroup,
  SidebarGroupContent,
  SidebarMenu,
  SidebarMenuBadge,
  SidebarMenuButton,
  SidebarMenuItem
} from '@/components/ui/Sidebar'

import { NAV_MENU } from '@/config/menu.config'

export function NavMain() {
  return (
    <SidebarGroup>
      <SidebarGroupContent className='flex flex-col gap-2'>
        <SidebarMenu>
          {NAV_MENU.map(item => {
            const Icon = item.icon
            return (
              <Link
                href={item.url}
                key={item.title}
              >
                <SidebarMenuItem>
                  <SidebarMenuButton
                    tooltip={item.title}
                    className='cursor-pointer'
                  >
                    <Icon />
                    <span>{item.title}</span>
                  </SidebarMenuButton>
                  {item.count !== undefined && item.count > 0 && (
                    <SidebarMenuBadge>{item.count}</SidebarMenuBadge>
                  )}
                </SidebarMenuItem>
              </Link>
            )
          })}
        </SidebarMenu>
      </SidebarGroupContent>
    </SidebarGroup>
  )
}
