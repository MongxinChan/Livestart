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
          label="手机号"
          name="phone"
          :rules="[{ required: true, message: '请输入 11 位手机号' }]"
        >
          <a-input
            v-model:value="loginForm.phone"
            placeholder="请输入手机号"
            size="large"
            :maxlength="11"
            autocomplete="tel"
          >
            <template #prefix>
              <MobileOutlined style="color: rgba(255, 255, 255, 0.45)" />
            </template>
          </a-input>
        </a-form-item>

        <a-form-item
          label="验证码"
          name="code"
          :rules="[{ required: true, message: '请输入 6 位验证码' }]"
        >
          <div style="display: flex; gap: 8px">
            <a-input
              v-model:value="loginForm.code"
              placeholder="请输入验证码"
              size="large"
              :maxlength="6"
              autocomplete="one-time-code"
              style="flex: 1"
            >
              <template #prefix>
                <SafetyCertificateOutlined style="color: rgba(255, 255, 255, 0.45)" />
              </template>
            </a-input>
            <a-button
              size="large"
              :disabled="countdown > 0"
              :loading="sendingCode"
              @click="sendVerificationCode"
            >
              {{ countdown > 0 ? `${countdown}s` : '获取验证码' }}
            </a-button>
          </div>
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
import { MobileOutlined, SafetyCertificateOutlined, ThunderboltOutlined } from '@ant-design/icons-vue'
import http, { saveAdminSession } from '@/api/http'

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
const sendingCode = ref(false)
const countdown = ref(0)
const showMockEntry = import.meta.env.DEV

const loginForm = reactive({
  phone: '',
  code: '',
})

function startCountdown() {
  countdown.value = 60
  const timer = window.setInterval(() => {
    if (countdown.value > 0) {
      countdown.value -= 1
    } else {
      window.clearInterval(timer)
    }
  }, 1000)
}

async function sendVerificationCode() {
  if (!loginForm.phone || loginForm.phone.length !== 11) {
    message.warning('请输入正确的 11 位手机号')
    return
  }

  sendingCode.value = true
  try {
    await http.post(`/api/live-start/admin/v1/user/send-code?phone=${loginForm.phone}`)
    message.success('验证码已发送，请查看后端控制台日志')
    startCountdown()
  } catch {
    // http 拦截器已处理错误提示
  } finally {
    sendingCode.value = false
  }
}

function persistSession(token: string, phone: string, payload: LoginResponse) {
  const username = payload.username || phone
  const userId = String(payload.userId || '1')
  const realName = payload.realName || username || '管理员'

  saveAdminSession(token, {
    username,
    userId,
    realName,
    phone,
  })

  message.success(`欢迎回来，${realName}`)
  router.push('/dashboard')
}

async function handleMockLogin() {
  mockLoading.value = true

  window.setTimeout(() => {
    mockLoading.value = false
    persistSession(`mock-admin-token-${Date.now()}`, '13800000000', {
      username: 'admin',
      userId: '1',
      realName: '系统管理员',
    })
  }, 400)
}

async function handleLogin() {
  if (!loginForm.phone || loginForm.phone.length !== 11) {
    message.warning('请输入正确的 11 位手机号')
    return
  }
  if (!loginForm.code || loginForm.code.length !== 6) {
    message.warning('请输入 6 位数字验证码')
    return
  }

  loading.value = true

  try {
    const data = await http.post<any, LoginResponse>(
      `/api/live-start/admin/v1/user/login/code?phone=${loginForm.phone}&code=${loginForm.code}`
    )

    if (data?.token) {
      persistSession(data.token, loginForm.phone, data)
      return
    }

    message.error('登录失败，请检查手机号或验证码')
  } catch (err: any) {
    if (!err.response) {
      const fallback =
        showMockEntry
          ? '无法连接登录服务，你可以先使用开发环境的 Mock 登录'
          : '无法连接登录服务，请稍后重试'
      message.error(fallback)
    }
  } finally {
    loading.value = false
  }
}
</script>
