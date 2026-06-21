import { ref, onScopeDispose, type Ref } from 'vue'
import type { ShardCell, ShardScanState } from './settlement.types'

export interface StartOptions {
  eventId: number
  onComplete?: () => void
}

export function useShardScanAnimation() {
  const state = ref<ShardScanState>({
    scanning: false,
    scanIndex: -1,
    shards: Array.from({ length: 16 }, (_, i) => ({
      index: i,
      tableName: `t_order_item_${i}` as const,
      visible: false,
      height: 0,
      revenue: 0,
    })),
  })

  const showVisualization = ref(false)

  let timer: ReturnType<typeof setInterval> | null = null

  function reset() {
    if (timer) {
      clearInterval(timer)
      timer = null
    }
    state.value.scanning = false
    state.value.scanIndex = -1
    state.value.shards = Array.from({ length: 16 }, (_, i) => ({
      index: i,
      tableName: `t_order_item_${i}` as const,
      visible: false,
      height: 0,
      revenue: 0,
    }))
    showVisualization.value = false
  }

  function start(opts: StartOptions) {
    reset()
    showVisualization.value = true
    state.value.scanning = true

    /* DEMO ONLY: 模拟 16 个分片扫描，生产环境应替换为真实后端 SSE/轮询进度 */
    const priceBase = opts.eventId === 102 ? 980 : 380
    const totalTicketsEstimated = Math.floor(180 + Math.random() * 220)
    const maxRev = (totalTicketsEstimated * priceBase) / 10

    let i = 0
    timer = setInterval(() => {
      if (i < 16) {
        state.value.scanIndex = i
        const shardTickets = Math.floor(8 + Math.random() * 32)
        const shardRevenue = shardTickets * priceBase

        state.value.shards[i].visible = true
        state.value.shards[i].revenue = shardRevenue
        state.value.shards[i].height = (shardRevenue / maxRev) * 90 + 10
        i++
      } else {
        clearInterval(timer!)
        timer = null
        state.value.scanning = false
        opts.onComplete?.()
      }
    }, 120)
  }

  onScopeDispose(() => {
    if (timer) clearInterval(timer)
  })

  return {
    state: state as Readonly<Ref<ShardScanState>>,
    showVisualization,
    start,
    reset,
  }
}
