import { Label } from '@radix-ui/react-label'
import Image from 'next/image'

import { Button } from '../ui/Button'
import { Input } from '../ui/Input'
import {
  Dialog,
  DialogClose,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
  DialogTrigger
} from '../ui/dialog'

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
          <Dialog>
            <form>
              <DialogTrigger asChild>
                <Button>Заказать</Button>
              </DialogTrigger>
              <DialogContent className='sm:max-w-106.25'>
                <DialogHeader>
                  <DialogTitle>{project.name}</DialogTitle>
                  <DialogDescription>
                    Заполните форму ниже, и мы свяжемся с вами
                  </DialogDescription>
                </DialogHeader>
                <div className='grid gap-4'>
                  <div className='grid gap-3'>
                    <Label htmlFor='name-1'>Имя</Label>
                    <Input
                      id='name-1'
                      name='name'
                      placeholder='Иванов Иван'
                    />
                  </div>
                  <div className='grid gap-3'>
                    <Label htmlFor='username-1'>Телефон</Label>
                    <Input
                      id='phone-1'
                      name='phone'
                      placeholder='+7 (999) 999-99-99'
                    />
                  </div>
                </div>
                <DialogFooter>
                  <DialogClose asChild>
                    <Button variant='outline'>Отменить</Button>
                  </DialogClose>
                  <DialogClose asChild>
                    <Button type='submit'>Отправить</Button>
                  </DialogClose>
                </DialogFooter>
              </DialogContent>
            </form>
          </Dialog>
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
