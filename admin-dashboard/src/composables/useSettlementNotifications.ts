import { computed, ref } from 'vue'
import dayjs from 'dayjs'
import { settlementApi } from '@/api/settlement'
import type { SettlementItem } from '@/types'

const RECENT_UPDATE_HOURS = 72

export interface SettlementNotificationItem {
  id: string
  eventId: number
  eventTitle: string
  performerName?: string
  type: 'pending' | 'updated'
  typeLabel: string
  description: string
  updateTime: string
  amount: number
  tickets: number
}

const loading = ref(false)
const notifications = ref<SettlementNotificationItem[]>([])
const loaded = ref(false)

export function useSettlementNotifications() {
  const actionableCount = computed(() => notifications.value.length)

  async function fetchNotifications(force = false) {
    if (loading.value) return
    if (loaded.value && !force) return

    loading.value = true
    try {
      const page = await settlementApi.page({
        pageNum: 1,
        pageSize: 30,
        sortField: 'updateTime',
        sortOrder: 'descend',
      })
      notifications.value = buildNotifications(page?.records || [])
      loaded.value = true
    } finally {
      loading.value = false
    }
  }

  return {
    loading,
    loaded,
    notifications,
    actionableCount,
    fetchNotifications,
  }
}

function buildNotifications(records: SettlementItem[]): SettlementNotificationItem[] {
  return records
    .flatMap((item) => {
      if (item.status === 0) {
        return [toPendingNotification(item)]
      }

      if (isRecentlyUpdated(item.updateTime)) {
        return [toUpdatedNotification(item)]
      }

      return []
    })
    .sort((left, right) => dayjs(right.updateTime).valueOf() - dayjs(left.updateTime).valueOf())
}

function toPendingNotification(item: SettlementItem): SettlementNotificationItem {
  return {
    id: `pending-${item.id}`,
    eventId: item.eventId,
    eventTitle: item.eventTitle,
    performerName: item.performerName,
    type: 'pending',
    typeLabel: '待结算',
    description: `待结算金额 ${formatAmount(item.settlementAmount)}，共 ${item.totalTickets} 张票待核对`,
    updateTime: item.updateTime,
    amount: item.settlementAmount,
    tickets: item.totalTickets,
  }
}

function toUpdatedNotification(item: SettlementItem): SettlementNotificationItem {
  return {
    id: `updated-${item.id}`,
    eventId: item.eventId,
    eventTitle: item.eventTitle,
    performerName: item.performerName,
    type: 'updated',
    typeLabel: '已更新',
    description: `结算金额 ${formatAmount(item.settlementAmount)}，佣金 ${formatAmount(item.commissionAmount)}`,
    updateTime: item.updateTime,
    amount: item.settlementAmount,
    tickets: item.totalTickets,
  }
}

function isRecentlyUpdated(value?: string) {
  if (!value) return false
  const updateTime = dayjs(value)
  if (!updateTime.isValid()) return false
  return dayjs().diff(updateTime, 'hour') <= RECENT_UPDATE_HOURS
}

function formatAmount(value?: number) {
  return Number(value || 0).toLocaleString('zh-CN', {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2,
  })
}
