import { computed, onMounted } from 'vue'
import { message } from 'ant-design-vue'
import { useReminderRegistry } from './useReminderRegistry'

export function useReminders() {
  const { reminders, loading, fetchReminders } = useReminderRegistry()

  async function safeFetchReminders(force = false) {
    try {
      await fetchReminders(force)
    } catch (err: any) {
      message.error(err.message || '获取提醒列表失败')
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

  const pendingCount = computed(() => reminders.value.filter((item) => item.status === 0).length)

  onMounted(() => {
    void safeFetchReminders()
  })

  return {
    reminders,
    loading,
    pendingCount,
    fetchReminders: safeFetchReminders,
    statusColor,
  }
}
