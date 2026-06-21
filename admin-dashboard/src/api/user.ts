import http from './http'
import type { UserItem, VisitorItem, PageResult } from '@/types'

export const userApi = {
  page: (params?: { current?: number; size?: number }) =>
    http.get<any, PageResult<UserItem>>('/api/live-start/admin/v1/user/page', { params }),

  visitors: (userId: number) =>
    http.get<any, VisitorItem[]>(`/api/live-start/admin/v1/visitor/list/${userId}`),
}
