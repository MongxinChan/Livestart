<template>
  <div class="event-square">
    <!-- 1. 轮播 Banner -->
    <div class="carousel-container" style="margin-bottom: 28px">
      <a-carousel autoplay :dots="true" :autoplay-speed="5000" effect="fade">
        <div v-for="(slide, idx) in carouselSlides" :key="idx" class="carousel-slide-wrap">
          <div class="carousel-slide-bg" :style="{ backgroundImage: `url(${slide.image})` }">
            <div class="carousel-overlay">
              <div style="max-width: 500px">
                <a-tag color="error" style="margin-bottom: 12px; font-weight: 600; border-radius: 6px; padding: 2px 10px">
                  {{ slide.tag }}
                </a-tag>
                <h2 style="font-size: 1.7rem; font-weight: 800; color: #fff; margin-bottom: 10px; line-height: 1.35; text-shadow: 0 4px 10px rgba(0,0,0,0.8); font-family: 'Outfit', sans-serif">
                  {{ slide.title }}
                </h2>
                <p style="color: rgba(255,255,255,0.72); font-size: 0.83rem; margin-bottom: 18px; line-height: 1.45">
                  {{ slide.desc }}
                </p>
                <a-button type="primary" @click="clickBannerLink(slide.eventId)">
                  <template #icon><ThunderboltOutlined /></template>
                  立即准点抢购
                </a-button>
              </div>
            </div>
          </div>
        </div>
      </a-carousel>
    </div>

    <!-- 2. 搜索 + 热搜词云 -->
    <a-card class="glass-panel" style="margin-bottom: 28px; text-align: center" :bordered="false">
      <h2 style="font-size: 1.6rem; font-weight: 800; margin-bottom: 6px">
        搜索您心仪的 <span class="text-gradient">明星 / 现场演出</span>
      </h2>
      <p style="color: var(--ls-text-secondary); margin-bottom: 18px; font-size: 0.86rem">
        内置 Redis ZSet 计分词云排行。每次点击与检索都将动态推升其全网热搜排名。
      </p>
      <a-input-search
        v-model:value="searchQuery"
        placeholder="搜索演出、艺人、Livehouse、场馆..."
        size="large"
        enter-button="搜索"
        style="max-width: 540px"
        @search="handleSearch"
      />
      <!-- 热搜词云 -->
      <div style="display: flex; flex-wrap: wrap; justify-content: center; gap: 8px; margin-top: 16px">
        <span style="font-size: 12px; font-weight: 600; color: var(--ls-text-secondary); margin-right: 4px">🔥 实时热搜:</span>
        <a-tag
          v-for="(hot, idx) in hotSearches"
          :key="idx"
          class="glow-card"
          style="cursor: pointer; border-radius: 16px"
          @click="clickHotWord(hot.keyword)"
        >
          <span style="font-weight: bold; margin-right: 3px">#{{ idx + 1 }}</span>
          {{ hot.keyword }}
          <span style="opacity: 0.6; font-size: 11px; margin-left: 4px">({{ hot.score }})</span>
        </a-tag>
      </div>
    </a-card>

    <!-- 3. 推荐卡片 -->
    <div v-if="!searchQuery" style="margin-bottom: 28px">
      <h3 style="font-size: 1.1rem; font-weight: 800; margin-bottom: 16px; display: flex; align-items: center; gap: 8px">
        <FireOutlined style="color: var(--ant-color-primary)" />
        🔥 答辩特设：高并发热门抢票推荐榜
      </h3>
      <a-row :gutter="[20, 20]">
        <a-col :xs="24" :md="12" v-for="rec in recommendCards" :key="rec.eventId">
          <a-card hoverable class="glow-card" :bordered="false" @click="clickBannerLink(rec.eventId)">
            <div style="display: flex; gap: 16px; align-items: center">
              <img :src="rec.cover" :alt="rec.title" style="width: 80px; height: 110px; object-fit: cover; border-radius: 10px" />
              <div style="flex: 1; display: flex; flex-direction: column; gap: 5px">
                <a-tag :color="rec.tagColor" style="width: fit-content; font-size: 11px">{{ rec.tag }}</a-tag>
                <h4 style="font-size: 0.88rem; font-weight: 700; margin: 0; line-height: 1.35">{{ rec.title }}</h4>
                <span style="font-size: 12px; color: var(--ls-text-secondary)">{{ rec.venue }}</span>
                <div style="display: flex; justify-content: space-between; align-items: center; margin-top: 4px">
                  <span style="font-weight: 700; font-size: 0.88rem">¥ {{ rec.priceRange }}</span>
                  <a-tag :color="rec.statusColor">{{ rec.status }}</a-tag>
                </div>
              </div>
            </div>
          </a-card>
        </a-col>
      </a-row>
    </div>

    <!-- 4. 分类筛选 + 列表 -->
    <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px">
      <a-segmented v-model:value="activeCategory" :options="['全部', '演唱会', 'Livehouse']" />
      <span style="font-size: 13px; color: var(--ls-text-secondary)">
        共匹配 <span style="font-weight: 700">{{ filteredEvents.length }}</span> 场精彩现场
      </span>
    </div>

    <!-- 骨架屏 -->
    <a-row v-if="loading" :gutter="[24, 24]">
      <a-col :xs="24" :sm="12" :lg="8" :xl="6" v-for="i in 4" :key="i">
        <a-card :bordered="false">
          <a-skeleton active :paragraph="{ rows: 4 }" />
        </a-card>
      </a-col>
    </a-row>

    <!-- 演出卡片网格 -->
    <a-row v-else :gutter="[24, 24]">
      <a-col :xs="24" :sm="12" :lg="8" :xl="6" v-for="event in filteredEvents" :key="event.id">
        <a-card hoverable class="glow-card" :bordered="false" :body-style="{ padding: '14px' }">
          <!-- 封面 -->
          <div class="event-cover-wrap" style="height: 180px; margin-bottom: 12px">
            <img :src="event.cover" :alt="event.title" style="width: 100%; height: 100%; object-fit: cover; border-radius: 10px" />
            <a-tag
              :color="event.type === '演唱会' ? 'magenta' : 'cyan'"
              style="position: absolute; top: 10px; left: 10px; border-radius: 6px"
            >
              {{ event.type }}
            </a-tag>
            <a-tag
              v-if="event.ticketStage"
              :color="event.ticketStage === 2 ? 'orange' : 'blue'"
              style="position: absolute; top: 10px; right: 10px; border-radius: 6px"
            >
              {{ event.ticketStage === 2 ? '二开' : '一开' }}
            </a-tag>
          </div>
          <!-- 信息 -->
          <h3 style="font-size: 0.9rem; font-weight: 700; line-height: 1.4; height: 42px; overflow: hidden; display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical; margin-bottom: 8px">
            {{ event.title }}
          </h3>
          <div style="font-size: 12px; color: var(--ls-text-secondary); display: flex; flex-direction: column; gap: 3px; margin-bottom: 12px">
            <span><CalendarOutlined /> {{ event.date }}</span>
            <span><EnvironmentOutlined /> {{ event.venue }}</span>
            <span v-if="event.artist"><UserOutlined /> {{ event.artist }}</span>
          </div>
          <!-- 底部操作 -->
          <a-divider style="margin: 10px 0" />
          <div style="display: flex; justify-content: space-between; align-items: center">
            <div>
              <span style="font-size: 11px; color: var(--ls-text-secondary)">¥ </span>
              <span style="font-size: 1.2rem; font-weight: 800">{{ event.minPrice }}</span>
              <span style="font-size: 11px; color: var(--ls-text-secondary)"> 起</span>
            </div>
            <a-button type="primary" size="small" @click.stop="$emit('selectEvent', event)">
              <template #icon><ThunderboltOutlined /></template>
              立即抢票
            </a-button>
          </div>
        </a-card>
      </a-col>
    </a-row>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue'
