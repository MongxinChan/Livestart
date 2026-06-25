import { ref, onScopeDispose, type Ref } from 'vue'
import type { SettlementShardItem } from '@/types'
import type { ShardCell, ShardScanState } from './settlement.types'

export interface StartOptions {
  shards: SettlementShardItem[]
  onComplete?: () => void
}

const SHARD_COUNT = 16
const DEFAULT_HEIGHT = 10

function createEmptyShards(): ShardCell[] {
  return Array.from({ length: SHARD_COUNT }, (_, i) => ({
    index: i,
    tableName: `t_order_item_${i}` as const,
    visible: true,
    height: DEFAULT_HEIGHT,
    revenue: 0,
    tickets: 0,
    commissionAmount: 0,
    settlementAmount: 0,
  }))
}

function normalizeShardHeights(shards: SettlementShardItem[]) {
  const maxRevenue = Math.max(...shards.map((item) => Number(item.totalSalesAmount ?? 0)), 0)
  if (maxRevenue <= 0) {
    return shards.map(() => DEFAULT_HEIGHT)
  }
  return shards.map((item) => Math.max((Number(item.totalSalesAmount ?? 0) / maxRevenue) * 90 + 10, DEFAULT_HEIGHT))
}

function toShardCells(shards: SettlementShardItem[]): ShardCell[] {
  const sorted = [...shards].sort((left, right) => left.shardIndex - right.shardIndex)
  const heights = normalizeShardHeights(sorted)

  return Array.from({ length: SHARD_COUNT }, (_, index) => {
    const shard = sorted[index]
    if (!shard) {
      return {
        index,
        tableName: `t_order_item_${index}` as const,
        visible: true,
        height: DEFAULT_HEIGHT,
        revenue: 0,
        tickets: 0,
        commissionAmount: 0,
        settlementAmount: 0,
      }
    }

    return {
      index,
      tableName: `t_order_item_${index}` as const,
      visible: true,
      height: heights[index],
      revenue: Number(shard.totalSalesAmount ?? 0),
      tickets: Number(shard.totalTickets ?? 0),
      commissionAmount: Number(shard.commissionAmount ?? 0),
      settlementAmount: Number(shard.settlementAmount ?? 0),
    }
  })
}

export function useShardScanAnimation() {
  const state = ref<ShardScanState>({
    scanning: false,
    scanIndex: SHARD_COUNT - 1,
    shards: createEmptyShards(),
  })

  const showVisualization = ref(true)
  let timer: ReturnType<typeof setInterval> | null = null

  function syncFromShards(shards: SettlementShardItem[]) {
    state.value.scanning = false
    state.value.scanIndex = SHARD_COUNT - 1
    state.value.shards = toShardCells(shards)
    showVisualization.value = true
  }

  function reset() {
    if (timer) {
      clearInterval(timer)
      timer = null
    }
    state.value.scanning = false
    state.value.scanIndex = SHARD_COUNT - 1
    state.value.shards = createEmptyShards()
    showVisualization.value = true
  }

  function start(opts: StartOptions) {
    if (timer) {
      clearInterval(timer)
      timer = null
    }

    const shardCells = toShardCells(opts.shards)
    showVisualization.value = true
    state.value.scanning = true
    state.value.scanIndex = -1
    state.value.shards = createEmptyShards()

    let i = 0
    timer = setInterval(() => {
      if (i < SHARD_COUNT) {
        state.value.scanIndex = i
        state.value.shards[i] = { ...shardCells[i], visible: true }
        i++
      } else {
        clearInterval(timer!)
        timer = null
        state.value.scanning = false
        state.value.scanIndex = SHARD_COUNT - 1
        opts.onComplete?.()
      }
    }, 120)
  }

  onScopeDispose(() => {
    if (timer) {
      clearInterval(timer)
    }
  })

  return {
    state: state as Readonly<Ref<ShardScanState>>,
    showVisualization,
    start,
    reset,
    syncFromShards,
  }
}
