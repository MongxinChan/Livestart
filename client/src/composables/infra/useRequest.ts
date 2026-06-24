import { handleMockRequest } from '@/composables/mockHandlers'
import { apiState } from '@/composables/sessionState'

export { apiState } from '@/composables/sessionState'

const API_BASE = import.meta.env.VITE_API_BASE_URL || ''

export async function request<T = any>(url: string, options: RequestInit = {}): Promise<T> {
  if (apiState.isMock) {
    return handleMockRequest(url, options) as Promise<T>
  }

  const fullUrl = url.startsWith('http') ? url : `${API_BASE}${url}`

  const headers: Record<string, string> = {
    'Content-Type': 'application/json',
    token: apiState.token,
    phone: apiState.phone || apiState.currentUser?.phone || '',
    ...((options.headers as Record<string, string>) || {}),
  }

  const response = await fetch(fullUrl, { ...options, headers })

  if (!response.ok) {
    if (response.status === 429) {
      throw new Error('抢票请求过于频繁，请稍后再试 (HTTP 429)')
    }

    const errData = await response.json().catch(() => ({}))
    throw new Error(errData.message || `网络异常 (HTTP ${response.status})`)
  }

  const resJson = await response.json().catch(() => {
    throw new Error('服务端响应格式异常，请稍后重试')
  })
  if (resJson.code !== '0' && resJson.code !== 0 && resJson.code !== '200') {
    throw new Error(resJson.message || '业务请求失败')
  }

  return resJson.data
}
