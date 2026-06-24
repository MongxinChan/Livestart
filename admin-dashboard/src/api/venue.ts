import http from './http'
import type { VenueItem, VenueSaveReq, PageResult } from '@/types'

export const venueApi = {
  page: (params?: { current?: number; size?: number }) =>
    http.get<any, PageResult<VenueItem>>('/api/live-start/merchant-admin/venue/page', { params }),

  create: (data: VenueSaveReq) =>
    http.post<any, void>('/api/live-start/merchant-admin/venue/create', data),

  update: (data: VenueItem) =>
    http.put<any, void>('/api/live-start/merchant-admin/venue/update', data),

  delete: (id: number) =>
    http.delete<any, void>(`/api/live-start/merchant-admin/venue/delete/${id}`),
}
