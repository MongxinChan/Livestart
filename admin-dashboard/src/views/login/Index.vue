<template>
  <div class="login-wrapper">
    <div class="glow-orb orb-1"></div>
    <div class="glow-orb orb-2"></div>

    <div class="login-card">
      <div class="login-header">
        <div class="logo-area">
          <ThunderboltOutlined class="logo-icon" />
          <span class="logo-text">LIVESTART</span>
        </div>
        <p class="subtitle">演出票务后台管理系统</p>
      </div>

      <a-form :model="loginForm" layout="vertical" class="login-form" @finish="handleLogin">
        <a-form-item
          label="管理员账号 / 手机号"
          name="username"
          :rules="[{ required: true, message: '请输入管理员账号或手机号' }]"
        >
          <a-input
            v-model:value="loginForm.username"
            placeholder="账号或手机号"
            size="large"
            autocomplete="username"
          >
            <template #prefix>
              <UserOutlined style="color: rgba(255, 255, 255, 0.45)" />
            </template>
          </a-input>
        </a-form-item>

        <a-form-item
          label="密码"
          name="password"
          :rules="[{ required: true, message: '请输入密码' }]"
        >
          <a-input-password
            v-model:value="loginForm.password"
            placeholder="请输入密码"
            size="large"
            autocomplete="current-password"
          >
            <template #prefix>
              <LockOutlined style="color: rgba(255, 255, 255, 0.45)" />
            </template>
          </a-input-password>
        </a-form-item>

        <a-form-item class="actions-area">
          <a-button
            type="primary"
            html-type="submit"
            size="large"
            :loading="loading"
            block
            class="login-btn"
          >
            登录
          </a-button>

          <div v-if="showMockEntry" class="divider-text">
            <span>开发联调入口</span>
          </div>

          <a-button
            v-if="showMockEntry"
            type="dashed"
            size="large"
            block
            class="mock-btn"
            :loading="mockLoading"
            @click="handleMockLogin"
          >
            一键 Mock 登录（仅开发环境）
          </a-button>
        </a-form-item>
      </a-form>

      <div class="login-footer">
        <span>Livestart Admin Dashboard &copy; 2026 陈孟欣</span>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import axios from 'axios'
import { LockOutlined, ThunderboltOutlined, UserOutlined } from '@ant-design/icons-vue'
import { saveAdminSession } from '@/api/http'

interface LoginResponse {
  token?: string
  userId?: string | number
  username?: string
  realName?: string
  phone?: string
}

const router = useRouter()
const loading = ref(false)
const mockLoading = ref(false)
const showMockEntry = import.meta.env.DEV

const loginForm = reactive({
  username: '',
  password: '',
})

function persistSession(token: string, payload: LoginResponse) {
  const username = payload.username || loginForm.username
  const userId = String(payload.userId || '1')
  const realName = payload.realName || username || '管理员'

  saveAdminSession(token, {
    username,
    userId,
    realName,
    phone: payload.phone || '',
  })

  message.success(`欢迎回来，${realName}`)
  router.push('/dashboard')
}

async function handleMockLogin() {
  mockLoading.value = true

  window.setTimeout(() => {
    mockLoading.value = false
    persistSession(`mock-admin-token-${Date.now()}`, {
      username: 'admin',
      userId: '1',
      realName: '系统管理员',
    })
  }, 400)
}

async function handleLogin() {
  loading.value = true

  try {
    const res = await axios.post('/api/admin/user/login', {
      username: loginForm.username,
      password: loginForm.password,
    })

    const apiResult = res.data as { code?: string; message?: string; data?: LoginResponse }
    if (apiResult.code === '0' && apiResult.data?.token) {
      persistSession(apiResult.data.token, apiResult.data)
      return
    }

    message.error(apiResult.message || '登录失败，请检查账号或密码')
  } catch (err: any) {
    const fallback =
      showMockEntry
        ? '无法连接登录服务，你可以先使用开发环境的 Mock 登录'
        : '无法连接登录服务，请稍后重试'
    message.error(err.response?.data?.message || fallback)
  } finally {
    loading.value = false
  }
}
</script>
