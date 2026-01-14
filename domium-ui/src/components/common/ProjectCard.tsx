'use client'

import Link from 'next/link'
import React from 'react'

import { PAGES } from '@/config/pages.config'

import { Badge } from '../ui/Badge'
import { Progress } from '../ui/Progress'

import { ProjectStatusLabel } from '@/shared/enums.type'
import { IProject } from '@/shared/types/project.interface'

interface ProjectProps {
  project: IProject
}

export function ProjectCard({ project }: ProjectProps) {
  const [progress, setProgress] = React.useState(0)
  React.useEffect(() => {
    const timer = setTimeout(() => setProgress(project.progress), 500)
    return () => clearTimeout(timer)
  }, [])
  const date = new Date(project.updatedAt)

  return (
    <Link
      href={PAGES.LK_PROJECT(project.id)}
      className='grid p-4 bg-accent rounded-lg cursor-pointer hover:shadow-md transition-shadow'
    >
      <div className='flex justify-between'>
        <h3 className='font-bold text-xl'>{project.name}</h3>
        <Badge>{ProjectStatusLabel[project.stage]}</Badge>
      </div>

      <div className='flex gap-3 items-center mt-5'>
        <Progress value={progress} />
        <span className=''>{progress}%</span>
      </div>
    </Link>
  )
}
