import { computed, onMounted, onUnmounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { BellOutlined, SearchOutlined, ShoppingOutlined } from '@ant-design/icons-vue'
import { request } from '@/composables/infra/useRequest'
import type { ViewId } from '@/types'

export function useNav() {
  const router = useRouter()

  const navOptions = [
    { value: 'square', label: '演出广场', icon: SearchOutlined },
    { value: 'orders', label: '电子票夹', icon: ShoppingOutlined },
    { value: 'reminders', label: '我的提醒', icon: BellOutlined },
  ]

  function onNavChange(val: string | number) {
    const view = val as ViewId
    if (view === 'square') {
      void router.push({ name: 'Square' })
      return
    }
    if (view === 'orders') {
      void router.push({ name: 'Orders' })
      return
    }
    if (view === 'reminders') {
      void router.push({ name: 'Reminders' })
    }
  }

  function navigateTo(view: ViewId) {
    onNavChange(view)
  }

  const isScrolled = ref(false)

  function handleScroll() {
    isScrolled.value = window.scrollY > 20
  }

  const navSearchVisible = ref(false)
  const navSearchQuery = ref('')
  const navSuggest = ref<string[]>([])
  let suggestTimer: ReturnType<typeof setTimeout> | null = null

  function toggleNavSearch() {
    navSearchVisible.value = !navSearchVisible.value
    if (!navSearchVisible.value) {
      navSearchQuery.value = ''
      navSuggest.value = []
    }
  }

  async function fetchSuggest(keyword: string) {
    if (!keyword.trim()) {
      navSuggest.value = []
      return
    }
    try {
      const res = await request<string[]>(`/api/search/suggest?keyword=${encodeURIComponent(keyword)}&limit=5`)
      navSuggest.value = Array.isArray(res) ? res : []
    } catch {
      navSuggest.value = []
    }
  }

  function onNavSearchInput(val: string) {
    navSearchQuery.value = val
    if (suggestTimer) clearTimeout(suggestTimer)
    suggestTimer = setTimeout(() => fetchSuggest(val), 250)
  }

  onMounted(() => {
    window.addEventListener('scroll', handleScroll, { passive: true })
  })

  onUnmounted(() => {
    window.removeEventListener('scroll', handleScroll)
    if (suggestTimer) clearTimeout(suggestTimer)
  })

  return {
    activeView: computed<ViewId>(() => 'square'),
    navOptions,
    onNavChange,
    navigateTo,
    isScrolled,
    navSearchVisible,
    navSearchQuery,
    navSuggest,
    toggleNavSearch,
    onNavSearchInput,
  }
}
