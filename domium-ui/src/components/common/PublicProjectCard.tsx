import Image from 'next/image'

import { Button } from '../ui/Button'

import { IPublicProject } from '@/shared/types/public-project.interface'

interface ProjectProps {
  project: IPublicProject
}

export function PublicProjectCard({ project }: ProjectProps) {
  return (
    <div className='bg-accent rounded-md overflow-hidden'>
      <div className='relative w-full aspect-video'>
        <Image
          src={project.image}
          alt={project.name}
          fill
          className='object-cover rounded-md'
          loading='lazy'
        />
      </div>
      <div className='p-3'>
        <h2 className='font-bold'>{project.name}</h2>
        <div className='flex justify-between items-center mt-5'>
          <Button>Заказать</Button>
          {project.price && (
            <span className='font-medium text-sm block'>
              {project.price.toLocaleString('ru-RU')} ₽
            </span>
          )}
        </div>
      </div>
    </div>
  )
}
