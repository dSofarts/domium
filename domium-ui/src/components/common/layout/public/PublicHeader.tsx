import { CircleUserRound } from 'lucide-react'
import Link from 'next/link'

import { PAGES } from '@/config/pages.config'

import { Logo } from '../Logo'

export function PublicHeader() {
  return (
    <header className='flex justify-between py-3 mb-3'>
      <Logo />
      <Link
        href={PAGES.LK}
        className='flex gap-2'
      >
        Личный кабинет
        <CircleUserRound />
      </Link>
    </header>
  )
}
