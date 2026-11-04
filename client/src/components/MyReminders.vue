<template>
  <div class="my-reminders-view" style="display: flex; flex-direction: column; gap: 24px">
    <a-card class="glass-panel" :bordered="false">
      <template #title>
        <span style="font-size: 1.15rem; font-weight: 800">
          <BellOutlined style="margin-right: 8px; color: var(--ls-color-primary)" /> 我的抢票提醒
        </span>
      </template>
      <template #extra>
        <a-space>
          <a-button type="link" @click="fetchReminders(true)" style="padding: 0; font-weight: 600">
            刷新提醒
          </a-button>
          <a-button type="link" @click="$emit('backToSquare')" style="padding: 0; font-weight: 600">
            返回演出广场
          </a-button>
        </a-space>
      </template>

      <a-alert
        type="info"
        show-icon
        style="margin-bottom: 18px"
        :message="`当前共有 ${pendingCount} 条待提醒记录，系统会在开售前自动触发。`"
      />

      <div v-if="reminders.length" class="reminder-filter-bar">
        <a-segmented v-model:value="reminderFilter" :options="reminderOptions" />
        <span>{{ visibleReminders.length }} 条记录</span>
      </div>

      <a-empty
        v-if="!loading && reminders.length === 0"
        description="你还没有预约任何抢票提醒，去演出广场挑一场吧。"
      />

      <a-empty
        v-else-if="!loading && visibleReminders.length === 0"
        description="该分类暂无提醒"
      />

      <a-list v-else :loading="loading" :data-source="visibleReminders" item-layout="vertical">
        <template #renderItem="{ item }">
          <a-list-item>
            <a-card size="small" :bordered="true" style="border-radius: 14px">
              <div style="display: flex; justify-content: space-between; align-items: flex-start; gap: 16px">
                <div style="display: flex; flex-direction: column; gap: 8px">
                  <div style="display: flex; align-items: center; gap: 8px; flex-wrap: wrap">
                    <span style="font-size: 16px; font-weight: 700">{{ item.eventTitle }}</span>
                    <a-tag :color="item.stageNo === 2 ? 'orange' : 'blue'">
                      {{ item.stageName || (item.stageNo === 2 ? '二开' : '一开') }}
                    </a-tag>
                    <a-tag :color="statusColor(item.status)">
                      {{ item.statusDesc }}
                    </a-tag>
                  </div>
                  <span style="font-size: 13px; color: var(--ls-text-secondary)">
                    开售时间：{{ item.saleStartTime }}
                  </span>
                  <span style="font-size: 13px; color: var(--ls-text-secondary)">
                    提醒时间：{{ item.remindTime }}
                  </span>
                  <span style="font-size: 13px; color: var(--ls-color-primary)">
                    {{ item.reminderMessage }}
                  </span>
                </div>
                <a-button size="small" @click="copyMessage(item.reminderMessage)">
                  复制提醒文案
                </a-button>
              </div>
            </a-card>
          </a-list-item>
        </template>
      </a-list>
    </a-card>
  </div>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { message } from 'ant-design-vue'
import { BellOutlined } from '@ant-design/icons-vue'
import { useReminders } from '@/composables/reminder/useReminders'

defineEmits<{
  backToSquare: []
}>()

const {
  reminders,
  loading,
  pendingCount,
  fetchReminders,
  statusColor,
} = useReminders()

const reminderFilter = ref<'all' | 'pending' | 'done'>('all')
const reminderOptions = [
  { label: '全部', value: 'all' },
  { label: '待开售', value: 'pending' },
  { label: '已完成', value: 'done' },
]
const visibleReminders = computed(() => {
  if (reminderFilter.value === 'all') return reminders.value
  return reminders.value.filter((item) => reminderFilter.value === 'pending' ? item.status === 0 : item.status === 1)
})

async function copyMessage(text: string) {
  try {
    await navigator.clipboard.writeText(text)
    message.success('提醒文案已复制')
  } catch {
    message.warning('复制失败，请手动复制')
  }
}
</script>

<style scoped>
.reminder-filter-bar { display: flex; justify-content: space-between; align-items: center; gap: 12px; margin-bottom: 16px; color: var(--ls-text-secondary); font-size: 12px; }
.reminder-filter-bar .ant-segmented { background: var(--ls-input-bg); }
</style>
