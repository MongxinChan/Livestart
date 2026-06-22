import { ref, computed, onMounted, watch } from 'vue'
import { request } from '@/composables/useRequest'
import type { LiveEvent, HotSearch, CarouselSlide } from '@/types'

export interface PriceRangeOption {
  label: string
  minPrice: number | null
  maxPrice: number | null
}

export const PRICE_RANGES: PriceRangeOption[] = [
  { label: '不限', minPrice: null, maxPrice: null },
  { label: '100 以下', minPrice: null, maxPrice: 100 },
  { label: '100-300', minPrice: 100, maxPrice: 300 },
  { label: '300-800', minPrice: 300, maxPrice: 800 },
  { label: '800 以上', minPrice: 800, maxPrice: null },
]

const CATEGORY_TO_TYPE: Record<string, number | null> = {
  '全部': null,
  '演唱会': 1,
  'Livehouse': 0,
  '音乐节': 2,
}

export function useEventSquare(emit: any) {
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
      desc: '阔别已久，万人狂欢现场。搭载 RocketMQ 异步落库，为您提供极致的物理削峰抢票保航！',
      tag: '超级热门 ⚡ 抢购推荐',
      image: 'https://images.unsplash.com/photo-1540039155733-5bb30b53aa14?auto=format&fit=crop&q=80&w=1200',
      eventId: 102,
    },
    {
      title: '万能青年旅店巡回音乐会 - 上海站',
      desc: '独立摇滚金曲专场。配置高并发 Redis 滑动窗口限流防御，全线护卫售票体系健康！',
      tag: '口碑推荐 🎵 Livehouse',
      image: 'https://images.unsplash.com/photo-1506157786151-b8491531f063?auto=format&fit=crop&q=80&w=1200',
      eventId: 101,
    },
    {
      title: '「重塑雕像的权利」特别专场',
      desc: '后朋克美学极致。内置商户票房多表结算对账，可穿透统计 16 张物理订单分表！',
      tag: '美学前沿 ⚡ 后朋克',
      image: 'https://images.unsplash.com/photo-1514525253161-7a46d19cd819?auto=format&fit=crop&q=80&w=1200',
      eventId: 103,
    },
  ]

  const recommendCards = [
    {
      eventId: 102,
      title: '周杰伦 2026「嘉年华」巡演',
      venue: '杭州奥体中心体育场',
      cover: 'https://images.unsplash.com/photo-1540039155733-5bb30b53aa14?auto=format&fit=crop&q=80&w=200',
      priceRange: '580 - 2000',
      tag: '超火热万人抢票',
      tagColor: 'volcano',
      status: '极速抢票中 ⚡',
      statusColor: 'success',
    },
    {
      eventId: 101,
      title: '万能青年旅店 上海站演出',
      venue: '上海 Modern Sky LAB',
      cover: 'https://images.unsplash.com/photo-1506157786151-b8491531f063?auto=format&fit=crop&q=80&w=200',
      priceRange: '280 - 580',
      tag: '独立摇滚精选',
      tagColor: 'cyan',
      status: '高口碑热卖 🎵',
      statusColor: 'processing',
    },
  ]

  const filteredEvents = computed(() => events.value)

  function buildSearchQueryString(): string {
    const params = new URLSearchParams()
    if (searchQuery.value.trim()) {
      params.set('keyword', searchQuery.value.trim())
    }
    const eventType = CATEGORY_TO_TYPE[activeCategory.value]
    if (eventType !== null && eventType !== undefined) {
      params.set('eventType', String(eventType))
    }
    if (activeCity.value !== '全国') {
      params.set('city', activeCity.value)
    }
    const priceRange = priceRanges.find(p => p.label === activePriceLabel.value)
    if (priceRange?.minPrice != null) {
      params.set('minPrice', String(priceRange.minPrice))
    }
    if (priceRange?.maxPrice != null) {
      params.set('maxPrice', String(priceRange.maxPrice))
    }
    params.set('pageNum', '1')
    params.set('pageSize', '20')
    return params.toString()
  }

  async function fetchEvents() {
    loading.value = true
    try {
      const hasFilter = activeCategory.value !== '全部'
        || activeCity.value !== '全国'
        || activePriceLabel.value !== '不限'
        || searchQuery.value.trim() !== ''

      if (hasFilter) {
        const res = await request<any>('/api/search/event?' + buildSearchQueryString())
        events.value = Array.isArray(res) ? res : (res?.records || [])
      } else {
        events.value = await request<LiveEvent[]>('/api/live-start/engine/event/list')
      }
    } catch (err) {
      console.error('拉取演出失败', err)
    } finally {
      loading.value = false
    }
  }

  async function fetchHotSearches() {
    try {
      hotSearches.value = await request<HotSearch[]>('/api/search/hot')
    } catch (err) {
      console.error('拉取热搜失败', err)
    }
  }

  async function handleSearch() {
    await fetchEvents()
    if (searchQuery.value.trim()) {
      try {
        await request('/api/search/click?keyword=' + encodeURIComponent(searchQuery.value), { method: 'POST' })
        fetchHotSearches()
      } catch (err) {
        console.error('点击热搜失败', err)
      }
    }
  }

  watch([activeCategory, activeCity, activePriceLabel], () => {
    fetchEvents()
  })

  function clickHotWord(word: string) {
    searchQuery.value = word
    handleSearch()
  }

  function clickBannerLink(eventId: number) {
    const target = events.value.find(e => e.id === eventId)
    if (target) emit('selectEvent', target)
  }

  onMounted(() => {
    fetchEvents()
    fetchHotSearches()
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
    clickBannerLink
  }
}
