import http from './http'
import type { EventItem, EventSaveReq, EventUpdateReq, PageResult } from '@/types'

export const eventApi = {
  page: (params?: { current?: number; size?: number }) =>
    http.get<any, PageResult<EventItem>>('/api/merchant-admin/event/page', { params }),

  getById: (id: number) =>
    http.get<any, EventItem>(`/api/merchant-admin/event/${id}`),

  create: (data: EventSaveReq) =>
    http.post<any, void>('/api/merchant-admin/event/create', data),

  update: (data: EventUpdateReq) =>
    http.put<any, void>('/api/merchant-admin/event/update', data),

  delete: (id: number) =>
    http.delete<any, void>(`/api/merchant-admin/event/delete/${id}`),

  publish: (id: number) =>
    http.post<any, void>(`/api/merchant-admin/event/publish/${id}`),

  shelve: (id: number) =>
    http.post<any, void>(`/api/merchant-admin/event/shelve/${id}`),

  terminate: (id: number) =>
    http.post<any, void>(`/api/merchant-admin/event/terminate/${id}`),
}
