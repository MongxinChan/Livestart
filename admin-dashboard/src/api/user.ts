import http from './http'
import type { UserItem, VisitorItem, PageResult } from '@/types'

export interface CurrentUserResp {
  id: number
  username: string
  realName?: string
  phone?: string
  userType: number
  status?: number
}

export const userApi = {
  page: (params?: { current?: number; size?: number }) =>
    http.get<any, PageResult<UserItem>>('/api/live-start/admin/v1/user/page', { params }),

  visitors: (userId: number) =>
    http.get<any, VisitorItem[]>(`/api/live-start/admin/v1/visitor/list/${userId}`),

  /** 获取当前登录用户完整画像（含 userType，用于角色守卫） */
  me: () =>
    http.get<any, CurrentUserResp>('/api/live-start/admin/v1/user/me'),
}
