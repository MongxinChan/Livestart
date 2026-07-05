import http from './http'
import type { UserItem, VisitorItem, PageResult } from '@/types'

export interface CurrentUserResp {
  id: string
  username: string
  realName?: string
  phone?: string
  userType: number
  status?: number
}

export const userApi = {
  page: (params?: {
    current?: number
    size?: number
    sortField?: string
    sortOrder?: string | null
    userType?: number | undefined
    phone?: string
  }) =>
    http.get<any, PageResult<UserItem>>('/api/live-start/admin/v1/user/page', { params }),

  visitors: (userId: string) =>
    http.get<any, VisitorItem[]>(`/api/live-start/admin/v1/visitor/list/${userId}`),

  me: () =>
    http.get<any, CurrentUserResp>('/api/live-start/admin/v1/user/me'),

  updateUserType: (userId: string, userType: number) =>
    http.put<any, void>('/api/live-start/admin/v1/user/type', null, { params: { userId, userType } }),

  updateStatus: (userId: string, status: number) =>
    http.put<any, void>('/api/live-start/admin/v1/user/status', null, { params: { userId, status } }),

  bindVenueAdmin: (data: { userId: string; venueId: number }) =>
    http.put<any, void>('/api/live-start/admin/v1/user/venue-admin', data),
}
