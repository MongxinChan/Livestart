import { reactive, ref } from 'vue'
import { message } from 'ant-design-vue'
import { apiState, request } from './useRequest'
import { clearSession, persistSession } from './sessionState'

// ---- 模块级单例状态（所有调用方共享） ----
const showAuthModal = ref(false)
const showVisitorModal = ref(false)
const authLoading = ref(false)
const countdown = ref(0)
const authForm = reactive({
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
  if (!authForm.phone || authForm.phone.length !== 11) {
    message.warning('请输入正确的 11 位手机号')
    return
  }

  if (apiState.isMock) {
    message.success('【模拟短信】验证码已发送，请填写 888888 登录')
    startCountdown()
    return
  }

  try {
    await request(`/api/live-start/admin/v1/user/send-code?phone=${authForm.phone}`, { method: 'POST' })
    message.success('验证码已发送，请查看后端控制台日志')
    startCountdown()
  } catch (err: any) {
    message.error(`发送验证码失败: ${err.message}`)
  }
}

async function handleAuthSubmit() {
  if (!authForm.phone || authForm.phone.length !== 11) {
    message.warning('请输入正确的 11 位手机号')
    return
  }

  if (!authForm.code || authForm.code.length !== 6) {
    message.warning('请输入 6 位数字验证码')
    return
  }

  authLoading.value = true

  try {
    if (apiState.isMock) {
      if (authForm.code !== '888888') {
        throw new Error('验证码错误，请填写 888888 登录')
      }

      apiState.userId = '20099'
      apiState.token = `mock-session-token-${authForm.phone}`
      apiState.currentUser = {
        username: `Live_${authForm.phone.substring(7)}`,
        realName: `新用户(${authForm.phone.substring(7)})`,
        phone: authForm.phone,
      }
      persistSession()
      message.success('登录成功')
      showAuthModal.value = false
      authForm.phone = ''
      authForm.code = ''
      return
    }

    const data = await request<{ token: string }>(
      `/api/live-start/admin/v1/user/login/code?phone=${authForm.phone}&code=${authForm.code}`,
      { method: 'POST' }
    )

    apiState.token = data.token

    const userRes = await request<any>(`/api/live-start/admin/v1/user/${authForm.phone}`)
    apiState.userId = String(userRes.id)
    apiState.currentUser = userRes
    persistSession()

    message.success('验证成功，登录完成')
    showAuthModal.value = false
    authForm.phone = ''
    authForm.code = ''
    window.location.reload()
  } catch (err: any) {
    message.error(err.message || '登录验证失败，请重试')
  } finally {
    authLoading.value = false
  }
}

async function handleLogout() {
  if (!apiState.isMock && apiState.currentUser) {
    try {
      await request(
        `/api/live-start/admin/v1/user/logout?phone=${apiState.currentUser.phone || ''}`,
        { method: 'DELETE' }
      )
    } catch {
      // Swallow logout errors so local session can still be cleared.
    }
  }

  clearSession()
  message.success('已安全退出登录状态')
  window.location.reload()
}

// ---- Composable 入口（返回共享单例） ----
export function useAuth() {
  return {
    showAuthModal,
    showVisitorModal,
    authLoading,
    countdown,
    authForm,
    sendVerificationCode,
    handleAuthSubmit,
    handleLogout,
  }
}
