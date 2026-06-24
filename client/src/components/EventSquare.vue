<template>
  <div class="event-square">
    <div class="carousel-container" style="margin-bottom: 28px">
      <a-carousel autoplay :dots="true" :autoplay-speed="5000" effect="fade">
        <div v-for="(slide, idx) in carouselSlides" :key="idx" class="carousel-slide-wrap">
          <div class="carousel-slide-bg" :style="{ backgroundImage: `url(${slide.image})` }">
            <div class="carousel-overlay">
              <div style="max-width: 500px">
                <a-tag color="error" style="margin-bottom: 12px; font-weight: 600; border-radius: 6px; padding: 2px 10px">
                  {{ slide.tag }}
                </a-tag>
                <h2
                  style="font-size: 1.7rem; font-weight: 800; color: #fff; margin-bottom: 10px; line-height: 1.35; text-shadow: 0 4px 10px rgba(0,0,0,0.8); font-family: 'Outfit', sans-serif"
                >
                  {{ slide.title }}
                </h2>
                <p style="color: rgba(255,255,255,0.72); font-size: 0.83rem; margin-bottom: 18px; line-height: 1.45">
                  {{ slide.desc }}
                </p>
                <a-button type="primary" @click="clickBannerLink(normalizeEventId(slide.eventId))">
                  <template #icon><ThunderboltOutlined /></template>
                  立即查看
                </a-button>
              </div>
            </div>
          </div>
        </div>
      </a-carousel>
    </div>

    <a-card class="glass-panel" style="margin-bottom: 28px; text-align: center" :bordered="false">
      <h2 style="font-size: 1.6rem; font-weight: 800; margin-bottom: 6px">
        搜索你关注的 <span class="text-gradient">明星 / 演出</span>
      </h2>
      <p style="color: var(--ls-text-secondary); margin-bottom: 18px; font-size: 0.86rem">
        当前会直接展示一开、二开、待开售、抢票中还是已开演，不用再自己猜测。
      </p>
      <a-input-search
        v-model:value="searchQuery"
        placeholder="搜索演出、艺人、Livehouse、场馆..."
        size="large"
        enter-button="搜索"
        style="max-width: 540px"
        @search="handleSearch"
      />
      <div style="display: flex; flex-wrap: wrap; justify-content: center; gap: 8px; margin-top: 16px">
        <span style="font-size: 12px; font-weight: 600; color: var(--ls-text-secondary); margin-right: 4px">实时热搜:</span>
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

    <div v-if="!searchQuery" style="margin-bottom: 28px">
      <h3 style="font-size: 1.1rem; font-weight: 800; margin-bottom: 16px; display: flex; align-items: center; gap: 8px">
        <FireOutlined style="color: var(--ant-color-primary)" />
        热门推荐
      </h3>
      <a-row :gutter="[20, 20]">
        <a-col :xs="24" :md="12" v-for="rec in recommendCards" :key="rec.eventId">
          <a-card hoverable class="glow-card" :bordered="false" @click="clickBannerLink(normalizeEventId(rec.eventId))">
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

    <div
      class="filters-panel"
      style="margin-bottom: 24px; padding: 18px 24px; background: rgba(255,255,255,0.01); border: 1px solid rgba(255,255,255,0.06); border-radius: 12px"
    >
      <div class="filter-row" style="margin-bottom: 14px; display: flex; align-items: center; gap: 16px">
        <span class="filter-label" style="font-size: 13px; font-weight: 700; color: var(--ls-text-secondary); min-width: 50px">城市:</span>
        <div class="filter-options" style="display: flex; flex-wrap: wrap; gap: 8px">
          <span
            v-for="c in citiesList"
            :key="c"
            :class="['filter-tag', { active: activeCity === c }]"
            @click="activeCity = c"
          >
            {{ c }}
          </span>
        </div>
      </div>
      <div class="filter-row" style="margin-bottom: 14px; display: flex; align-items: center; gap: 16px">
        <span class="filter-label" style="font-size: 13px; font-weight: 700; color: var(--ls-text-secondary); min-width: 50px">分类:</span>
        <div class="filter-options" style="display: flex; flex-wrap: wrap; gap: 8px">
          <span
            v-for="cat in categoriesList"
            :key="cat"
            :class="['filter-tag', { active: activeCategory === cat }]"
            @click="activeCategory = cat"
          >
            {{ cat }}
          </span>
        </div>
      </div>
      <div class="filter-row" style="display: flex; align-items: center; gap: 16px">
        <span class="filter-label" style="font-size: 13px; font-weight: 700; color: var(--ls-text-secondary); min-width: 50px">价格:</span>
        <div class="filter-options" style="display: flex; flex-wrap: wrap; gap: 8px">
          <span
            v-for="p in priceRanges"
            :key="p.label"
            :class="['filter-tag', { active: activePriceLabel === p.label }]"
            @click="activePriceLabel = p.label"
          >
            {{ p.label }}
          </span>
        </div>
      </div>
    </div>

    <div style="display: flex; justify-content: flex-end; align-items: center; margin-bottom: 16px">
      <span style="font-size: 13px; color: var(--ls-text-secondary)">
        已为你找到 <span style="font-weight: 700; color: var(--ant-color-primary)">{{ filteredEvents.length }}</span> 场演出
      </span>
    </div>

    <a-row v-if="loading" :gutter="[24, 24]">
      <a-col :xs="24" :sm="12" :lg="8" :xl="6" v-for="i in 4" :key="i">
        <a-card :bordered="false">
          <a-skeleton active :paragraph="{ rows: 4 }" />
        </a-card>
      </a-col>
    </a-row>

    <a-row v-else :gutter="[24, 24]">
      <a-col :xs="24" :sm="12" :lg="8" :xl="6" v-for="event in filteredEvents" :key="event.id">
        <a-card hoverable class="glow-card" :bordered="false" :body-style="{ padding: '14px' }">
          <div class="event-cover-wrap" style="height: 180px; margin-bottom: 12px; position: relative">
            <img :src="event.cover" :alt="event.title" style="width: 100%; height: 100%; object-fit: cover; border-radius: 10px" />
            <a-tag
              :color="event.type === '演唱会' ? 'magenta' : 'cyan'"
              style="position: absolute; top: 10px; left: 10px; border-radius: 6px"
            >
              {{ event.type }}
            </a-tag>
            <a-tag
              :color="resolveEventStageMeta(event).stageColor"
              style="position: absolute; top: 10px; right: 10px; border-radius: 6px"
            >
              {{ resolveEventStageMeta(event).stageLabel }}
            </a-tag>
          </div>

          <h3
            style="font-size: 0.9rem; font-weight: 700; line-height: 1.4; height: 42px; overflow: hidden; display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical; margin-bottom: 8px"
          >
            {{ event.title }}
          </h3>
          <div style="font-size: 12px; color: var(--ls-text-secondary); display: flex; flex-direction: column; gap: 3px; margin-bottom: 12px">
            <span><CalendarOutlined /> {{ event.date }}</span>
            <span><EnvironmentOutlined /> {{ event.venue }}<template v-if="event.city"> · {{ event.city }}</template></span>
            <span v-if="event.artist"><UserOutlined /> {{ event.artist }}</span>
            <span style="font-weight: 600; color: var(--ant-color-primary)">{{ resolveEventStageMeta(event).statusText }}</span>
            <span v-if="resolveEventStageMeta(event).timeText">{{ resolveEventStageMeta(event).timeText }}</span>
          </div>

          <a-divider style="margin: 10px 0" />
          <div style="display: flex; justify-content: space-between; align-items: center">
            <div>
              <span style="font-size: 11px; color: var(--ls-text-secondary)">票价区间</span>
              <div style="font-size: 1rem; font-weight: 800">{{ formatEventPriceRange(event) }}</div>
            </div>
            <a-button type="primary" size="small" :disabled="!resolveEventStageMeta(event).canGrab" @click.stop="$emit('selectEvent', event)">
              <template #icon><ThunderboltOutlined /></template>
              {{ resolveEventStageMeta(event).canGrab ? '立即抢票' : '查看阶段' }}
            </a-button>
          </div>
          <div style="margin-top: 10px">
            <a-button
              block
              size="small"
              ghost
              :disabled="resolveEventStageMeta(event).canGrab || resolveEventStageMeta(event).hasStarted"
              @click.stop="subscribeReminder(event)"
            >
              预约开售提醒
            </a-button>
          </div>
        </a-card>
      </a-col>
    </a-row>
  </div>
