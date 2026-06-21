import type { SettlementItem, SettlementStats } from '@/types'

export const SettlementStatus = {
  Pending: 0,
  Settled: 1,
} as const

export type SettlementStatusValue = typeof SettlementStatus[keyof typeof SettlementStatus]

export const SETTLEMENT_STATUS_META: Record<
  SettlementStatusValue,
  { label: string; color: string }
> = {
  [SettlementStatus.Pending]: { label: '待结算', color: 'orange' },
  [SettlementStatus.Settled]: { label: '已结算', color: 'green' },
}

export interface SettlementRow extends Omit<SettlementItem, 'status'> {
  status: SettlementStatusValue
}

export interface AntTablePaginationChange {
  current: number
  pageSize: number
}

export interface StatCardConfig {
  title: string
  value: number
  prefix?: string
  color: string
}

export interface ShardCell {
  index: number
  tableName: `t_order_item_${number}`
  visible: boolean
  height: number
  revenue: number
}

export interface ShardScanState {
  scanning: boolean
  scanIndex: number
  shards: ShardCell[]
}

export function buildStatsCards(stats: SettlementStats): StatCardConfig[] {
  return [
    { title: '总票房收入', value: stats.totalRevenue, prefix: '¥', color: '#52c41a' },
    { title: '平台佣金', value: stats.totalCommission, prefix: '¥', color: '#fa8c16' },
    { title: '净结算额', value: stats.totalNetAmount, prefix: '¥', color: '#1677ff' },
    { title: '结算单数', value: stats.totalOrders, color: '#262626' },
  ]
}
