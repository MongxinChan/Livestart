import http from './http'
import type { ImportResult, TicketSkuItem, TicketSkuSaveReq, PageResult } from '@/types'

export const ticketSkuApi = {
  page: (params?: { eventId?: number; current?: number; size?: number }) =>
    http.get<any, PageResult<TicketSkuItem>>('/api/live-start/merchant-admin/ticket-sku/page', { params }),

  create: (data: TicketSkuSaveReq) =>
    http.post<any, void>('/api/live-start/merchant-admin/ticket-sku/create', data),

  importExcel: (file: File) => {
    const formData = new FormData()
    formData.append('file', file)
    return http.post<any, ImportResult>('/api/live-start/merchant-admin/ticket-sku/import', formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
    })
  },

  update: (data: TicketSkuItem) =>
    http.put<any, void>('/api/live-start/merchant-admin/ticket-sku/update', data),

  delete: (id: number) =>
    http.delete<any, void>(`/api/live-start/merchant-admin/ticket-sku/delete/${id}`),

  increaseStock: (data: { skuId: number; count: number }) =>
    http.post<any, void>('/api/live-start/merchant-admin/ticket-sku/increase-stock', data),
}
