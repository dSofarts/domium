import { Metadata } from 'next'

import { Button } from '@/components/ui/Button'

export const metadata: Metadata = {
  title: 'Главная страница',
  description: 'Описание'
}

export default function HomePage() {
  return (
    <div>
      <Button variant={'default'}>Hello World!</Button>
    </div>
  )
}
