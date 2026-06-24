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

export const PRICE_RANGES: PriceRangeOption[] = [
  { label: '不限', minPrice: null, maxPrice: null },
  { label: '100 以下', minPrice: null, maxPrice: 100 },
  { label: '100-300', minPrice: 100, maxPrice: 300 },
  { label: '300-800', minPrice: 300, maxPrice: 800 },
  { label: '800 以上', minPrice: 800, maxPrice: null },
]

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

  const recommendCards = [
    {
      eventId: 102,
      title: '周杰伦 2026「嘉年华」巡演',
      venue: '杭州奥体中心体育场',
      cover: 'https://images.unsplash.com/photo-1540039155733-5bb30b53aa14?auto=format&fit=crop&q=80&w=200',
      priceRange: '580 - 2000',
      tag: '超热万人抢票',
      tagColor: 'volcano',
      status: '正在热卖',
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
      status: '口碑推荐',
      statusColor: 'processing',
    },
  ]

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

      if (priceRange) {
        if (priceRange.minPrice != null && event.minPrice < priceRange.minPrice) {
          return false
        }
        if (priceRange.maxPrice != null && event.minPrice > priceRange.maxPrice) {
          return false
        }
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
    const target = events.value.find((event) => event.id === eventId)
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
