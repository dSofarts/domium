import { ProjectFlowStage } from '@/shared/enums.type'

const PROJECT_UI_KEY = 'domium_project_ui'

interface ProjectUiState {
  flowStage: ProjectFlowStage
}

function readUiMap(): Record<string, ProjectUiState> {
  if (typeof window === 'undefined') return {}
  const raw = window.localStorage.getItem(PROJECT_UI_KEY)
  if (!raw) return {}
  try {
    return JSON.parse(raw) as Record<string, ProjectUiState>
  } catch {
    return {}
  }
}

function writeUiMap(map: Record<string, ProjectUiState>) {
  if (typeof window === 'undefined') return
  window.localStorage.setItem(PROJECT_UI_KEY, JSON.stringify(map))
}

export function getProjectUiState(buildingId: string): ProjectUiState {
  const map = readUiMap()
  const existing = map[buildingId]
  if (existing) return existing

  const initial: ProjectUiState = {
    flowStage: ProjectFlowStage.REQUESTED
  }
  map[buildingId] = initial
  writeUiMap(map)
  return initial
}

export function updateProjectFlowStage(
  buildingId: string,
  flowStage: ProjectFlowStage
) {
  const map = readUiMap()
  const state = map[buildingId] || getProjectUiState(buildingId)
  map[buildingId] = { ...state, flowStage }
  writeUiMap(map)
}
