import { reactive } from 'vue'

const USER_ID_KEY = 'livestart_user_id'
const TOKEN_KEY = 'livestart_token'
const CURRENT_USER_KEY = 'livestart_current_user'

export interface CurrentUser {
  id?: number | string
  username?: string
  realName?: string
  phone?: string
}

function readStoredUser(): CurrentUser | null {
  const raw = localStorage.getItem(CURRENT_USER_KEY)
  if (!raw) {
    return null
  }

  try {
    return JSON.parse(raw) as CurrentUser
  } catch {
    localStorage.removeItem(CURRENT_USER_KEY)
    return null
  }
}

export const apiState = reactive({
  isMock: import.meta.env.VITE_USE_MOCK === 'true',
  gatewayUrl: 'http://localhost:8888',
  userId: localStorage.getItem(USER_ID_KEY) || (import.meta.env.VITE_USE_MOCK === 'true' ? '10086' : ''),
  token: localStorage.getItem(TOKEN_KEY) || (import.meta.env.VITE_USE_MOCK === 'true' ? 'mock-user-token-9988' : ''),
  currentUser:
    readStoredUser() ||
    (import.meta.env.VITE_USE_MOCK === 'true'
      ? { username: '陈孟欣(模拟开发)', realName: '陈孟欣' }
      : null),
})

export function persistSession() {
  localStorage.setItem(TOKEN_KEY, apiState.token)
  localStorage.setItem(USER_ID_KEY, apiState.userId)

  if (apiState.currentUser) {
    localStorage.setItem(CURRENT_USER_KEY, JSON.stringify(apiState.currentUser))
  } else {
    localStorage.removeItem(CURRENT_USER_KEY)
  }
}

export function clearSession() {
  apiState.userId = ''
  apiState.token = ''
  apiState.currentUser = null
  localStorage.removeItem(TOKEN_KEY)
  localStorage.removeItem(USER_ID_KEY)
  localStorage.removeItem(CURRENT_USER_KEY)
}
