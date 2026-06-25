import axios from 'axios'
import { message } from 'ant-design-vue'
import type { AxiosError } from 'axios'
import type { ApiResult } from '@/types'

/**
 * admin-dashboard 用户角色（与后端 t_user.user_type 对齐）
 *   1 = 乐迷    （禁止登录后台）
 *   2 = 艺人    （考虑中，当前禁止登录后台）
 *   3 = 场地管理员/主办方
 *   4 = 超级管理员
 */
export const UserRole = {
  Fan: 1,
  Artist: 2,
  VenueAdmin: 3,
  SuperAdmin: 4,
} as const
export type UserRoleValue = typeof UserRole[keyof typeof UserRole]

/** admin-dashboard 允许登录的角色（场地管理员 + 超管） */
export const ADMIN_DASHBOARD_ALLOWED_ROLES: UserRoleValue[] = [UserRole.VenueAdmin, UserRole.SuperAdmin]

export interface AdminSession {
  username: string
  userId: string
  realName: string
  phone?: string
  userType?: UserRoleValue
}

const TOKEN_KEY = 'admin_token'
const USER_KEY = 'admin_user'

export function getAdminToken(): string {
  return localStorage.getItem(TOKEN_KEY) || ''
}

export function getAdminSession(): AdminSession | null {
  const raw = localStorage.getItem(USER_KEY)
  if (!raw) {
    return null
  }

  try {
    return JSON.parse(raw) as AdminSession
    // const session = JSON.parse(raw) as AdminSession & { userType?: UserRoleValue | string | number }
    // const normalizedUserType =
    //   session.userType === undefined || session.userType === null
    //     ? undefined
    //     : Number(session.userType)
    //
    // return {
    //   ...session,
    //   userType:
    //     normalizedUserType && [UserRole.Fan, UserRole.Artist, UserRole.VenueAdmin, UserRole.SuperAdmin].includes(normalizedUserType)
    //       ? (normalizedUserType as UserRoleValue)
    //       : undefined,
    // }
  } catch {
    localStorage.removeItem(USER_KEY)
    return null
  }
}

export function saveAdminSession(token: string, session: AdminSession) {
  localStorage.setItem(TOKEN_KEY, token)
  localStorage.setItem(USER_KEY, JSON.stringify(session))
  // console.log('[AdminSession] saveAdminSession', {
  //   hasToken: !!token,
  //   tokenPreview: token ? token.slice(0, 20) : '',
  //   session,
  // })
}

export function updateAdminSession(patch: Partial<AdminSession>) {
  const current = getAdminSession()
  if (!current) return
  const next = { ...current, ...patch }
  localStorage.setItem(USER_KEY, JSON.stringify(next))
  // console.log('[AdminSession] updateAdminSession', {
  //   patch,
  //   next,
  // })
}

export function clearAdminSession(reason = 'unknown') {
  // console.warn('[AdminSession] clearAdminSession', {
  //   reason,
  //   token: localStorage.getItem(TOKEN_KEY),
  //   user: localStorage.getItem(USER_KEY),
  //   stack: new Error().stack,
  // })
  localStorage.removeItem(TOKEN_KEY)
  localStorage.removeItem(USER_KEY)
}

export function isSuperAdmin(): boolean {
  return getAdminSession()?.userType === UserRole.SuperAdmin
}

export function getCurrentUserType(): UserRoleValue | undefined {
  return getAdminSession()?.userType
}

function toSafeNumber(value: unknown): number | undefined {
  if (typeof value === 'number' && Number.isFinite(value)) {
    return value
  }

  if (typeof value === 'string' && value.trim() !== '') {
    const parsed = Number(value)
    if (Number.isFinite(parsed)) {
      return parsed
    }
  }

  return undefined
}

function normalizePageResult<T>(data: T): T {
  if (!data || typeof data !== 'object' || !Array.isArray((data as { records?: unknown }).records)) {
    return data
  }

  const pageData = data as T & {
    total?: unknown
    size?: unknown
    current?: unknown
    pages?: unknown
  }

  const normalizedTotal = toSafeNumber(pageData.total)
  const normalizedSize = toSafeNumber(pageData.size)
  const normalizedCurrent = toSafeNumber(pageData.current)
  const normalizedPages = toSafeNumber(pageData.pages)

  if (
    normalizedTotal === undefined &&
    normalizedSize === undefined &&
    normalizedCurrent === undefined &&
    normalizedPages === undefined
  ) {
    return data
  }

  return {
    ...pageData,
    ...(normalizedTotal !== undefined ? { total: normalizedTotal } : {}),
    ...(normalizedSize !== undefined ? { size: normalizedSize } : {}),
    ...(normalizedCurrent !== undefined ? { current: normalizedCurrent } : {}),
    ...(normalizedPages !== undefined ? { pages: normalizedPages } : {}),
  }
}

const http = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '',
  timeout: 15000,
  headers: { 'Content-Type': 'application/json' },
})

http.interceptors.request.use((config) => {
  const token = getAdminToken()
  const adminUser = getAdminSession()

  if (token) {
    config.headers.Authorization = token
    config.headers.token = token
  }

  if (adminUser) {
    config.headers.username = adminUser.username
    config.headers.userId = adminUser.userId
    config.headers.realName = adminUser.realName
    if (adminUser.userType !== undefined) {
      config.headers.userType = String(adminUser.userType)
    }
    if (adminUser.phone) {
      config.headers.phone = adminUser.phone
    }
  }

  return config
})

http.interceptors.response.use(
  (response) => {
    const res = response.data as ApiResult
    if (res.code === '0' || res.code === '200') {
      return normalizePageResult(res.data)
    }

    message.error(res.message || '请求失败')
    return Promise.reject(new Error(res.message || 'Request failed'))
  },
  (error: AxiosError<{ message?: string }>) => {
    const status = error.response?.status

    if (status === 401) {
      clearAdminSession('http-401')
      window.location.href = '/login'
    }

    const msg = error.response?.data?.message || error.message || '网络异常'
    message.error(msg)
    return Promise.reject(error)
  }
)

export default http
