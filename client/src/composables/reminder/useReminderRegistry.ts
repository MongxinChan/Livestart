import { computed, ref } from 'vue'
import { request } from '@/composables/infra/useRequest'
import type { TicketReminder } from '@/types'

const reminders = ref<TicketReminder[]>([])
const loading = ref(false)
const loaded = ref(false)

export function useReminderRegistry() {
  const reminderMap = computed(() => {
    const map = new Map<string, TicketReminder>()
    reminders.value.forEach((item) => {
      map.set(String(item.eventId), item)
    })
    return map
  })

  async function fetchReminders(force = false) {
    if (loading.value) return
    if (loaded.value && !force) return
    loading.value = true
    try {
      reminders.value = await request<TicketReminder[]>('/api/live-start/distribution/v1/reminder/list')
      loaded.value = true
    } finally {
      loading.value = false
    }
  }

  async function subscribeReminder(eventId: number | string) {
    const reminderId = await request<number>('/api/live-start/distribution/v1/reminder/subscribe', {
      method: 'POST',
      body: JSON.stringify({ eventId }),
    })
    await fetchReminders(true)
    return reminderId
  }

  function getReminderByEventId(eventId: number | string) {
    return reminderMap.value.get(String(eventId))
  }

  return {
    reminders,
    loading,
    loaded,
    reminderMap,
    fetchReminders,
    subscribeReminder,
    getReminderByEventId,
  }
}
