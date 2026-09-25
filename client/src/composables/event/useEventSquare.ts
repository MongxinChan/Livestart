import { computed, onMounted, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { request } from '@/composables/infra/useRequest'
import { requireAuth } from '@/composables/useAuth'
import type { CarouselSlide, HotSearch, LiveEvent } from '@/types'
import { resolveEventStageMeta } from '@/utils/eventStage'

export interface PriceRangeOption {
  label: string
  minPrice: number | null
  maxPrice: number | null
}

interface RecommendCard {
  eventId: string
  title: string
  venue: string
  cover: string
  priceRange: string
  tag: string
  tagColor: string
  status: string
  statusColor: string
}

export const PRICE_RANGES: PriceRangeOption[] = [
  { label: '不限', minPrice: null, maxPrice: null },
  { label: '100 以下', minPrice: null, maxPrice: 100 },
  { label: '100-300', minPrice: 100, maxPrice: 300 },
  { label: '300-800', minPrice: 300, maxPrice: 800 },
  { label: '800 以上', minPrice: 800, maxPrice: null },
]

function getEventPrices(event: LiveEvent) {
  const skuPrices = (event.skus || [])
    .map((sku) => Number(sku.price))
    .filter((price) => Number.isFinite(price) && price >= 0)

  if (skuPrices.length > 0) {
    return skuPrices
  }

  const fallbackPrice = Number(event.minPrice)
  return Number.isFinite(fallbackPrice) && fallbackPrice >= 0 ? [fallbackPrice] : []
}

export function formatEventPriceRange(event: LiveEvent) {
  const prices = getEventPrices(event).sort((a, b) => a - b)
  if (prices.length === 0) {
    return '价格待定'
  }

  const minPrice = prices[0]
  const maxPrice = prices[prices.length - 1]
  if (minPrice === maxPrice) {
    return `¥${minPrice}`
  }

  return `¥${minPrice} - ¥${maxPrice}`
}

export function useEventSquare(emit: { (e: 'selectEvent', event: LiveEvent): void }) {
  let latestFetchId = 0
  const route = useRoute()
  const searchQuery = ref(typeof route.query.keyword === 'string' ? route.query.keyword : '')
  const activeCategory = ref('全部')
  const activeCity = ref('全国')
  const activePriceLabel = ref<string>('不限')
  const page = ref(1)
  const pageSize = ref(12)
  const loading = ref(false)
  const events = ref<LiveEvent[]>([])
  const totalEvents = ref(0)
  const hotSearches = ref<HotSearch[]>([])

  const citiesList = ['全国', '北京', '上海', '杭州', '广州', '深圳', '成都', '武汉', '西安']
  const categoriesList = ['全部', '演唱会', 'Livehouse']
  const priceRanges = PRICE_RANGES

  const carouselSlides = computed<CarouselSlide[]>(() => events.value
    .filter((event) => event.cover)
    .slice(0, 3)
    .map((event) => ({
      title: event.title,
      desc: [event.artist, event.venue, event.date].filter(Boolean).join(' · '),
      tag: event.type,
      image: event.cover,
      eventId: event.id,
    })))

  const recommendCards = computed<RecommendCard[]>(() => {
    return events.value.slice(0, 2)
      .map((event, index) => {
        const stageMeta = resolveEventStageMeta(event)
        return {
          eventId: String(event.id),
          title: event.title,
          venue: event.venue,
          cover: event.cover,
          priceRange: formatEventPriceRange(event).replace(/^¥/, ''),
          tag: index === 0 ? '热门抢票' : '独立现场',
          tagColor: index === 0 ? 'volcano' : 'cyan',
          status: stageMeta.statusText,
          statusColor: stageMeta.canGrab ? 'success' : 'processing',
        }
      })
  })

  const pagedEvents = computed(() => events.value)

  async function fetchEvents() {
    const fetchId = ++latestFetchId
    loading.value = true
    try {
      const priceRange = priceRanges.find((item) => item.label === activePriceLabel.value)
      const eventType = activeCategory.value === 'Livehouse'
        ? 0
        : activeCategory.value === '演唱会'
          ? 1
          : null
      const params = new URLSearchParams({
        pageNum: String(page.value),
        pageSize: String(pageSize.value),
      })
      if (searchQuery.value.trim()) params.set('keyword', searchQuery.value.trim())
      if (eventType !== null) params.set('eventType', String(eventType))
      if (activeCity.value !== '全国') params.set('city', activeCity.value)
      if (priceRange?.minPrice != null) params.set('minPrice', String(priceRange.minPrice))
      if (priceRange?.maxPrice != null) params.set('maxPrice', String(priceRange.maxPrice))

      const result = await request<{ records?: Array<Partial<LiveEvent>>; total?: number }>(
        `/api/live-start/search/event?${params.toString()}`
      )
      if (fetchId !== latestFetchId) return
      const records = Array.isArray(result) ? result : result.records || []
      totalEvents.value = Array.isArray(result) ? result.length : Number(result.total || 0)
      events.value = records.map((event) => ({
        id: event.id || '',
        title: event.title || '',
        type: event.type || '',
        cover: event.cover || event.posterUrl || '',
        date: event.date || '',
        venue: event.venue || '',
        city: event.city || '',
        artist: event.artist || '',
        minPrice: Number(event.minPrice || 0),
        tags: event.tags || [],
        skus: event.skus || [],
        ticketStage: event.ticketStage,
        status: event.status,
        statusText: event.statusText,
        started: event.started,
      }))
    } catch (err) {
      if (fetchId !== latestFetchId) return
      console.error('拉取演出失败', err)
      events.value = []
      totalEvents.value = 0
    } finally {
      if (fetchId === latestFetchId) {
        loading.value = false
      }
    }
  }

  async function fetchHotSearches() {
    try {
      hotSearches.value = await request<HotSearch[]>('/api/live-start/search/hot')
    } catch (err) {
      console.error('拉取热搜失败', err)
      hotSearches.value = []
    }
  }

  async function handleSearch() {
    page.value = 1
    await fetchEvents()
    if (searchQuery.value.trim()) {
      try {
        await request(`/api/live-start/search/click?keyword=${encodeURIComponent(searchQuery.value)}`, { method: 'POST' })
        void fetchHotSearches()
      } catch (err) {
        console.error('记录热搜点击失败', err)
      }
    }
  }

  watch([activeCategory, activeCity, activePriceLabel], () => {
    page.value = 1
    void fetchEvents()
  })

  watch(searchQuery, () => {
    page.value = 1
  })

  watch(
    () => route.query.keyword,
    (keyword) => {
      searchQuery.value = typeof keyword === 'string' ? keyword : ''
      page.value = 1
      void fetchEvents()
    }
  )

  function clickHotWord(word: string) {
    searchQuery.value = word
    void handleSearch()
  }

  function changePage(nextPage: number, nextPageSize?: number) {
    page.value = nextPage
    if (nextPageSize != null) {
      pageSize.value = nextPageSize
    }
    void fetchEvents()
  }

  function ensureAuthenticatedAction() {
    return requireAuth()
  }

  function clickBannerLink(eventId: number | string) {
    const target = events.value.find((event) => String(event.id) === String(eventId))
    if (target) {
      emit('selectEvent', target)
    }
  }

  onMounted(() => {
    void fetchEvents()
    void fetchHotSearches()
  })

  return {
    searchQuery,
    activeCategory,
    activeCity,
    activePriceLabel,
    page,
    pageSize,
    loading,
    events,
    totalEvents,
    hotSearches,
    citiesList,
    categoriesList,
    priceRanges,
    carouselSlides,
    recommendCards,
    pagedEvents,
    fetchEvents,
    fetchHotSearches,
    handleSearch,
    changePage,
    ensureAuthenticatedAction,
    clickHotWord,
    clickBannerLink,
  }
}
