import { Metadata } from 'next'

import { ProjectCard } from '@/components/common/ProjectCard'

import { PROJECTS } from '@/shared/data/projects.data'

export const metadata: Metadata = {
  title: 'Мои проекты',
  description: 'Описание'
}

export default async function ProjectsPage() {
  return (
    <div className='grid grid-cols-1 lg:grid-cols-2 2xl:grid-cols-4 gap-3 w-100%'>
      {PROJECTS.map(project => {
        return (
          <ProjectCard
            key={project.id}
            project={project}
          />
        )
      })}
    </div>
  )
}
