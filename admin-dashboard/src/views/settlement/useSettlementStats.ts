import { reactive } from 'vue'
import { settlementApi } from '@/api/settlement'
import type { SettlementStats } from '@/types'

export function useSettlementStats() {
  const statsData = reactive<SettlementStats>({
    totalRevenue: 0,
    totalCommission: 0,
    totalNetAmount: 0,
    totalOrders: 0,
  })

  async function fetchStats(eventId?: number) {
    try {
      const res = await settlementApi.stats(eventId)
      if (res) Object.assign(statsData, res)
    } catch {
      // 统计数据非关键路径，静默失败
    }
  }

  return {
    statsData,
    fetchStats,
  }
}
