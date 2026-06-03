import http from './http'
import type { StyleItem, PageResult } from '@/types'

export interface StyleSaveReq {
  id?: number
  name: string
  code: string
  description: string
}

export const styleApi = {
  page: (params?: { current?: number; size?: number }) =>
    http.get<any, PageResult<StyleItem>>('/api/merchant-admin/style/page', { params }),

  getById: (id: number) =>
    http.get<any, StyleItem>(`/api/merchant-admin/style/${id}`),

  create: (data: StyleSaveReq) =>
    http.post<any, void>('/api/merchant-admin/style/create', data),

  update: (data: StyleSaveReq) =>
    http.put<any, void>('/api/merchant-admin/style/update', data),

  delete: (id: number) =>
    http.delete<any, void>(`/api/merchant-admin/style/delete/${id}`),
}
