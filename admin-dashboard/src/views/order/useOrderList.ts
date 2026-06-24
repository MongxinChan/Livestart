import { onMounted, reactive, ref } from 'vue'
import { orderApi } from '@/api/order'
import { orderStatusColors, orderStatusLabels, orderTableColumns } from './columns'

export function useOrderList() {
  const loading = ref(false)
  const list = ref<any[]>([])
  const pagination = reactive({ current: 1, pageSize: 10, total: 0 })
  const ALL_STATUS = -1
  const statusFilter = ref<number>(ALL_STATUS)

  async function fetchList() {
    loading.value = true
    try {
      const res = await orderApi.page({
        status: statusFilter.value === ALL_STATUS ? undefined : statusFilter.value,
        current: pagination.current,
        size: pagination.pageSize,
      })
      list.value = res?.records || []
      pagination.total = res?.total || 0
    } finally {
      loading.value = false
    }
  }

  function onTableChange(pag: any) {
    pagination.current = pag.current
    void fetchList()
  }

  function handleStatusChange() {
    pagination.current = 1
    void fetchList()
  }

  onMounted(() => {
    void fetchList()
  })

  return {
    columns: orderTableColumns,
    statusLabels: orderStatusLabels,
    statusColors: orderStatusColors,
    loading,
    list,
    pagination,
    statusFilter,
    fetchList,
    handleStatusChange,
    onTableChange,
  }
}
