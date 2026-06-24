<template>
  <a-spin :spinning="loading">
    <div v-if="event" class="event-detail">
      <a-card class="glass-panel" :bordered="false">
        <a-button type="link" @click="goBackToSquare" style="padding: 0; margin-bottom: 20px; font-weight: 600">
          返回演出广场
        </a-button>

        <a-row :gutter="[28, 24]">
          <a-col :xs="24" :md="10">
            <img
              :src="event.cover"
              :alt="event.title"
              style="width: 100%; height: 360px; object-fit: cover; border-radius: 16px"
            />
          </a-col>
          <a-col :xs="24" :md="14">
            <a-space direction="vertical" size="middle" style="width: 100%">
              <div>
                <a-space wrap>
                  <a-tag :color="event.type === '演唱会' ? 'magenta' : 'cyan'">{{ event.type }}</a-tag>
                  <a-tag :color="eventStageMeta.stageColor">{{ eventStageMeta.stageLabel }}</a-tag>
                </a-space>
                <h1 style="font-size: 1.8rem; font-weight: 800; margin: 16px 0 10px">{{ event.title }}</h1>
                <p style="font-size: 14px; color: var(--ls-text-secondary); line-height: 1.8">
                  {{ event.artist }} · {{ event.date }} · {{ event.venue }}
                  <template v-if="event.city"> · {{ event.city }}</template>
                </p>
              </div>

              <a-alert
                type="info"
                show-icon
                :message="eventStageMeta.statusText"
                :description="eventStageMeta.timeText || '当前可在详情页查看票档信息，并从这里进入抢票流程。'"
              />

              <a-card size="small" :bordered="false" style="background: rgba(255,255,255,0.03)">
                <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 12px">
                  <span style="font-weight: 700">票档预览</span>
                  <span style="font-size: 12px; color: var(--ls-text-secondary)">最低价 ¥{{ event.minPrice }}</span>
                </div>
                <a-space direction="vertical" style="width: 100%">
                  <div
                    v-for="sku in event.skus"
                    :key="sku.id"
                    style="display: flex; justify-content: space-between; align-items: center; padding: 10px 0; border-bottom: 1px solid rgba(255,255,255,0.06)"
                  >
                    <span>{{ sku.name }}</span>
                    <a-space>
                      <a-tag :color="sku.stock > 0 ? 'success' : 'error'">{{ sku.stock > 0 ? `余票 ${sku.stock}` : '已售罄' }}</a-tag>
                      <span style="font-weight: 700">¥{{ sku.price }}</span>
                    </a-space>
                  </div>
                </a-space>
              </a-card>

              <a-space wrap>
                <a-button type="primary" size="large" :disabled="!eventStageMeta.canGrab" @click="enterCabin">
                  进入票仓
                </a-button>
                <a-button size="large" @click="goBackToSquare">继续逛广场</a-button>
              </a-space>
            </a-space>
          </a-col>
        </a-row>
      </a-card>
    </div>

    <a-result
      v-else-if="!loading"
      status="404"
      title="演出不存在"
      sub-title="没有找到对应的演出详情，可能已经下架或链接有误。"
    >
      <template #extra>
        <a-button type="primary" @click="goBackToSquare">返回演出广场</a-button>
      </template>
    </a-result>
  </a-spin>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import { fetchEventById } from '@/composables/event/useEventCatalog'
import { useEventAccess } from '@/composables/event/useEventAccess'
import { resolveEventStageMeta } from '@/utils/eventStage'
import type { LiveEvent } from '@/types'

const route = useRoute()
const router = useRouter()
const { selectedEvent, setSelectedEvent, allowCabinEntry } = useEventAccess()

const loading = ref(false)
const event = ref<LiveEvent | null>(null)

const eventStageMeta = computed(() => resolveEventStageMeta(event.value))

async function loadEvent() {
  const eventId = Number(route.params.id)
  if (!Number.isFinite(eventId) || eventId <= 0) {
    event.value = null
    return
  }

  if (selectedEvent.value?.id === eventId) {
    event.value = selectedEvent.value
    return
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
