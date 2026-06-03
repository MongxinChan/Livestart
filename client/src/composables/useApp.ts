import { ref, reactive, computed, onMounted, type CSSProperties } from 'vue'
import {
  SearchOutlined,
  ShoppingOutlined,
  FundOutlined,
} from '@ant-design/icons-vue'
import { message } from 'ant-design-vue'
import { themeConfigs, themeOptions, customVars } from '../styles/themes'
import { apiState, request } from './useRequest'
import type { ThemeId, LiveEvent } from '../types'

export function useApp() {
  // --- 主题 ---
  const activeTheme = ref<ThemeId>('cyberpunk-dark')

  const currentAntTheme = computed(() => themeConfigs[activeTheme.value])
  const currentThemeLabel = computed(() => {
    const t = themeOptions.find(x => x.id === activeTheme.value)
    return t ? `${t.icon} ${t.name}` : '主题'
  })

  const rootStyle = computed<CSSProperties>(() => {
    const vars = customVars[activeTheme.value]
    const style: Record<string, string> = {
      minHeight: '100vh',
      transition: 'all 0.3s ease',
    }
    for (const [k, v] of Object.entries(vars)) {
      style[k] = v
    }
    return style as CSSProperties
  })

  function onThemeChange(info: { key: string | number }) {
    activeTheme.value = info.key as ThemeId
  }

  // --- 导航 ---
  type ViewId = 'square' | 'cabin' | 'orders' | 'settlement'
  const activeView = ref<string>('square')
  const selectedEvent = ref<LiveEvent | null>(null)

  const navOptions = [
    { value: 'square', label: '演出发现', icon: SearchOutlined },
    { value: 'orders', label: '电子票包', icon: ShoppingOutlined },
    { value: 'settlement', label: '商户核算', icon: FundOutlined },
  ]

  function onNavChange(val: string | number) {
    activeView.value = val as string
    if (val === 'square') selectedEvent.value = null
  }

  function navigateTo(view: ViewId) {
    activeView.value = view
    if (view === 'square') selectedEvent.value = null
  }

  function selectEventForCabin(event: LiveEvent) {
    selectedEvent.value = event
    activeView.value = 'cabin'
  }

  const showAuthModal = ref(false)
  const authLoading = ref(false)
  const countdown = ref(0)
  const authForm = reactive({
    phone: '',
    code: '',
  })

  // 发送验证码
  async function sendVerificationCode() {
    if (!authForm.phone || authForm.phone.length !== 11) {
      message.warning('请输入正确的11位手机号！')
      return
    }

    if (apiState.isMock) {
      message.success('【模拟短信】验证码已发送，请填入 888888 登录')
      countdown.value = 60
      const timer = setInterval(() => {
        if (countdown.value > 0) countdown.value--
        else clearInterval(timer)
      }, 1000)
      return
    }

    try {
      await request(`/api/live-start/admin/v1/user/send-code?phone=${authForm.phone}`, { method: 'POST' })
      message.success('验证码已成功发送！请查看后端控制台日志打印')
      countdown.value = 60
      const timer = setInterval(() => {
        if (countdown.value > 0) countdown.value--
        else clearInterval(timer)
      }, 1000)
    } catch (err: any) {
      message.error('发送验证码失败: ' + err.message)
    }
  }

  // 登录/自动注册提交
  async function handleAuthSubmit() {
    if (!authForm.phone || authForm.phone.length !== 11) {
      message.warning('请输入正确的11位手机号！')
      return
    }
    if (!authForm.code || authForm.code.length !== 6) {
      message.warning('请输入6位数字验证码！')
      return
    }

    authLoading.value = true
    try {
      if (apiState.isMock) {
        if (authForm.code !== '888888') {
          throw new Error('验证码错误！请填入 888888 登录')
        }
        // Mock 成功登录
        apiState.userId = '20099'
        apiState.token = 'mock-session-token-' + authForm.phone
        apiState.currentUser = {
          username: 'Live_' + authForm.phone.substring(7),
          realName: '新用户 (' + authForm.phone.substring(7) + ')',
          phone: authForm.phone,
        }
        message.success('登录成功！')
        showAuthModal.value = false
        authForm.phone = ''
        authForm.code = ''
        return
      }

      // 联调登录
      const data = await request(`/api/live-start/admin/v1/user/login/code?phone=${authForm.phone}&code=${authForm.code}`, {
        method: 'POST'
      })
      
      // 更新登录状态
      apiState.token = data.token
      
      // 拉取用户信息
      const userRes = await request(`/api/live-start/admin/v1/user/${authForm.phone}`)
      apiState.userId = String(userRes.id)
      apiState.currentUser = userRes

      message.success('验证成功，登录就绪！')
      showAuthModal.value = false
      authForm.phone = ''
      authForm.code = ''
      
      // 强制刷新页面以同步状态
      window.location.reload()
    } catch (err: any) {
      message.error(err.message || '登录验证失败，请重试')
    } finally {
      authLoading.value = false
    }
  }

  // 退出登录
  async function handleLogout() {
    apiState.userId = ''
    apiState.token = ''
    apiState.currentUser = null
    message.success('已安全退出登录态')
    window.location.reload()
  }

  // --- 骨架屏移除 ---
  onMounted(() => {
    const skeleton = document.querySelector('.app-skeleton') as HTMLElement | null
    if (skeleton) {
      setTimeout(() => {
        skeleton.style.opacity = '0'
        setTimeout(() => skeleton.remove(), 300)
      }, 400)
    }
  })

  return {
    activeTheme,
    currentAntTheme,
    currentThemeLabel,
    themeOptions,
    rootStyle,
    onThemeChange,
    activeView,
    selectedEvent,
    navOptions,
    onNavChange,
    navigateTo,
    selectEventForCabin,
    showAuthModal,
    authLoading,
    countdown,
    authForm,
    sendVerificationCode,
    handleAuthSubmit,
    handleLogout,
  }
}
