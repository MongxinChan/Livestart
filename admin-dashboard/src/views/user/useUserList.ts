import { onMounted, reactive, ref } from 'vue'
import { userApi } from '@/api/user'
import type { UserItem, VisitorItem } from '@/types'
import { userTableColumns, userVisitorColumns } from './columns'

export function useUserList() {
  const loading = ref(false)
  const list = ref<UserItem[]>([])
  const pagination = reactive({ current: 1, pageSize: 10, total: 0 })
  const visitorMap = reactive<Record<number, VisitorItem[]>>({})
  const loadingVisitors = reactive<Record<number, boolean>>({})

  function maskPhone(phone: string) {
    if (!phone || phone.length < 7) return phone
    return `${phone.substring(0, 3)}****${phone.substring(7)}`
  }

  function maskIdCard(idCard: string) {
    if (!idCard || idCard.length < 10) return idCard
    return `${idCard.substring(0, 4)}**********${idCard.substring(idCard.length - 4)}`
  }

  async function fetchList() {
    loading.value = true
    try {
      const res = await userApi.page({ current: pagination.current, size: pagination.pageSize })
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

  async function onExpand(expanded: boolean, record: UserItem) {
    if (!expanded) return
    if (visitorMap[record.id]) return
    loadingVisitors[record.id] = true
    try {
      const res = await userApi.visitors(record.id)
      visitorMap[record.id] = res || []
    } catch {
      visitorMap[record.id] = []
    } finally {
      loadingVisitors[record.id] = false
    }
  }

  onMounted(() => {
    void fetchList()
  })

  return {
    columns: userTableColumns,
    visitorColumns: userVisitorColumns,
    loading,
    list,
    pagination,
    visitorMap,
    loadingVisitors,
    maskPhone,
    maskIdCard,
    onTableChange,
    onExpand,
  }
}
