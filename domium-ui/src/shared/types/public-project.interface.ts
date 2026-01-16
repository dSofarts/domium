export interface IPublicProject {
  id: string
  name: string
  description: string
  image: string
  images?: string[]
  price: number
  type?: string
  category?: string
  material?: string
  location?: string
  floors?: IPublicProjectFloor[]
  videoUrl?: string
  managerId: string
  managerName?: string
  workflowId?: string
}

export interface IPublicProjectFloor {
  id: string
  floorNumber?: number
  rooms?: IPublicProjectRoom[]
}

export interface IPublicProjectRoom {
  id: string
  roomType?: string
  area?: number
}
