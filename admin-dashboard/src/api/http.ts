import axios from 'axios'
import { message } from 'ant-design-vue'
import type { ApiResult } from '@/types'

const http = axios.create({
  timeout: 15000,
  headers: { 'Content-Type': 'application/json' },
})

// 请求拦截：注入管理员 token
http.interceptors.request.use((config) => {
  const token = localStorage.getItem('admin_token')
  if (token) {
    config.headers.Authorization = token
  }
  // 开发阶段模拟管理员身份
  config.headers['username'] = 'admin'
  config.headers['userId'] = '1'
  config.headers['realName'] = '系统管理员'
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
