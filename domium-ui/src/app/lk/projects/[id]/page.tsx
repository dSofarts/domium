import { File } from 'lucide-react'
import { Metadata } from 'next'

import ChatUI from '@/components/common/ChatUI'
import { Badge } from '@/components/ui/Badge'
import { AspectRatio } from '@/components/ui/aspect-ratio'

import { PROJECTS } from '@/shared/data/projects.data'
import { ProjectStatusLabel } from '@/shared/enums.type'

export const metadata: Metadata = {
  title: 'Проект',
  description: 'Описание'
}

export default function ProjectPage() {
  const project = PROJECTS[0]
  return (
    <div>
      <div className='flex gap-2 items-center'>
        <h1 className='text-xl font-bold'>{project.name}</h1>
        <Badge>{ProjectStatusLabel[project.stage]}</Badge>
      </div>
      <div className='p-3 mt-2 mb-1 text-sm rounded-2xl bg-secondary max-w-200'>
        <div>Ваш менеджер: {project.managerId}</div>
        <div>Телефон менеджера: +7 999 999 99 99</div>
      </div>
      <div className='mt-5'>
        <h3 className='mb-3 font-bold'>Видео проекта</h3>
        <div className='max-w-200'>
          <AspectRatio
            ratio={16 / 9}
            className='rounded-xl overflow-hidden border'
          >
            <iframe
              src={`https://www.youtube.com/embed/cPqNOWC63mI`}
              title={project.name}
              allow='accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture; web-share'
              allowFullScreen
              className='w-full h-full'
            />
          </AspectRatio>
        </div>
      </div>
      <div className='mt-5'>
        <h3 className='mb-3 font-bold'>Чат с менеджером</h3>
        <ChatUI />
      </div>
      <div className='mt-5'>
        <h3 className='mb-3 font-bold'>Мои документы</h3>
        <div className='flex flex-col gap-4'>
          <div className='flex gap-4 items-center'>
            <File />
            <span className='font-medium'>Договор_123456.pdf</span>
          </div>
          <div className='flex gap-4 items-center'>
            <File />
            <span className='font-medium'>Акт.pdf</span>
          </div>
          <div className='flex gap-4 items-center'>
            <File />
            <span className='font-medium'>Прием.pdf</span>
          </div>
        </div>
      </div>
    </div>
  )
}