import {
  ThunderboltOutlined,
  FireOutlined,
  CalendarOutlined,
  EnvironmentOutlined,
  UserOutlined,
} from '@ant-design/icons-vue'
import { request } from '@/composables/useRequest'
import type { LiveEvent, HotSearch, CarouselSlide } from '@/types'

const emit = defineEmits<{
  selectEvent: [event: LiveEvent]
}>()

// --- 数据 ---
const searchQuery = ref('')
const activeCategory = ref('全部')
const loading = ref(false)
const events = ref<LiveEvent[]>([])
const hotSearches = ref<HotSearch[]>([])

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

// --- 计算属性 ---
const filteredEvents = computed(() => {
  if (activeCategory.value === '全部') return events.value
  return events.value.filter(e => e.type === activeCategory.value)
})

// --- 方法 ---
async function fetchEvents() {
  loading.value = true
  try {
    events.value = await request<LiveEvent[]>('/api/engine/event/list')
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
  if (!searchQuery.value.trim()) {
    fetchEvents()
    return
  }
  loading.value = true
  try {
    events.value = await request<LiveEvent[]>('/api/search/event?keyword=' + encodeURIComponent(searchQuery.value))
    await request('/api/search/click?keyword=' + encodeURIComponent(searchQuery.value))
    fetchHotSearches()
  } catch (err) {
    console.error('搜索失败', err)
  } finally {
    loading.value = false
  }
}

function clickHotWord(word: string) {
  searchQuery.value = word
  handleSearch()
}

function clickBannerLink(eventId: number) {
  const target = events.value.find(e => e.id === eventId)
  if (target) emit('selectEvent', target)
}

// --- 生命周期 ---
onMounted(() => {
  fetchEvents()
  fetchHotSearches()
})
</script>

<style scoped>
.event-cover-wrap {
  position: relative;
}
.carousel-slide-wrap {
  height: 300px;
}
.carousel-slide-bg {
  width: 100%;
  height: 300px;
  background-size: cover;
  background-position: center;
  position: relative;
}
</style>
