<template>
  <div class="order-cabin">
    <a-card v-if="selectedEvent" class="glass-panel" :bordered="false" style="margin-bottom: 28px">
      <a-button type="link" @click="$emit('backToSquare')" style="padding: 0; margin-bottom: 20px; font-weight: 600">
        <template #icon><ArrowLeftOutlined /></template>
        返回演出广场
      </a-button>

      <a-row :gutter="[36, 24]">
        <a-col :xs="24" :md="8">
          <div style="border-radius: 14px; overflow: hidden; border: 1px solid rgba(var(--ls-accent-rgb), 0.15); box-shadow: var(--ls-neon-glow); margin-bottom: 16px">
            <img :src="selectedEvent.cover" :alt="selectedEvent.title" style="width: 100%; height: 280px; object-fit: cover" />
          </div>
          <h3 style="font-size: 1.1rem; font-weight: 700; line-height: 1.4; margin-bottom: 10px">{{ selectedEvent.title }}</h3>
          <div style="font-size: 13px; color: var(--ls-text-secondary); display: flex; flex-direction: column; gap: 5px">
            <span><CalendarOutlined /> {{ selectedEvent.date }}</span>
            <span><EnvironmentOutlined /> {{ selectedEvent.venue }}</span>
          </div>
          <div style="margin-top: 12px; display: flex; flex-direction: column; gap: 8px">
            <a-tag :color="eventStageMeta.stageColor" style="width: fit-content; border-radius: 999px">
              {{ eventStageMeta.stageLabel }}
            </a-tag>
            <a-tag :color="eventStageMeta.canGrab ? 'success' : (eventStageMeta.hasStarted ? 'default' : 'gold')" style="width: fit-content; border-radius: 999px">
              {{ eventStageMeta.statusText }}
            </a-tag>
            <span v-if="eventStageMeta.timeText" style="font-size: 12px; color: var(--ls-text-secondary)">
              {{ eventStageMeta.timeText }}
            </span>
            <a-button
              v-if="!eventStageMeta.canGrab && !eventStageMeta.hasStarted"
              size="small"
              ghost
              style="width: fit-content; margin-top: 4px"
              :disabled="isReminderDisabled"
              @click="handleReminderClick"
            >
              {{ reminderButtonText }}
            </a-button>
          </div>
        </a-col>

        <a-col :xs="24" :md="16">
          <h4 style="font-size: 0.95rem; font-weight: 600; margin-bottom: 12px">
            <TagOutlined style="margin-right: 6px" /> 选择票档规格
          </h4>
          <a-radio-group v-model:value="activeSkuId" style="width: 100%; margin-bottom: 20px">
            <a-space direction="vertical" style="width: 100%">
              <a-radio-button
                v-for="sku in selectedEvent.skus"
                :key="sku.id"
                :value="sku.id"
                :disabled="sku.stock <= 0"
                style="width: 100%; height: auto; padding: 12px 16px; border-radius: 10px; display: flex; justify-content: space-between; align-items: center"
              >
                <div style="display: flex; justify-content: space-between; width: 100%; align-items: center">
                  <span style="font-weight: 600">{{ sku.name }}</span>
                  <a-space>
                    <a-tag v-if="sku.stock <= 0" color="error">该规格已售罄</a-tag>
                    <a-tag v-else color="success">余 {{ sku.stock }} 张</a-tag>
                    <span style="font-weight: 800; font-size: 1.05rem">¥{{ sku.price }}</span>
                  </a-space>
                </div>
              </a-radio-button>
            </a-space>
          </a-radio-group>

          <h4 style="font-size: 0.95rem; font-weight: 600; margin-bottom: 10px; display: flex; justify-content: space-between; align-items: center">
            <span>
              <TeamOutlined style="margin-right: 6px" /> 勾选实名观演人
              <span style="font-size: 11px; color: var(--ls-text-secondary); font-weight: normal; margin-left: 6px">(观演人数需与购票张数一致)</span>
            </span>
            <a-button type="link" size="small" style="padding: 0" @click="showVisitorManager = true">
              <template #icon><SettingOutlined /></template>
              管理观演人
            </a-button>
          </h4>
          <a-row :gutter="[12, 12]" style="margin-bottom: 20px">
            <a-col :xs="24" :sm="8" v-for="v in visitorList" :key="v.id">
              <a-card
                size="small"
                hoverable
                :class="{ 'glow-card': true }"
                :style="{ borderColor: v.checked ? 'var(--ls-color-primary)' : undefined }"
                @click="toggleVisitor(v)"
              >
                <div style="display: flex; justify-content: space-between; align-items: center">
                  <span style="font-weight: 600; font-size: 13px">{{ v.name }}</span>
                  <a-checkbox :checked="v.checked" />
                </div>
                <div style="font-size: 11px; color: var(--ls-text-secondary); font-family: monospace; margin-top: 4px">
                  {{ v.idCard }}
                </div>
              </a-card>
            </a-col>
          </a-row>

          <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 24px">
            <span style="font-weight: 600">购买数量：</span>
            <a-space>
              <a-input-number v-model:value="ticketCount" :min="1" :max="6" size="large" @change="onCountChange" />
              <span style="font-size: 12px; color: var(--ls-text-secondary)">(每人限购 6 张)</span>
            </a-space>
          </div>

          <a-card :bordered="false" style="background: rgba(0,0,0,0.08); border: 1px dashed rgba(var(--ls-accent-rgb), 0.2); border-radius: 14px">
            <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; padding-bottom: 12px; border-bottom: 1px dashed rgba(var(--ls-accent-rgb), 0.15)">
              <span style="font-weight: 600; font-size: 13px; display: flex; align-items: center; gap: 6px">
                <ThunderboltOutlined style="color: var(--ls-color-primary)" />
                开启高并发压测演示模式
              </span>
              <a-switch v-model:checked="showStressPanel" :disabled="grabStatus !== 'idle' || stressRunning" />
            </div>

            <div v-if="!showStressPanel">
              <div v-if="grabStatus === 'idle'">
                <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 14px">
                  <span style="font-size: 13px; color: var(--ls-text-secondary)">合计：</span>
                  <span style="font-size: 1.5rem; font-weight: 800">¥{{ totalPrice }}</span>
                </div>
                <div style="margin-bottom: 12px; font-size: 12px; color: var(--ls-text-secondary)">
                  当前阶段：<span style="font-weight: 700; color: var(--ls-color-primary)">{{ eventStageMeta.statusText }}</span>
                </div>
                <a-button type="primary" block size="large" :disabled="!activeSku || activeSku.stock <= 0 || !eventStageMeta.canGrab" @click="triggerGrab">
                  <template #icon><ThunderboltOutlined /></template>
                  {{
                    activeSku && activeSku.stock <= 0
                      ? '该规格已售罄'
                      : !eventStageMeta.canGrab
                        ? `当前为${eventStageMeta.statusText}`
                        : '立即开始抢票'
                  }}
                </a-button>
              </div>

              <div v-else-if="grabStatus === 'fetching_token'" style="display: flex; gap: 20px; align-items: center; padding: 8px 0">
                <div class="fingerprint-scanner" style="color: var(--ls-color-primary)">
                  <ScanOutlined style="font-size: 28px" />
                </div>
                <div>
                  <h5 style="font-weight: 700; margin-bottom: 2px">安全校验中</h5>
                  <p style="font-size: 12px; color: var(--ls-text-secondary); margin: 0">正在获取一次性下单 Token，请稍候。</p>
                </div>
              </div>

              <div v-else-if="grabStatus === 'grabbing'" style="text-align: center; padding: 8px 0">
                <a-progress :percent="grabProgress" :stroke-color="{ from: 'var(--ls-color-primary)', to: 'var(--ls-color-success)' }" :show-info="false" style="margin-bottom: 12px" />
                <div style="display: flex; justify-content: space-between; margin-bottom: 6px">
                  <a-tag color="success"><SafetyCertificateOutlined /> Token 校验通过</a-tag>
                  <span style="font-size: 11px; color: var(--ls-text-secondary); font-family: monospace">{{ pathToken.substring(0, 18) }}...</span>
                </div>
                <h5 style="font-weight: 700; margin-bottom: 3px">
                  <LoadingOutlined spin /> 正在提交抢票请求 ({{ grabProgress }}%)
                </h5>
                <p style="font-size: 12px; color: var(--ls-text-secondary); margin: 0">请求已经进入异步下单链路，正在处理库存与订单写入。</p>
              </div>

              <a-result v-else-if="grabStatus === 'success'" status="success" title="抢票成功" :sub-title="'订单号：' + createdOrderNo">
                <template #extra>
                  <a-space>
                    <a-button @click="resetGrab">再抢一单</a-button>
                    <a-button type="primary" @click="$emit('goToOrders')">查看订单</a-button>
                  </a-space>
                </template>
              </a-result>

              <a-result v-else-if="grabStatus === 'failed'" status="error" title="抢票失败" :sub-title="grabError">
                <template #extra>
                  <a-button type="primary" @click="resetGrab">重新选择规格</a-button>
                </template>
              </a-result>
            </div>

            <div v-else>
              <div style="margin-bottom: 16px">
                <a-row :gutter="16" align="middle">
                  <a-col :span="12">
                    <div style="font-size: 12px; margin-bottom: 4px; color: var(--ls-text-secondary)">
                      并发用户数：<b style="color: var(--ls-color-primary)">{{ stressConcurrency }}</b>
                    </div>
                    <a-slider v-model:value="stressConcurrency" :min="10" :max="500" :step="10" :disabled="stressRunning" />
                  </a-col>
                  <a-col :span="12">
                    <div style="font-size: 12px; margin-bottom: 4px; color: var(--ls-text-secondary)">
                      请求间隔：<b style="color: var(--ls-color-primary)">{{ stressInterval }} ms</b>
                    </div>
                    <a-slider v-model:value="stressInterval" :min="0" :max="500" :step="5" :disabled="stressRunning" />
                  </a-col>
                </a-row>

                <div style="margin-top: 12px; display: flex; gap: 10px">
                  <a-button type="primary" :danger="stressRunning" block :loading="stressRunning" @click="stressRunning ? stopStressTest() : runStressTest()">
                    <template #icon><ThunderboltOutlined /></template>
                    {{ stressRunning ? '压测中，点击停止' : '启动并发压测' }}
                  </a-button>
                </div>
              </div>

              <div v-if="stressTested" style="margin-top: 16px; border-top: 1px dashed rgba(255,255,255,0.06); padding-top: 16px">
                <a-row :gutter="16" style="margin-bottom: 16px">
                  <a-col :xs="24" :md="10" style="display: flex; flex-direction: column; align-items: center; justify-content: center">
                    <div style="position: relative; width: 110px; height: 110px">
                      <svg viewBox="0 0 100 100" style="width: 100%; height: 100%; transform: rotate(-90deg)">
                        <circle cx="50" cy="50" r="40" stroke="rgba(255,255,255,0.06)" stroke-width="8" fill="none" />
                        <circle
                          cx="50"
                          cy="50"
                          r="40"
                          stroke="var(--ls-color-success)"
                          stroke-width="8"
                          fill="none"
                          :stroke-dasharray="2 * Math.PI * 40"
                          :stroke-dashoffset="2 * Math.PI * 40 * (1 - successRate / 100)"
                          stroke-linecap="round"
                          style="transition: stroke-dashoffset 0.15s ease, stroke 0.3s"
                        />
                      </svg>
                      <div style="position: absolute; top: 0; left: 0; width: 100%; height: 100%; display: flex; flex-direction: column; align-items: center; justify-content: center">
                        <span style="font-size: 10px; color: var(--ls-text-secondary)">抢票成功率</span>
                        <span style="font-size: 1.25rem; font-weight: 800; font-family: 'Outfit'; color: var(--ls-color-success)">{{ successRate }}%</span>
                      </div>
                    </div>
                    <div style="font-size: 11px; color: var(--ls-text-secondary); margin-top: 8px; text-align: center">
                      已发请求: <b style="color: var(--ls-color-primary)">{{ stressResults.total }}</b> / {{ stressConcurrency }}
                    </div>
                  </a-col>

                  <a-col :xs="24" :md="14" style="display: flex; flex-direction: column; justify-content: center">
                    <div style="display: flex; flex-direction: column; gap: 6px">
                      <div class="stress-metric-card" style="border-left-color: var(--ls-color-success)">
                        <span style="font-size: 11px; color: var(--ls-text-secondary)">成功请求：</span>
                        <span style="font-weight: 700; color: var(--ls-color-success); font-family: monospace">{{ stressResults.success }}</span>
                      </div>
                      <div class="stress-metric-card" style="border-left-color: var(--ls-color-warning)">
                        <span style="font-size: 11px; color: var(--ls-text-secondary)">限流拦截：</span>
                        <span style="font-weight: 700; color: var(--ls-color-warning); font-family: monospace">{{ stressResults.rateLimit }}</span>
                      </div>
                      <div class="stress-metric-card" style="border-left-color: var(--ls-color-error)">
                        <span style="font-size: 11px; color: var(--ls-text-secondary)">WAF 拦截：</span>
                        <span style="font-weight: 700; color: var(--ls-color-error); font-family: monospace">{{ stressResults.wafBlock }}</span>
                      </div>
                      <div class="stress-metric-card" style="border-left-color: rgba(255,255,255,0.3)">
                        <span style="font-size: 11px; color: var(--ls-text-secondary)">售罄拦截：</span>
                        <span style="font-weight: 700; color: rgba(255,255,255,0.7); font-family: monospace">{{ stressResults.soldOut }}</span>
                      </div>
                    </div>
                  </a-col>
                </a-row>

                <div style="margin-top: 12px">
                  <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 6px">
                    <span style="font-size: 12px; font-weight: 600; color: var(--ls-text-secondary)">实时日志</span>
                    <a-button type="link" size="small" style="padding: 0; font-size: 11px" @click="stressLogs = []">清空</a-button>
                  </div>
                  <div
                    ref="stressLogContainer"
                    style="height: 180px; overflow-y: auto; background: #08090d; border: 1px solid rgba(255,255,255,0.06); border-radius: 8px; padding: 10px; font-family: monospace; font-size: 11px; color: #00ffcc; line-height: 1.5; text-align: left;"
                  >
                    <div v-if="stressLogs.length === 0" style="color: rgba(255,255,255,0.25); text-align: center; margin-top: 70px">
                      等待压测启动...
                    </div>
                    <div v-for="(log, idx) in stressLogs" :key="idx" style="white-space: pre-wrap">
                      {{ log }}
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </a-card>
        </a-col>
      </a-row>
    </a-card>

    <VisitorManagerModal v-model:open="showVisitorManager" @change="fetchVisitors" />
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { message } from 'ant-design-vue'
import {
  ArrowLeftOutlined,
  CalendarOutlined,
  EnvironmentOutlined,
  LoadingOutlined,
  SafetyCertificateOutlined,
  ScanOutlined,
  SettingOutlined,
  TagOutlined,
  TeamOutlined,
  ThunderboltOutlined,
} from '@ant-design/icons-vue'
import type { LiveEvent } from '@/types'
import VisitorManagerModal from './VisitorManagerModal.vue'
import { useTicketOrderCabin } from '@/composables/order/useTicketOrderCabin'
import { useReminderRegistry } from '@/composables/reminder/useReminderRegistry'

