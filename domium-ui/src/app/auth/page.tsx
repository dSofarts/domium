import { Suspense } from 'react'

import AuthClient from './AuthClient'

export default function AuthPage() {
  return (
    <Suspense fallback={<div className='p-6 text-sm'>Загрузка...</div>}>
      <AuthClient />
    </Suspense>
  )
}
