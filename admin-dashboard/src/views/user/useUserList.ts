import { computed, onMounted, reactive, ref } from 'vue'
import { message } from 'ant-design-vue'
import { getAdminSession, UserRole } from '@/api/http'
import { userApi } from '@/api/user'
import { venueApi } from '@/api/venue'
import type { UserItem, VenueItem, VisitorItem } from '@/types'
import {
  formatUserType,
  formatVerifiedStatus,
  userTableColumns,
  userTypeOptions,
  userVisitorColumns,
} from './columns'

export function useUserList() {
  const loading = ref(false)
  const list = ref<UserItem[]>([])
  const pagination = reactive({
    current: 1,
    pageSize: 10,
    total: 0,
    showSizeChanger: true,
    pageSizeOptions: ['10', '20', '50', '100'],
    showTotal: (total: number) => `共 ${total} 条`,
  })
  const sorterState = reactive<{ field?: string; order?: string | null }>({
    field: 'id',
    order: 'descend',
  })
  const filters = reactive<{ userType?: number; phone: string }>({
    phone: '',
  })
  const visitorMap = reactive<Record<string, VisitorItem[]>>({})
  const loadingVisitors = reactive<Record<string, boolean>>({})

  const venueOptions = ref<VenueItem[]>([])
  const bindModalOpen = ref(false)
  const bindSubmitting = ref(false)
  const bindForm = reactive<{ userId: string; phone: string; venueId?: number; userType?: number }>({
    userId: '',
    phone: '',
  })

  const currentAdminUserId = computed(() => getAdminSession()?.userId)

  function maskPhone(phone: string) {
    if (!phone || phone.length < 7) return phone
    return `${phone.substring(0, 3)}****${phone.substring(7)}`
  }

  function maskIdCard(idCard: string) {
    if (!idCard || idCard.length < 10) return idCard
    return `${idCard.substring(0, 4)}**********${idCard.substring(idCard.length - 4)}`
  }

  function formatDate(dateTime?: string) {
    if (!dateTime) return ''
    return dateTime.slice(0, 10)
  }

  function normalizePhone(value: string) {
    return value.replace(/\D/g, '').slice(0, 11)
  }

  function isSelf(record: UserItem) {
    return String(record.id) === String(currentAdminUserId.value || '')
  }

  function canOperate(record: UserItem) {
    return !isSelf(record) && record.userType !== UserRole.SuperAdmin
  }

  function warnUnoperable(record: UserItem) {
    if (isSelf(record)) {
      message.warning('不能在用户管理中修改当前登录账号')
      return true
    }
    if (record.userType === UserRole.SuperAdmin) {
      message.warning('不能在用户管理中修改超级管理员账号')
      return true
    }
    return false
  }

  async function fetchList() {
    loading.value = true
    try {
      const res = await userApi.page({
        current: pagination.current,
        size: pagination.pageSize,
        sortField: sorterState.field,
        sortOrder: sorterState.order,
        userType: filters.userType,
        phone: filters.phone || undefined,
      })
      list.value = res?.records || []
      pagination.total = res?.total || 0
    } finally {
      loading.value = false
    }
  }

  function onTableChange(pag: any, _filters: any, sorter: any) {
    pagination.current = pag.current
    pagination.pageSize = pag.pageSize
    if (sorter?.field) {
      sorterState.field = sorter.field
      sorterState.order = sorter.order
    }
    void fetchList()
  }

  function onUserTypeChange(value: number | undefined) {
    filters.userType = value
    pagination.current = 1
    void fetchList()
  }

  function onPhoneChange(value: string) {
    filters.phone = normalizePhone(value)
  }

  function onSearch() {
    if (filters.phone && filters.phone.length < 3) {
      message.warning('手机号搜索至少输入 3 位数字')
      return
    }
    pagination.current = 1
    void fetchList()
  }

  async function ensureVenueOptions() {
    if (venueOptions.value.length > 0) return
    const res = await venueApi.page({ current: 1, size: 200 })
    venueOptions.value = res?.records || []
  }

  async function openBindVenueModal(record: UserItem) {
    if (warnUnoperable(record)) return
    if (record.status === 0) {
      message.warning('封禁用户不能设置为场地管理员')
      return
    }
    bindForm.userId = record.id
    bindForm.phone = record.phone
    bindForm.userType = record.userType
    bindForm.venueId = undefined
    await ensureVenueOptions()
    const ownedVenue = venueOptions.value.find((item) => String(item.ownerUserId || '') === String(record.id))
    if (ownedVenue) {
      bindForm.venueId = ownedVenue.id
    }
    bindModalOpen.value = true
  }

  async function submitBindVenue() {
    if (!bindForm.userId) {
      message.warning('缺少用户ID')
      return
    }
    if (!bindForm.venueId) {
      message.warning('请选择要关联的场馆')
      return
    }

    bindSubmitting.value = true
    try {
      await userApi.bindVenueAdmin({
        userId: bindForm.userId,
        venueId: bindForm.venueId,
      })
      message.success('已设置为场地管理员并关联场馆，用户需重新登录')
      bindModalOpen.value = false
      venueOptions.value = []
      void fetchList()
    } finally {
      bindSubmitting.value = false
    }
  }

  function isCurrentUserType(record: UserItem, userType: number) {
    return record.userType === userType
  }

  async function updateUserType(record: UserItem, userType: number) {
    if (warnUnoperable(record)) return
    if (isCurrentUserType(record, userType)) {
      message.warning('当前用户已经是该类型，无需重复设置')
      return
    }

    if (userType === UserRole.VenueAdmin) {
      await openBindVenueModal(record)
      return
    }

    await userApi.updateUserType(record.id, userType)
    message.success(`${userType === UserRole.Artist ? '已设置为艺人' : '已设置为普通用户'}，用户需重新登录`)
    void fetchList()
  }

  async function toggleUserStatus(record: UserItem) {
    if (warnUnoperable(record)) return
    const nextStatus = record.status === 0 ? 1 : 0
    await userApi.updateStatus(record.id, nextStatus)
    message.success(nextStatus === 0 ? '账号已封禁，用户需重新登录' : '账号已解封')
    void fetchList()
  }

  const columns = computed(() => [
    ...userTableColumns,
    { title: '操作', key: 'action', width: 260, fixed: 'right' },
  ])

  const venueSelectOptions = computed(() =>
    venueOptions.value.map((item) => ({
      label: `${item.name} (${item.city || '未设置城市'})${item.ownerUserId && String(item.ownerUserId) === String(bindForm.userId) ? ' - 当前绑定' : ''}`,
      value: item.id,
      disabled: !!item.ownerUserId && String(item.ownerUserId) !== String(bindForm.userId),
    }))
  )

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
    columns,
    visitorColumns: userVisitorColumns,
    userTypeOptions,
    loading,
    list,
    pagination,
    filters,
    bindModalOpen,
    bindSubmitting,
    bindForm,
    venueSelectOptions,
    visitorMap,
    loadingVisitors,
    maskPhone,
    maskIdCard,
    formatDate,
    formatUserType,
    formatVerifiedStatus,
    canOperate,
    isCurrentUserType,
    onTableChange,
    onUserTypeChange,
    onPhoneChange,
    onSearch,
    openBindVenueModal,
    submitBindVenue,
    updateUserType,
    toggleUserStatus,
    onExpand,
  }
}
