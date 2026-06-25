import { ref, reactive, type Ref } from 'vue'
import { settlementApi } from '@/api/settlement'
import type { SettlementItem } from '@/types'
import type { AntTablePaginationChange } from './settlement.types'

export function useSettlementList() {
  const currentEventId = ref<number | undefined>(undefined)
  const columns = [
    { title: 'ID', dataIndex: 'id', key: 'id', width: 60 },
    { title: '演出', dataIndex: 'eventTitle', key: 'eventTitle', ellipsis: true },
    { title: '总出票', dataIndex: 'totalTickets', key: 'totalTickets', width: 90 },
    { title: '总票房', key: 'revenue', width: 140 },
    { title: '佣金', key: 'commission', width: 120 },
    { title: '净结算', key: 'net', width: 140 },
    { title: '状态', key: 'status', width: 90 },
    { title: '更新时间', dataIndex: 'updateTime', key: 'updateTime', width: 170 },
  ]

  const loading = ref(false)
  const list: Ref<SettlementItem[]> = ref([])
  const pagination = reactive({ current: 1, pageSize: 10, total: 0 })

  async function fetchList(eventId?: number) {
    currentEventId.value = eventId
    loading.value = true
    try {
      const res = await settlementApi.page({
        eventId,
        pageNum: pagination.current,
        pageSize: pagination.pageSize,
      })
      list.value = res?.records || []
      pagination.total = res?.total || 0
    } finally {
      loading.value = false
    }
  }

  function onTableChange(pag: AntTablePaginationChange) {
    pagination.current = pag.current
    fetchList(currentEventId.value)
  }

  return {
    columns,
    loading,
    list,
    pagination,
    fetchList,
    onTableChange,
  }
}
