import http from './http'
import type { TicketSkuItem, TicketSkuSaveReq, PageResult } from '@/types'

export const ticketSkuApi = {
  page: (params?: { eventId?: number; current?: number; size?: number }) =>
    http.get<any, PageResult<TicketSkuItem>>('/api/merchant-admin/ticket-sku/page', { params }),

  create: (data: TicketSkuSaveReq) =>
    http.post<any, void>('/api/merchant-admin/ticket-sku/create', data),

  update: (data: TicketSkuItem) =>
    http.put<any, void>('/api/merchant-admin/ticket-sku/update', data),

  delete: (id: number) =>
    http.delete<any, void>(`/api/merchant-admin/ticket-sku/delete/${id}`),

  increaseStock: (data: { skuId: number; count: number }) =>
    http.post<any, void>('/api/merchant-admin/ticket-sku/increase-stock', data),
}
