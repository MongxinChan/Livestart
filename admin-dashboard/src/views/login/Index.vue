<template>
  <div class="login-wrapper">
    <!-- 背景光效圆球 -->
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

      <a-form
        :model="loginForm"
        layout="vertical"
        class="login-form"
        @finish="handleLogin"
      >
        <a-form-item
          label="管理员账号 / 手机号"
          name="username"
          :rules="[{ required: true, message: '请输入管理员账号或关联手机号' }]"
        >
          <a-input
            v-model:value="loginForm.username"
            placeholder="账号 (开发测试请输入 admin)"
            size="large"
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
            placeholder="密码 (开发测试请输入 123456)"
            size="large"
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
            登 录
          </a-button>

          <div class="divider-text">
            <span>开发联调通道</span>
          </div>

          <a-button
            type="dashed"
            size="large"
            block
            class="mock-btn"
            :loading="mockLoading"
            @click="handleMockLogin"
          >
            极速 Mock 登入 (默认管理员)
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
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import { ThunderboltOutlined, UserOutlined, LockOutlined } from '@ant-design/icons-vue'
import axios from 'axios'

const router = useRouter()
const loading = ref(false)
const mockLoading = ref(false)

const loginForm = reactive({
  username: '',
  password: '',
})

// 统一写入登录态和跳转
function saveLoginSession(token: string, user: { username: string; userId: string; realName: string }) {
  localStorage.setItem('admin_token', token)
  localStorage.setItem('admin_user', JSON.stringify(user))
  message.success(`欢迎回来，${user.realName}！`)
  router.push('/dashboard')
}

// 模拟登入处理
async function handleMockLogin() {
  mockLoading.value = true
  // 模拟精美的轻微等待延时，提升视觉感知质感
  setTimeout(() => {
    mockLoading.value = false
    saveLoginSession(`mock-admin-token-${Date.now()}`, {
      username: 'admin',
      userId: '1',
      realName: '系统管理员',
    })
  }, 800)
}

// 账号密码或手机号密码登入
async function handleLogin() {
  const { username, password } = loginForm

  // 1. 如果是开发模式默认账密，直接走 Mock 逻辑，保证开发顺畅
  if (username === 'admin' && password === '123456') {
    loading.value = true
    setTimeout(() => {
      loading.value = false
      saveLoginSession(`mock-admin-token-${Date.now()}`, {
        username: 'admin',
        userId: '1',
        realName: '系统管理员',
      })
    }, 600)
    return
  }

  // 2. 如果输入的是手机号，且看起来是个真实手机号，尝试调用后端微服务接口进行登录
  const isPhone = /^[1][3-9]\d{9}$/.test(username)
  if (isPhone) {
    loading.value = true
    try {
      // 本地如果有运行 admin 服务的代理，可以直接发往后端
      const res = await axios.post('/api/live-start/admin/v1/user/login', {
        phone: username,
        password: password,
      })
      const apiResult = res.data
      if (apiResult.code === '0' && apiResult.data?.token) {
        saveLoginSession(apiResult.data.token, {
          username: username,
          userId: '100', // 真实账号这里可以是解析后的 userId 占位
          realName: `用户_${username.substring(7)}`,
        })
      } else {
        message.error(apiResult.message || '登录失败，请检查账号或密码')
      }
    } catch (err: any) {
      console.error(err)
      message.error(err.response?.data?.message || '无法连接到后端登录服务，请使用一键 Mock 登入')
    } finally {
      loading.value = false
    }
  } else {
    // 普通文本账号报错提示
    message.warning('开发模式下，请直接使用 Mock 登入或输入特定账密进行测试')
  }
}
</script>
