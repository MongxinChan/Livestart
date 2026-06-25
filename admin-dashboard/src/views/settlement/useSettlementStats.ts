import { reactive } from 'vue'
import { settlementApi } from '@/api/settlement'
import type { SettlementStats } from '@/types'

export function useSettlementStats() {
  const statsData = reactive<SettlementStats>({
    totalEvents: 0,
    totalTickets: 0,
    grossRevenue: 0,
    totalCommission: 0,
    netSettlement: 0,
  })

  async function fetchStats(eventId?: number) {
    try {
      const res = await settlementApi.stats(eventId)
      if (res) {
        Object.assign(statsData, res)
      }
    } catch {
      // 统计信息不是关键链路，失败时静默处理
    }
  }

  return {
    statsData,
    fetchStats,
  }
}
