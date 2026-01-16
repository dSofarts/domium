'use client'

import { CircleUserRound, LogOut } from 'lucide-react'
import Link from 'next/link'
import { usePathname } from 'next/navigation'
import { useEffect, useState } from 'react'

import { PAGES } from '@/config/pages.config'
import { Button } from '@/components/ui/Button'
import { useAuth } from '@/shared/auth/AuthProvider'

import { Logo } from '../Logo'

export function PublicHeader() {
  const { user, logout } = useAuth()
  const pathname = usePathname()
  const [redirectParam, setRedirectParam] = useState('')

  useEffect(() => {
    const target = `${window.location.pathname}${window.location.search}`
    setRedirectParam(encodeURIComponent(target))
  }, [pathname])

  return (
    <header className='flex justify-between py-3 mb-3'>
      <Logo />
      {user ? (
        <div className='flex flex-wrap items-center gap-3'>
          <span className='text-sm text-muted-foreground'>{user.name || 'Пользователь'}</span>
          <Link href={PAGES.LK} className='flex items-center gap-2 text-sm'>
            Личный кабинет
            <CircleUserRound />
          </Link>
          <Button variant='outline' onClick={logout} className='gap-2'>
            Выйти
            <LogOut />
          </Button>
        </div>
      ) : (
        <Button asChild variant='outline'>
          <Link href={`${PAGES.AUTH}?redirect=${redirectParam || encodeURIComponent(pathname)}`}>
            Войти
            <CircleUserRound />
          </Link>
        </Button>
      )}
    </header>
  )
}
