import { onMounted, reactive, ref } from 'vue'
import { orderApi } from '@/api/order'
import { orderStatusColors, orderStatusLabels, orderTableColumns } from './columns'

export function useOrderList() {
  const loading = ref(false)
  const list = ref<any[]>([])
  const pagination = reactive({ current: 1, pageSize: 10, total: 0 })
  const statusFilter = ref<number | undefined>(undefined)

  async function fetchList() {
    loading.value = true
    try {
      const res = await orderApi.page({ status: statusFilter.value, current: pagination.current, size: pagination.pageSize })
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
    onTableChange,
  }
}
