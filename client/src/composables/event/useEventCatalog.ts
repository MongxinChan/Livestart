import { request } from '@/composables/infra/useRequest'
import type { LiveEvent } from '@/types'

export async function fetchEventList() {
  return request<LiveEvent[]>('/api/live-start/engine/event/list')
}

export async function fetchEventById(eventId: number | string) {
  return request<LiveEvent | null>(`/api/live-start/engine/event/${String(eventId)}`)
}
