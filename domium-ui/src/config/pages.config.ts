export const PAGES = {
  HOME: '/',
  AUTH: '/auth',
  CREATE_PROJECT: '/add-project',
  PROJECT: (id: string) => `/${id}`,
  LK: '/lk/projects',
  LK_PROJECT: (id: string) => `/lk/projects/${id}`
}