const props = defineProps<{
  selectedEvent: LiveEvent | null
}>()

const emit = defineEmits<{
  backToSquare: []
  goToOrders: []
}>()

const {
  activeSkuId,
  ticketCount,
  activeSku,
  totalPrice,
  eventStageMeta,
  visitorList,
  showVisitorManager,
  fetchVisitors,
  toggleVisitor,
  onCountChange,
  grabStatus,
  grabProgress,
  pathToken,
  createdOrderNo,
  grabError,
  triggerGrab,
  resetGrab,
  showStressPanel,
  stressConcurrency,
  stressInterval,
  stressRunning,
  stressTested,
  stressLogContainer,
  stressResults,
  stressLogs,
  successRate,
  stopStressTest,
  runStressTest,
} = useTicketOrderCabin(props, emit)

const { fetchReminders, subscribeReminder, getReminderByEventId } = useReminderRegistry()
void fetchReminders()

const currentReminder = computed(() => {
  if (!props.selectedEvent) return undefined
  return getReminderByEventId(props.selectedEvent.id)
})

const reminderButtonText = computed(() => {
  if (currentReminder.value?.status === 0) return '已预约提醒'
  if (currentReminder.value?.status === 1) return '已完成提醒'
  return '预约开售提醒'
})

const isReminderDisabled = computed(() => currentReminder.value?.status === 0 || currentReminder.value?.status === 1)

async function handleReminderClick() {
  if (!props.selectedEvent || isReminderDisabled.value) return
  try {
    await subscribeReminder(props.selectedEvent.id)
    message.success(`已为《${props.selectedEvent.title}》预约开售提醒`)
  } catch (err: any) {
    message.error(err.message || '预约提醒失败')
  }
}
</script>
