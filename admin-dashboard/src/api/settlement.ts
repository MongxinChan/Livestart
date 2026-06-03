import http from './http'
import type { SettlementItem, SettlementStats, PageResult } from '@/types'

export const settlementApi = {
  page: (params?: { eventId?: number; pageNum?: number; pageSize?: number }) =>
    http.get<any, PageResult<SettlementItem>>('/api/settlement/list', { params }),

  detail: (id: number) =>
    http.get<any, SettlementItem>(`/api/settlement/detail/${id}`),

  trigger: (eventId: number) =>
    http.post<any, void>(`/api/settlement/trigger/${eventId}`),

  stats: (eventId?: number) =>
    http.get<any, SettlementStats>('/api/settlement/stats', { params: { eventId } }),
}
