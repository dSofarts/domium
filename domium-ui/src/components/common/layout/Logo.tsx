import { HousePlus } from 'lucide-react'
import Link from 'next/link'

import { SITE_NAME } from '@/constants/seo.constants'

import { PAGES } from '@/config/pages.config'

export function Logo() {
  return (
    <Link
      href={PAGES.HOME}
      className='items-center flex gap-2'
    >
      <HousePlus className='size-5!' />
      <span className='text-base font-bold'>{SITE_NAME}</span>
    </Link>
  )
}
