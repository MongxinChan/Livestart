import http from './http'
import type { PageResult } from '@/types'

export const orderApi = {
  page: (params?: { status?: number; current?: number; size?: number }) =>
    http.get<any, PageResult<any>>('/api/live-start/engine/order/admin/page', { params }),
}