</template>

<script setup lang="ts">
import { message } from 'ant-design-vue'
import {
  CalendarOutlined,
  EnvironmentOutlined,
  FireOutlined,
  ThunderboltOutlined,
  UserOutlined,
} from '@ant-design/icons-vue'
import { formatEventPriceRange, useEventSquare } from '@/composables/event/useEventSquare'
import { resolveEventStageMeta } from '@/utils/eventStage'
import { request } from '@/composables/infra/useRequest'
import type { LiveEvent } from '@/types'

const emit = defineEmits<{
  selectEvent: [event: LiveEvent]
}>()

const {
  searchQuery,
  activeCategory,
  activeCity,
  activePriceLabel,
  loading,
  hotSearches,
  citiesList,
  categoriesList,
  priceRanges,
  carouselSlides,
  recommendCards,
  filteredEvents,
  handleSearch,
  clickHotWord,
  clickBannerLink,
} = useEventSquare(emit)

async function subscribeReminder(event: LiveEvent) {
  try {
    await request('/api/live-start/distribution/v1/reminder/subscribe', {
      method: 'POST',
      body: JSON.stringify({ eventId: event.id }),
    })
    message.success(`已为《${event.title}》预约开售提醒`)
  } catch (err: any) {
    message.error(err.message || '预约提醒失败')
  }
}

function normalizeEventId(eventId: number | string) {
  return String(eventId)
}
</script>
