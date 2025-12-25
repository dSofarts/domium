import { PropsWithChildren } from 'react'

import { PublicHeader } from '@/components/common/layout/public/PublicHeader'

export default function Layout({ children }: PropsWithChildren) {
  return (
    <div className='md:container mx-auto px-4'>
      <PublicHeader />
      {children}
    </div>
  )
}
