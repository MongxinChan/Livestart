import http from './http'
import type { SettlementItem, SettlementStats, PageResult } from '@/types'

export const settlementApi = {
  page: (params?: { eventId?: number; pageNum?: number; pageSize?: number }) =>
    http.get<any, PageResult<SettlementItem>>('/api/live-start/settlement/list', { params }),

  detail: (id: number) =>
    http.get<any, SettlementItem>(`/api/live-start/settlement/detail/${id}`),

  trigger: (eventId: number) =>
    http.post<any, void>(`/api/live-start/settlement/trigger/${eventId}`),

  stats: (eventId?: number) =>
    http.get<any, SettlementStats>('/api/live-start/settlement/stats', { params: { eventId } }),
}
