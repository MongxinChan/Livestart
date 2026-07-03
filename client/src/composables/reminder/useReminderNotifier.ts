import { computed, onBeforeUnmount, onMounted, watch } from 'vue'
import { notification } from 'ant-design-vue'
import router from '@/router'
import { apiState } from '@/composables/infra/useRequest'
import { useReminderRegistry } from './useReminderRegistry'
import type { TicketReminder } from '@/types'

const DELIVERED_STATUS = 1
const POLL_INTERVAL_MS = 30000

let timer: number | null = null
const notifiedKeys = new Set<string>()
let previousStatusMap = new Map<string, number>()

function buildReminderKey(item: TicketReminder) {
  return `${String(item.id)}::${String(item.eventId)}::${item.stageId == null ? 'default' : String(item.stageId)}`
}

function openReminderNotification(item: TicketReminder) {
  const key = buildReminderKey(item)
  if (notifiedKeys.has(key)) {
    return
  }
  notifiedKeys.add(key)

  notification.open({
    key,
    message: '抢票提醒已送达',
    description: `${item.eventTitle} ${item.stageName || ''} 已到提醒时间。${item.reminderMessage}`,
    duration: 8,
    btn: undefined,
    onClick: () => {
      if (item.eventId != null) {
        void router.push({ name: 'EventDetail', params: { id: String(item.eventId) } })
      } else {
        void router.push({ name: 'Reminders' })
      }
    },
  })
}

export function useReminderNotifier() {
  const { reminders, fetchReminders } = useReminderRegistry()
  const isAuthenticated = computed(() => Boolean(apiState.token))

  async function pollReminders(force = false) {
    if (!isAuthenticated.value) {
      return
    }
    try {
      await fetchReminders(force)
    } catch (err) {
      console.warn('[ReminderNotifier] 拉取提醒列表失败', err)
    }
  }

  function startPolling() {
    if (timer != null || !isAuthenticated.value) {
      return
    }
    void pollReminders(true)
    timer = window.setInterval(() => {
      void pollReminders(true)
    }, POLL_INTERVAL_MS)
  }

  function stopPolling() {
    if (timer != null) {
      window.clearInterval(timer)
      timer = null
    }
  }

  watch(
    reminders,
    (items) => {
      const nextStatusMap = new Map<string, number>()
      items.forEach((item) => {
        const key = buildReminderKey(item)
        nextStatusMap.set(key, item.status)
        const previousStatus = previousStatusMap.get(key)
        if (item.status === DELIVERED_STATUS && previousStatus != null && previousStatus !== DELIVERED_STATUS) {
          openReminderNotification(item)
        }
      })
      previousStatusMap = nextStatusMap
    },
    { deep: true },
  )

  watch(
    isAuthenticated,
    (loggedIn) => {
      if (loggedIn) {
        startPolling()
      } else {
        stopPolling()
        previousStatusMap = new Map()
        notifiedKeys.clear()
      }
    },
    { immediate: true },
  )

  onMounted(() => {
    if (isAuthenticated.value) {
      startPolling()
    }
  })

  onBeforeUnmount(() => {
    stopPolling()
  })
}
