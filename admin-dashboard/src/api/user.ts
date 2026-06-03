import http from './http'
import type { UserItem, VisitorItem, PageResult } from '@/types'

export const userApi = {
  page: (params?: { current?: number; size?: number }) =>
    http.get<any, PageResult<UserItem>>('/api/admin/user/page', { params }),

  visitors: (userId: number) =>
    http.get<any, VisitorItem[]>(`/api/admin/visitor/list/${userId}`),
}
