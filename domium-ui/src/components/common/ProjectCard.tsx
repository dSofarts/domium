'use client'

import Link from 'next/link'
import React from 'react'

import { PAGES } from '@/config/pages.config'

import { Progress } from '../ui/Progress'

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

  return (
    <Link
      href={PAGES.LK_PROJECT(project.id)}
      className='grid p-4 bg-accent rounded-lg cursor-pointer hover:shadow-md transition-shadow'
    >
      <h3 className='font-bold'>{project.name}</h3>
      <br />
      <Progress value={progress} />
    </Link>
  )
}
