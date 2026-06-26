import { computed, ref } from 'vue'
import { settlementApi } from '@/api/settlement'
import type { SettlementNotificationItem } from '@/types'

const loading = ref(false)
const notifications = ref<SettlementNotificationItem[]>([])
const loaded = ref(false)

export function useSettlementNotifications() {
  const actionableCount = computed(() => notifications.value.filter((item) => !item.read).length)

  async function fetchNotifications(force = false) {
    if (loading.value) return
    if (loaded.value && !force) return

    loading.value = true
    try {
      notifications.value = await settlementApi.notifications()
      loaded.value = true
    } finally {
      loading.value = false
    }
  }

  async function markAsRead(notificationKey: string) {
    await settlementApi.markNotificationRead(notificationKey)
    const target = notifications.value.find((item) => item.notificationKey === notificationKey)
    if (target) {
      target.read = true
    }
  }

  return {
    loading,
    loaded,
    notifications,
    actionableCount,
    fetchNotifications,
    markAsRead,
  }
}
