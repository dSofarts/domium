'use client'

import Link from 'next/link'
import { useRouter } from 'next/navigation'

import {
  SidebarGroup,
  SidebarGroupContent,
  SidebarMenu,
  SidebarMenuButton,
  SidebarMenuItem
} from '@/components/ui/Sidebar'

import { SECONDARY_MENU } from '@/config/menu.config'
import { useAuth } from '@/shared/auth/AuthProvider'

export function NavSecondary({
  ...props
}: React.ComponentPropsWithoutRef<typeof SidebarGroup>) {
  const router = useRouter()
  const { logout } = useAuth()

  return (
    <SidebarGroup {...props}>
      <SidebarGroupContent>
        <SidebarMenu>
          {SECONDARY_MENU.map(item => (
            <SidebarMenuItem key={item.title}>
              {item.title === 'Выйти из профиля' ? (
                <SidebarMenuButton
                  onClick={() => {
                    logout()
                    router.push(item.url)
                  }}
                >
                  <item.icon />
                  <span>{item.title}</span>
                </SidebarMenuButton>
              ) : (
                <SidebarMenuButton asChild>
                  <Link href={item.url}>
                    <item.icon />
                    <span>{item.title}</span>
                  </Link>
                </SidebarMenuButton>
              )}
            </SidebarMenuItem>
          ))}
        </SidebarMenu>
      </SidebarGroupContent>
    </SidebarGroup>
  )
}
