import axios from 'axios'
import { message } from 'ant-design-vue'
import type { ApiResult } from '@/types'

const http = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '',
  timeout: 15000,
  headers: { 'Content-Type': 'application/json' },
})

// 请求拦截：注入管理员 token
http.interceptors.request.use((config) => {
  const token = localStorage.getItem('admin_token')
  if (token) {
    config.headers.Authorization = token
  }
  
  // 动态读取 localStorage 中的管理员信息注入头（供后端微服务鉴权），不存在则向下兼容降级
  const adminUserStr = localStorage.getItem('admin_user')
  let username = 'admin'
  let userId = '1'
  let realName = '系统管理员'
  if (adminUserStr) {
    try {
      const adminUser = JSON.parse(adminUserStr)
      username = adminUser.username || username
      userId = adminUser.userId || userId
      realName = adminUser.realName || realName
    } catch {
      // 忽略解析异常，降级默认
    }
  }
  config.headers['username'] = username
  config.headers['userId'] = userId
  config.headers['realName'] = realName
  return config
})

// 响应拦截：统一解包 Result<T>
http.interceptors.response.use(
  (response) => {
    const res = response.data as ApiResult
    if (res.code === '0') {
      return res.data
    }
    message.error(res.message || '请求失败')
    return Promise.reject(new Error(res.message))
  },
  (error) => {
    const msg = error.response?.data?.message || error.message || '网络异常'
    message.error(msg)
    return Promise.reject(error)
  }
)

export default http
