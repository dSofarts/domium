import { PublicProjectCard } from './PublicProjectCard'
import { PUBLIC_PROJECTS } from '@/shared/data/public-projects.data'

export function PublicProjects() {
  const projects = PUBLIC_PROJECTS
  return (
    <div>
      <h1 className='text-xl font-medium'>Наши проекты</h1>
      <div className='mt-4 grid gap-5 grid-cols-1 sm:grid-cols-2 xl:grid-cols-4'>
        {projects.map(project => {
          return (
            <PublicProjectCard
              key={project.id}
              project={project}
            />
          )
        })}
      </div>
    </div>
  )
}
