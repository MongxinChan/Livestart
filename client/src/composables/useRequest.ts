import { handleMockRequest } from './mockHandlers'
import { apiState } from './sessionState'

export { apiState } from './sessionState'

export async function request<T = any>(url: string, options: RequestInit = {}): Promise<T> {
  if (apiState.isMock) {
    return handleMockRequest(url, options) as Promise<T>
  }

  const headers: Record<string, string> = {
    'Content-Type': 'application/json',
    'User-Id': apiState.userId,
    Authorization: apiState.token,
    token: apiState.token,
    phone: apiState.currentUser?.phone || '',
    ...((options.headers as Record<string, string>) || {}),
  }

  const response = await fetch(url, { ...options, headers })

  if (!response.ok) {
    if (response.status === 429) {
      throw new Error('抢票请求过于频繁，请稍后再试 (HTTP 429)')
    }

    const errData = await response.json().catch(() => ({}))
    throw new Error(errData.message || `网络异常 (HTTP ${response.status})`)
  }

  const resJson = await response.json()
  if (resJson.code !== '0' && resJson.code !== 0 && resJson.code !== '200') {
    throw new Error(resJson.message || '业务请求失败')
  }

  return resJson.data
}
