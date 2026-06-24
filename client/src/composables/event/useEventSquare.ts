import { computed, onMounted, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { request } from '@/composables/infra/useRequest'
import type { CarouselSlide, HotSearch, LiveEvent } from '@/types'
import { resolveEventStageMeta } from '@/utils/eventStage'

export interface PriceRangeOption {
  label: string
  minPrice: number | null
  maxPrice: number | null
}

interface RecommendCard {
  eventId: number
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
    return `¥ ${minPrice}`
  }

  return `¥ ${minPrice} - ¥ ${maxPrice}`
}

function matchesPriceRange(event: LiveEvent, priceRange?: PriceRangeOption) {
  if (!priceRange || (priceRange.minPrice == null && priceRange.maxPrice == null)) {
    return true
  }

  const prices = getEventPrices(event)
  if (prices.length === 0) {
    return false
  }

  return prices.some((price) => {
    if (priceRange.minPrice != null && price < priceRange.minPrice) {
      return false
    }
    if (priceRange.maxPrice != null && price > priceRange.maxPrice) {
      return false
    }
    return true
  })
}

export function useEventSquare(emit: { (e: 'selectEvent', event: LiveEvent): void }) {
  const route = useRoute()
  const searchQuery = ref('')
  const activeCategory = ref('全部')
  const activeCity = ref('全国')
  const activePriceLabel = ref<string>('不限')
  const loading = ref(false)
  const events = ref<LiveEvent[]>([])
  const hotSearches = ref<HotSearch[]>([])

  const citiesList = ['全国', '北京', '上海', '杭州', '广州', '深圳', '成都', '武汉', '西安']
  const categoriesList = ['全部', '演唱会', 'Livehouse', '音乐节']
  const priceRanges = PRICE_RANGES

  const carouselSlides: CarouselSlide[] = [
    {
      title: '「周杰伦」嘉年华巡回演唱会',
      desc: '面向高并发抢票场景，活动状态会实时展示是否待开售、是否已开抢。',
      tag: '超热演出',
      image: 'https://images.unsplash.com/photo-1540039155733-5bb30b53aa14?auto=format&fit=crop&q=80&w=1200',
      eventId: 102,
    },
    {
      title: '万能青年旅店巡回音乐会 - 上海站',
      desc: 'Livehouse 场景同样支持阶段判断和开售提醒预约。',
      tag: 'Livehouse 推荐',
      image: 'https://images.unsplash.com/photo-1506157786151-b8491531f063?auto=format&fit=crop&q=80&w=1200',
      eventId: 101,
    },
    {
      title: '「重塑雕像的权利」特别专场',
      desc: '在演出广场就能看清当前是一开、二开还是已开演。',
      tag: '先锋现场',
      image: 'https://images.unsplash.com/photo-1514525253161-7a46d19cd819?auto=format&fit=crop&q=80&w=1200',
      eventId: 103,
    },
  ]

  const recommendCards = computed<RecommendCard[]>(() => {
    const hotEventIds = [102, 101]
    return hotEventIds
      .map((eventId, index) => {
        const event = events.value.find((item) => Number(item.id) === eventId)
        if (!event) {
          return null
        }

        const stageMeta = resolveEventStageMeta(event)
        return {
          eventId: Number(event.id),
          title: event.title,
          venue: event.venue,
          cover: event.cover,
          priceRange: formatEventPriceRange(event).replace(/^¥\s*/, ''),
          tag: index === 0 ? '超热万人抢票' : '独立摇滚精选',
          tagColor: index === 0 ? 'volcano' : 'cyan',
          status: stageMeta.statusText,
          statusColor: stageMeta.canGrab ? 'success' : 'processing',
        }
      })
      .filter((card): card is RecommendCard => card !== null)
  })

  const filteredEvents = computed(() => {
    const keyword = searchQuery.value.trim().toLowerCase()
    const priceRange = priceRanges.find((item) => item.label === activePriceLabel.value)

    return events.value.filter((event) => {
      if (activeCategory.value !== '全部' && event.type !== activeCategory.value) {
        return false
      }

      if (activeCity.value !== '全国' && event.city !== activeCity.value) {
        return false
      }

      if (!matchesPriceRange(event, priceRange)) {
        return false
      }

      if (!keyword) {
        return true
      }

      const stageMeta = resolveEventStageMeta(event)
      const haystack = [
        event.title,
        event.artist,
        event.venue,
        event.city,
        event.type,
        stageMeta.statusText,
      ]
        .filter(Boolean)
        .join(' ')
        .toLowerCase()

      return haystack.includes(keyword)
    })
  })

  async function fetchEvents() {
    loading.value = true
    try {
      events.value = await request<LiveEvent[]>('/api/live-start/engine/event/list')
    } catch (err) {
      console.error('拉取演出失败', err)
      events.value = []
    } finally {
      loading.value = false
    }
  }

  async function fetchHotSearches() {
    try {
      hotSearches.value = await request<HotSearch[]>('/api/search/hot')
    } catch (err) {
      console.error('拉取热搜失败', err)
      hotSearches.value = []
    }
  }

  async function handleSearch() {
    if (searchQuery.value.trim()) {
      try {
        await request(`/api/search/click?keyword=${encodeURIComponent(searchQuery.value)}`, { method: 'POST' })
        void fetchHotSearches()
      } catch (err) {
        console.error('记录热搜点击失败', err)
      }
    }
  }

  watch([activeCategory, activeCity, activePriceLabel], () => {
    void fetchEvents()
  })

  watch(
    () => route.query.keyword,
    (keyword) => {
      searchQuery.value = typeof keyword === 'string' ? keyword : ''
    },
    { immediate: true }
  )

  function clickHotWord(word: string) {
    searchQuery.value = word
    void handleSearch()
  }

  function clickBannerLink(eventId: number) {
    const target = events.value.find((event) => Number(event.id) === eventId)
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
    loading,
    events,
    hotSearches,
    citiesList,
    categoriesList,
    priceRanges,
    carouselSlides,
    recommendCards,
    filteredEvents,
    fetchEvents,
    fetchHotSearches,
    handleSearch,
    clickHotWord,
    clickBannerLink,
  }
}
