import { computed, onMounted, ref } from 'vue'
import { message } from 'ant-design-vue'
import { request } from '@/composables/useRequest'
import type { TicketReminder } from '@/types'

export function useReminders() {
  const reminders = ref<TicketReminder[]>([])
  const loading = ref(false)

  async function fetchReminders() {
    loading.value = true
    try {
      reminders.value = await request<TicketReminder[]>('/api/live-start/distribution/v1/reminder/list')
    } catch (err: any) {
      message.error(err.message || '获取提醒列表失败')
    } finally {
      loading.value = false
    }
  }

  function statusColor(status: number) {
    const map: Record<number, string> = {
      0: 'processing',
      1: 'success',
      2: 'default',
    }
    return map[status] || 'default'
  }

  const pendingCount = computed(() => reminders.value.filter(item => item.status === 0).length)

  onMounted(() => {
    fetchReminders()
  })

  return {
    reminders,
    loading,
    pendingCount,
    fetchReminders,
    statusColor,
  }
}
