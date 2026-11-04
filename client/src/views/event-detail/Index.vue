<template>
  <a-spin :spinning="loading">
    <div v-if="event" class="event-detail-page">
      <button class="detail-back" @click="goBackToSquare">← 返回演出广场</button>
      <section class="detail-hero">
        <div class="detail-cover"><img :src="event.cover" :alt="event.title" /></div>
        <div class="detail-summary">
          <div class="detail-tags"><a-tag color="orange">{{ event.type }}</a-tag><a-tag :color="eventStageMeta.stageColor">{{ eventStageMeta.stageLabel }}</a-tag></div>
          <h1>{{ event.title }}</h1>
          <p v-if="event.artist" class="detail-artist">{{ event.artist }}</p>
          <div class="detail-meta-list"><span>时间　{{ event.date }}</span><span>场馆　{{ event.venue }}<template v-if="event.city"> · {{ event.city }}</template></span></div>
          <a-alert type="info" show-icon :message="eventStageMeta.statusText" :description="eventStageMeta.timeText || '查看票档，选择合适的座位和观演人。'" />
          <div class="detail-action-bar">
            <div><small>起售价</small><strong>{{ formatEventPriceRange(event) }}</strong></div>
            <a-button type="primary" size="large" :disabled="!eventStageMeta.canGrab" @click="enterCabin">{{ eventStageMeta.canGrab ? '立即购票' : eventStageMeta.hasStarted ? '演出已开演' : '暂未开售' }}</a-button>
          </div>
        </div>
      </section>

      <section class="detail-content-grid">
        <a-card class="detail-panel" :bordered="false">
          <template #title>票档信息</template>
          <div class="sku-list">
            <div v-for="sku in event.skus" :key="sku.id" class="sku-row">
              <div><strong>{{ sku.name }}</strong><span>{{ sku.stock > 0 ? `余票 ${sku.stock} 张` : '已售罄' }}</span></div>
              <div><b>¥{{ sku.price }}</b><a-tag :color="sku.stock > 0 ? 'success' : 'error'">{{ sku.stock > 0 ? '可购买' : '售罄' }}</a-tag></div>
            </div>
          </div>
        </a-card>
        <a-card class="detail-panel" :bordered="false">
          <template #title>购票须知</template>
          <ul class="detail-notice"><li>实名制购票，观演人信息需与入场证件一致。</li><li>每个账号每个票档限购 6 张。</li><li>订单支付、退票规则以当前场次页面提示为准。</li></ul>
        </a-card>
      </section>
    </div>

    <a-result v-else-if="!loading" status="404" title="演出不存在" sub-title="没有找到对应的演出详情，可能已经下架或链接有误。">
      <template #extra><a-button type="primary" @click="goBackToSquare">返回演出广场</a-button></template>
    </a-result>
  </a-spin>
</template>



<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import { fetchEventById } from '@/composables/event/useEventCatalog'
import { useEventAccess } from '@/composables/event/useEventAccess'
import { formatEventPriceRange } from '@/composables/event/useEventSquare'
import { requireAuth } from '@/composables/useAuth'
import { resolveEventStageMeta } from '@/utils/eventStage'
import type { LiveEvent } from '@/types'

const route = useRoute()
const router = useRouter()
const { selectedEvent, setSelectedEvent, allowCabinEntry } = useEventAccess()

const loading = ref(false)
const event = ref<LiveEvent | null>(null)

const eventStageMeta = computed(() => resolveEventStageMeta(event.value))

function normalizeRouteEventId(id: unknown) {
  if (id == null) return null
  const value = String(id).trim()
  return /^\d+$/.test(value) ? value : null
}

async function loadEvent() {
  const eventId = normalizeRouteEventId(route.params.id)
  if (!eventId) {
    event.value = null
    return
  }

  if (String(selectedEvent.value?.id) === eventId) {
    event.value = selectedEvent.value
  }

  loading.value = true
  try {
    const detail = await fetchEventById(eventId)
    event.value = detail
    setSelectedEvent(detail)
  } catch (err: any) {
    event.value = null
    message.error(err.message || '加载演出详情失败')
  } finally {
    loading.value = false
  }
}

function goBackToSquare() {
  void router.push({ name: 'Square' })
}

function enterCabin() {
  if (!event.value) return
  if (!requireAuth()) {
    return
  }
  allowCabinEntry(event.value)
  void router.push({ name: 'OrderCabin', params: { id: event.value.id } })
}

watch(
  () => route.params.id,
  () => {
    void loadEvent()
  },
  { immediate: true }
)
</script>
