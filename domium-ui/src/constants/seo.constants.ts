export const NO_INDEX_PAGE = { robots: { index: false, follow: false } }
export const SITE_NAME = 'DOMIUM'

export const titles = [
  {
    match: (pathname: string) => pathname.startsWith('/lk/projects'),
    title: 'Мои проекты'
  },
  {
    match: (pathname: string) => pathname.startsWith('/lk/settings'),
    title: 'Настройки'
  }
]
