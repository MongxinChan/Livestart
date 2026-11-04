<template>
  <div class="square-page">
    <section class="square-hero">
      <div class="square-hero-copy">
        <div class="square-eyebrow">LIVESTART / LIVE EVENTS</div>
        <h1>把下一场现场，<span>放进日程。</span></h1>
        <p>发现城市里的演出，提前锁定喜欢的艺人和舞台。</p>
        <a-input-search
          v-model:value="searchQuery"
          class="square-hero-search"
          size="large"
          placeholder="搜索演出、艺人、场馆"
          enter-button="搜索"
          @search="handleSearch"
        />
        <div v-if="hotSearches.length" class="square-hotwords">
          <span>大家都在搜</span>
          <button v-for="hot in hotSearches.slice(0, 5)" :key="hot.keyword" @click="clickHotWord(hot.keyword)">
            {{ hot.keyword }}
          </button>
        </div>
      </div>
      <div class="square-hero-media" v-if="carouselSlides.length">
        <a-carousel autoplay :dots="false" :autoplay-speed="5000" effect="fade">
          <div v-for="slide in carouselSlides" :key="slide.eventId" class="square-hero-slide">
            <img :src="slide.image" :alt="slide.title" />
            <div class="square-hero-slide-copy">
              <a-tag color="orange">{{ slide.tag }}</a-tag>
              <strong>{{ slide.title }}</strong>
              <span>{{ slide.desc }}</span>
              <a-button type="primary" size="small" @click="clickBannerLink(normalizeEventId(slide.eventId))">
                查看演出
              </a-button>
            </div>
          </div>
        </a-carousel>
      </div>
    </section>

    <section v-if="!searchQuery && recommendCards.length" class="featured-section">
      <div class="section-heading">
        <div>
          <span class="section-kicker">EDITOR'S PICKS</span>
          <h2>本周值得去现场</h2>
        </div>
        <span>精选 {{ recommendCards.length }} 场</span>
      </div>
      <div class="featured-grid">
        <button
          v-for="rec in recommendCards"
          :key="rec.eventId"
          class="featured-card"
          @click="clickBannerLink(normalizeEventId(rec.eventId))"
        >
          <img :src="rec.cover" :alt="rec.title" />
          <span class="featured-card-overlay" />
          <div class="featured-card-copy">
            <a-tag :color="rec.tagColor">{{ rec.tag }}</a-tag>
            <strong>{{ rec.title }}</strong>
            <span>{{ rec.venue }}</span>
            <div><b>¥ {{ rec.priceRange }}</b><em>{{ rec.status }}</em></div>
          </div>
        </button>
      </div>
    </section>

    <section class="catalog-section">
      <div class="section-heading catalog-heading">
        <div>
          <span class="section-kicker">ALL EVENTS</span>
          <h2>{{ searchQuery ? `搜索“${searchQuery}”` : '全部演出' }}</h2>
        </div>
        <span v-if="!loading">{{ totalEvents }} 场演出</span>
      </div>

      <div class="filters-panel catalog-filters">
        <div class="filter-row">
          <span class="filter-label">城市</span>
          <div class="filter-options">
            <button v-for="city in citiesList" :key="city" :class="['filter-tag', { active: activeCity === city }]" @click="activeCity = city">
              {{ city }}
            </button>
          </div>
        </div>
        <div class="filter-row">
          <span class="filter-label">类型</span>
          <div class="filter-options">
            <button v-for="category in categoriesList" :key="category" :class="['filter-tag', { active: activeCategory === category }]" @click="activeCategory = category">
              {{ category }}
            </button>
          </div>
        </div>
        <div class="filter-row">
          <span class="filter-label">价格</span>
          <div class="filter-options">
            <button v-for="price in priceRanges" :key="price.label" :class="['filter-tag', { active: activePriceLabel === price.label }]" @click="activePriceLabel = price.label">
              {{ price.label }}
            </button>
          </div>
        </div>
      </div>

      <a-row v-if="loading" :gutter="[20, 20]">
        <a-col v-for="i in 8" :key="i" :xs="24" :sm="12" :lg="8" :xl="6">
          <a-card :bordered="false" class="event-card event-card-skeleton"><a-skeleton active :paragraph="{ rows: 4 }" /></a-card>
        </a-col>
      </a-row>

      <a-row v-else-if="pagedEvents.length" :gutter="[20, 20]">
        <a-col v-for="event in pagedEvents" :key="event.id" :xs="24" :sm="12" :lg="8" :xl="6">
          <a-card :bordered="false" class="event-card" :body-style="{ padding: 0 }" @click="handleSelectEvent(event)">
            <div class="event-card-cover">
              <img :src="event.cover" :alt="event.title" />
              <a-tag class="event-card-type" color="orange">{{ event.type }}</a-tag>
              <a-tag class="event-card-stage" :color="resolveEventStageMeta(event).stageColor">{{ resolveEventStageMeta(event).stageLabel }}</a-tag>
              <button class="event-card-favorite" aria-label="预约提醒" :disabled="isReminderButtonDisabled(event)" @click.stop="handleReminderClick(event)">
                {{ getReminderButtonText(event) === '预约开售提醒' ? '＋' : '✓' }}
              </button>
            </div>
            <div class="event-card-body">
              <h3>{{ event.title }}</h3>
              <p v-if="event.artist">{{ event.artist }}</p>
              <div class="event-card-meta"><span>{{ event.date }}</span><span>{{ event.venue }}<template v-if="event.city"> · {{ event.city }}</template></span></div>
              <div class="event-card-footer"><strong>{{ formatEventPriceRange(event) }}</strong><a-button type="primary" size="small" @click.stop="handleSelectEvent(event)">{{ resolveEventStageMeta(event).canGrab ? '立即购票' : '查看详情' }}</a-button></div>
            </div>
          </a-card>
        </a-col>
      </a-row>

      <a-empty v-else description="暂时没有匹配的演出" class="catalog-empty" />

      <a-pagination
        v-if="!loading && totalEvents > pageSize"
        class="catalog-pagination"
        :current="page"
        :page-size="pageSize"
        :total="totalEvents"
        :show-size-changer="false"
        @change="changePage"
      />
    </section>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { message } from 'ant-design-vue'
