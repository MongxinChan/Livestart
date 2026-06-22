import { reactive } from 'vue'

const USER_ID_KEY = 'livestart_user_id'
const TOKEN_KEY = 'livestart_token'
const CURRENT_USER_KEY = 'livestart_current_user'
const USER_PHONE_KEY = 'livestart_user_phone'

export interface CurrentUser {
  id?: number | string
  username?: string
  realName?: string
  phone?: string
}

function isPhoneValid(phone?: string): boolean {
  if (!phone) return false
  return /^\d{11}$/.test(phone)
}

function readStoredUser(): CurrentUser | null {
  const raw = localStorage.getItem(CURRENT_USER_KEY)
  if (!raw) {
    return null
  }

  try {
    const user = JSON.parse(raw) as CurrentUser
    const phone = localStorage.getItem(USER_PHONE_KEY)
    // 如果用户对象里缺少 phone 或者 phone 为脱敏形态，且本地存有正确的 phone，则进行修复还原
    if (phone && isPhoneValid(phone) && (!user.phone || !isPhoneValid(user.phone))) {
      user.phone = phone
    }
    return user
  } catch {
    localStorage.removeItem(CURRENT_USER_KEY)
    return null
  }
}

function loadInitialState() {
  const isMock = import.meta.env.VITE_USE_MOCK === 'true'
  
  if (isMock) {
    const storedUser = readStoredUser()
    const defaultUser = { username: '陈孟欣(模拟开发)', realName: '陈孟欣', phone: '13012345678' }
    return {
      isMock: true,
      gatewayUrl: 'http://localhost:8888',
      userId: localStorage.getItem(USER_ID_KEY) || '10086',
      token: localStorage.getItem(TOKEN_KEY) || 'mock-user-token-9988',
      phone: localStorage.getItem(USER_PHONE_KEY) || '13012345678',
      currentUser: storedUser || defaultUser
    }
  }

  const token = localStorage.getItem(TOKEN_KEY) || ''
  const userId = localStorage.getItem(USER_ID_KEY) || ''
  const phone = localStorage.getItem(USER_PHONE_KEY) || ''
  const currentUser = readStoredUser()

  // 如果已登录但手机号缺失或为脱敏形态，执行自愈：清除已损坏的登录会话，强制重新登录
  if (token && (!isPhoneValid(phone) && !isPhoneValid(currentUser?.phone))) {
    localStorage.removeItem(TOKEN_KEY)
    localStorage.removeItem(USER_ID_KEY)
    localStorage.removeItem(CURRENT_USER_KEY)
    localStorage.removeItem(USER_PHONE_KEY)
    return {
      isMock: false,
      gatewayUrl: 'http://localhost:8888',
      userId: '',
      token: '',
      phone: '',
      currentUser: null
    }
  }

  return {
    isMock: false,
    gatewayUrl: 'http://localhost:8888',
    userId,
    token,
    phone: phone || currentUser?.phone || '',
    currentUser
  }
}

const initialState = loadInitialState()

export const apiState = reactive({
  isMock: initialState.isMock,
  gatewayUrl: initialState.gatewayUrl,
  userId: initialState.userId,
  token: initialState.token,
  phone: initialState.phone,
  currentUser: initialState.currentUser,
})

export function persistSession() {
  localStorage.setItem(TOKEN_KEY, apiState.token)
  localStorage.setItem(USER_ID_KEY, apiState.userId)
  localStorage.setItem(USER_PHONE_KEY, apiState.phone)

  if (apiState.currentUser) {
    localStorage.setItem(CURRENT_USER_KEY, JSON.stringify(apiState.currentUser))
  } else {
    localStorage.removeItem(CURRENT_USER_KEY)
  }
}

export function clearSession() {
  apiState.userId = ''
  apiState.token = ''
  apiState.phone = ''
  apiState.currentUser = null
  localStorage.removeItem(TOKEN_KEY)
  localStorage.removeItem(USER_ID_KEY)
  localStorage.removeItem(CURRENT_USER_KEY)
  localStorage.removeItem(USER_PHONE_KEY)
}
