import http from './http'
import type { PageResult } from '@/types'

export const orderApi = {
  page: (params?: { status?: number; eventId?: number; venueId?: number; current?: number; size?: number }) =>
    http.get<any, PageResult<any>>('/api/live-start/engine/order/admin/page', { params }),
  verify: (checkCode: string) =>
    http.post<any, any>('/api/live-start/engine/order/verify', { checkCode }),
  verifyStats: (eventId?: number) =>
    http.get<any, any>('/api/live-start/engine/order/verify/stats', {
      params: eventId == null ? undefined : { eventId },
    }),
  verifyRecords: (eventId?: number) =>
    http.get<any, any>('/api/live-start/engine/order/verify/records', {
      params: eventId == null ? undefined : { eventId },
    }),
}
