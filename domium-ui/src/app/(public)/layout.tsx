import { PropsWithChildren } from 'react'

export default function Layout({ children }: PropsWithChildren) {
  return (
    <div className='min-h-screen'>
      <div className='mx-auto max-w-screen-2xl px-4 sm:px-6 lg:px-8'>
        {children}
      </div>
    </div>
  )
}
