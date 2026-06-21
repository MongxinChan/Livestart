import { ref, onMounted, onUnmounted } from 'vue'
import { SearchOutlined, ShoppingOutlined } from '@ant-design/icons-vue'
import { request } from './useRequest'
import type { LiveEvent, ViewId } from '../types'

export function useNav() {
  const activeView = ref<ViewId>('square')
  const selectedEvent = ref<LiveEvent | null>(null)

  const navOptions = [
    { value: 'square', label: '演出发现', icon: SearchOutlined },
    { value: 'orders', label: '电子票包', icon: ShoppingOutlined },
  ]

  /** 内部统一导航函数，消除 onNavChange / navigateTo 的重复逻辑 */
  function _setView(view: ViewId) {
    activeView.value = view
    if (view === 'square') {
      selectedEvent.value = null
    }
  }

  function onNavChange(val: string | number) {
    _setView(val as ViewId)
  }

  function navigateTo(view: ViewId) {
    _setView(view)
  }

  function selectEventForCabin(event: LiveEvent) {
    selectedEvent.value = event
    activeView.value = 'cabin'
  }

  // ---- Scroll 感知 ----
  const isScrolled = ref(false)

  function handleScroll() {
    isScrolled.value = window.scrollY > 20
  }

  // ---- 导航栏搜索框 ----
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
    activeView,
    selectedEvent,
    navOptions,
    onNavChange,
    navigateTo,
    selectEventForCabin,
    isScrolled,
    navSearchVisible,
    navSearchQuery,
    navSuggest,
    toggleNavSearch,
    onNavSearchInput,
  }
}
