import http from './http'
import type { SettlementItem, SettlementNotificationItem, SettlementStats, SettlementShardItem, PageResult } from '@/types'

export const settlementApi = {
  page: (params?: {
    eventId?: number
    keyword?: string
    sortField?: string
    sortOrder?: 'ascend' | 'descend'
    pageNum?: number
    pageSize?: number
  }) =>
    http.get<any, PageResult<SettlementItem>>('/api/live-start/settlement/list', { params }),

  detail: (id: number) =>
    http.get<any, SettlementItem>(`/api/live-start/settlement/detail/${id}`),

  trigger: (eventId: number) =>
    http.post<any, void>(`/api/live-start/settlement/trigger/${eventId}`),

  triggerVisible: () =>
    http.post<any, void>('/api/live-start/settlement/trigger-visible'),

  stats: (eventId?: number) =>
    http.get<any, SettlementStats>('/api/live-start/settlement/stats', { params: { eventId } }),

  shards: (eventId?: number) =>
    http.get<any, SettlementShardItem[]>('/api/live-start/settlement/shards', { params: { eventId } }),

  notifications: () =>
    http.get<any, SettlementNotificationItem[]>('/api/live-start/settlement/notifications'),

  markNotificationRead: (notificationKey: string) =>
    http.post<any, void>('/api/live-start/settlement/notifications/read', null, { params: { notificationKey } }),
}
