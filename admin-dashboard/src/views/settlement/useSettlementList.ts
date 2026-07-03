import { ref, reactive, type Ref } from 'vue'
import { settlementApi } from '@/api/settlement'
import type { SettlementItem } from '@/types'
import type { AntTableChange, SettlementSortField, SettlementSortOrder } from './settlement.types'

export function useSettlementList() {
  const currentEventId = ref<number | undefined>(undefined)
  const keyword = ref('')
  const sortField = ref<SettlementSortField>('eventId')
  const sortOrder = ref<SettlementSortOrder>('descend')

  const columns = [
    { title: '演出序号', dataIndex: 'eventId', key: 'eventId', width: 110, sorter: true },
    { title: '演出名称', dataIndex: 'eventTitle', key: 'eventTitle', ellipsis: true },
    { title: '艺人', dataIndex: 'performerName', key: 'performerName', width: 140, ellipsis: true },
    { title: '出票数', dataIndex: 'totalTickets', key: 'totalTickets', width: 100, sorter: true },
    { title: '总票房', key: 'revenue', width: 140, sorter: true },
    { title: '佣金金额', key: 'commission', width: 130, sorter: true },
    { title: '净结算', key: 'net', width: 140 },
    { title: '状态', key: 'status', width: 90 },
    { title: '更新时间', dataIndex: 'updateTime', key: 'updateTime', width: 180, sorter: true },
  ]

  const loading = ref(false)
  const list: Ref<SettlementItem[]> = ref([])
  const pagination = reactive({ current: 1, pageSize: 10, total: 0 })

  async function fetchList(eventId?: number) {
    currentEventId.value = eventId
    loading.value = true
    try {
      const sortFieldForApi = normalizeSortField(sortField.value)
      const res = await settlementApi.page({
        eventId,
        keyword: keyword.value.trim() || undefined,
        sortField: sortFieldForApi,
        sortOrder: sortOrder.value,
        pageNum: pagination.current,
        pageSize: pagination.pageSize,
      })
      list.value = res?.records || []
      pagination.total = res?.total || 0
    } finally {
      loading.value = false
    }
  }

  async function search(nextKeyword: string, eventId?: number) {
    keyword.value = nextKeyword
    pagination.current = 1
    await fetchList(eventId ?? currentEventId.value)
  }

  function clearSearchState() {
    keyword.value = ''
    pagination.current = 1
  }

  function onTableChange(change: AntTableChange) {
    pagination.current = change.current
    if (change.pageSize && change.pageSize !== pagination.pageSize) {
      pagination.pageSize = change.pageSize
    }
    if (change.sortField) {
      sortField.value = change.sortField
    } else {
      sortField.value = 'eventId'
    }
    if (change.sortOrder) {
      sortOrder.value = change.sortOrder
    } else {
      sortOrder.value = 'descend'
    }
    fetchList(currentEventId.value)
  }

  return {
    columns,
    loading,
    list,
    pagination,
    keyword,
    sortField,
    sortOrder,
    fetchList,
    search,
    clearSearchState,
    onTableChange,
  }
}

function normalizeSortField(field: SettlementSortField): string {
  if (field === 'revenue') return 'totalSalesAmount'
  if (field === 'commission') return 'commissionAmount'
  return field
}
