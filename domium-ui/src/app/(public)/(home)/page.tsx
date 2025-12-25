import { Metadata } from 'next'

import { PublicProjects } from '@/components/common/PublicProjects'

export const metadata: Metadata = {
  title: 'Главная страница',
  description: 'Описание'
}

export default function HomePage() {
  return (
    <div className='w-auto'>
      <PublicProjects />
    </div>
  )
}
