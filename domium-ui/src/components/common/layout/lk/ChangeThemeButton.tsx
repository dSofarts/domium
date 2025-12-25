'use client'

import { Check, Moon, Sun, SunMoon } from 'lucide-react'
import { useTheme } from 'next-themes'

import { Button } from '@/components/ui/Button'
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuGroup,
  DropdownMenuItem,
  DropdownMenuLabel,
  DropdownMenuSeparator,
  DropdownMenuTrigger
} from '@/components/ui/DropdownMenu'
import { useSidebar } from '@/components/ui/Sidebar'

export function ChangeThemeButton() {
  const { isMobile } = useSidebar()
  const { theme, setTheme } = useTheme()

  const isActive = (value: string) => theme === value

  return (
    <DropdownMenu>
      <DropdownMenuTrigger asChild>
        <Button variant='ghost'>
          <SunMoon />
        </Button>
      </DropdownMenuTrigger>
      <DropdownMenuContent
        className='w-(--radix-dropdown-menu-trigger-width) min-w-56 rounded-lg'
        side={isMobile ? 'bottom' : 'right'}
        align='end'
        sideOffset={4}
      >
        <DropdownMenuLabel>Тема оформления</DropdownMenuLabel>
        <DropdownMenuSeparator />
        <DropdownMenuGroup>
          <DropdownMenuItem onClick={() => setTheme('light')}>
            <Sun />
            Светлая
            {isActive('light') && <Check className='ml-auto h-4 w-4' />}
          </DropdownMenuItem>
          <DropdownMenuItem onClick={() => setTheme('dark')}>
            <Moon />
            Темная
            {isActive('dark') && <Check className='ml-auto h-4 w-4' />}
          </DropdownMenuItem>
          <DropdownMenuItem onClick={() => setTheme('system')}>
            <SunMoon />
            Системная
            {isActive('system') && <Check className='ml-auto h-4 w-4' />}
          </DropdownMenuItem>
        </DropdownMenuGroup>
      </DropdownMenuContent>
    </DropdownMenu>
  )
}
