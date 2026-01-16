import type { Metadata, Viewport } from 'next'
import { Manrope } from 'next/font/google'

import { ThemeProvider } from '@/components/theme/ThemeProvider'
import { AuthProvider } from '@/shared/auth/AuthProvider'

import { SITE_NAME } from '@/constants/seo.constants'
import { BASE_URL } from '@/constants/site.constants'

import './globals.css'

const manropeSans = Manrope({
  subsets: ['latin', 'cyrillic'],
  weight: ['300', '400', '500', '600', '700', '800'],
  display: 'swap'
})

export const metadata: Metadata = {
  title: {
    template: `%s - ${SITE_NAME}`,
    default: `${SITE_NAME}`
  },
  robots: {
    index: true,
    follow: true
  },
  metadataBase: new URL(BASE_URL),
  alternates: {
    canonical: './'
  }
}

export const viewport: Viewport = {
  width: 'device-width',
  initialScale: 1.0
}

export default function RootLayout({
  children
}: Readonly<{
  children: React.ReactNode
}>) {
  return (
    <html
      lang='ru'
      data-scroll-behavior='smooth'
      suppressHydrationWarning
    >
      <body className={`${manropeSans.className} antialiased`}>
        <ThemeProvider
          attribute='class'
          defaultTheme='system'
          enableSystem
          disableTransitionOnChange
        >
          <AuthProvider>{children}</AuthProvider>
        </ThemeProvider>
      </body>
    </html>
  )
}