import { formatEventPriceRange, useEventSquare } from '@/composables/event/useEventSquare'
import { useReminderRegistry } from '@/composables/reminder/useReminderRegistry'
import { resolveEventStageMeta } from '@/utils/eventStage'
import { request } from '@/composables/infra/useRequest'
import type { LiveEvent } from '@/types'

interface NextSaleStage {
  eventId: string
  id: string
  stageName: string
  saleStartTime: string
}

const emit = defineEmits<{
  selectEvent: [event: LiveEvent]
}>()

const {
  searchQuery,
  activeCategory,
  activeCity,
  activePriceLabel,
  page,
  pageSize,
  loading,
  totalEvents,
  hotSearches,
  citiesList,
  categoriesList,
  priceRanges,
  carouselSlides,
  recommendCards,
  pagedEvents,
  handleSearch,
  changePage,
  ensureAuthenticatedAction,
  clickHotWord,
  clickBannerLink,
} = useEventSquare(emit)

const { fetchReminders, subscribeReminder, getReminderByEventId } = useReminderRegistry()
const reminderLoadingId = ref<string | null>(null)
void fetchReminders().catch((err) => {
  console.warn('拉取提醒列表失败', err)
})

function getReminderButtonText(event: LiveEvent) {
  const stageMeta = resolveEventStageMeta(event)
  const reminder = event.stageId ? getReminderByEventId(event.distributionEventId || event.id, event.stageId) : null
  if (reminder?.status === 0) return '已预约提醒'
  if (reminder?.status === 1) return '已完成提醒'
  if (stageMeta.hasStarted) return '演出已开演'
  return '预约开售提醒'
}

function isReminderButtonDisabled(event: LiveEvent) {
  const stageMeta = resolveEventStageMeta(event)
  const reminder = event.stageId ? getReminderByEventId(event.distributionEventId || event.id, event.stageId) : null
  return stageMeta.hasStarted
    || reminder?.status === 0
    || reminder?.status === 1
}

async function handleReminderClick(event: LiveEvent) {
  if (isReminderButtonDisabled(event) || reminderLoadingId.value === String(event.id)) return
  if (!ensureAuthenticatedAction()) {
    return
  }
  reminderLoadingId.value = String(event.id)
  try {
    const stage = await request<NextSaleStage | null>(
      `/api/live-start/distribution/v1/event/${event.id}/next-sale-stage`
    )
    if (!stage) {
      message.warning('该演出暂无可预约的开售阶段')
      return
    }
    event.distributionEventId = stage.eventId
    event.stageId = stage.id
    event.stageName = stage.stageName
    event.saleStartTime = stage.saleStartTime
    if (getReminderByEventId(stage.eventId, stage.id)?.status === 0) {
      message.info('已预约该阶段的开售提醒')
      return
    }
    await subscribeReminder(stage.eventId, stage.id)
    message.success(`已为《${event.title}》预约开售提醒`)
  } catch (err: any) {
    message.error(err.message || '预约提醒失败')
  } finally {
    reminderLoadingId.value = null
  }
}

function handleSelectEvent(event: LiveEvent) {
  emit('selectEvent', event)
}

function normalizeEventId(eventId: number | string) {
  return String(eventId)
}
</script>
