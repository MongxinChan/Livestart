import http from './http'
import type { ImportResult, PerformerItem, PerformerSaveReq, PageResult } from '@/types'

export const performerApi = {
  page: (params?: { current?: number; size?: number }) =>
    http.get<any, PageResult<PerformerItem>>('/api/live-start/merchant-admin/performer/page', { params }),

  create: (data: PerformerSaveReq) =>
    http.post<any, void>('/api/live-start/merchant-admin/performer/create', data),

  importExcel: (file: File) => {
    const formData = new FormData()
    formData.append('file', file)
    return http.post<any, ImportResult>('/api/live-start/merchant-admin/performer/import', formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
    })
  },

  update: (data: PerformerItem) =>
    http.put<any, void>('/api/live-start/merchant-admin/performer/update', data),

  delete: (id: number) =>
    http.delete<any, void>(`/api/live-start/merchant-admin/performer/delete/${id}`),
}
