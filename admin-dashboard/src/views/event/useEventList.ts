import { computed, onMounted, reactive, ref } from 'vue'
import { message, Modal } from 'ant-design-vue'
import { eventApi } from '@/api/event'
import { performerApi } from '@/api/performer'
import { styleApi } from '@/api/style'
import { eventConfigApi, type EventConfigItem } from '@/api/eventConfig'
import type { EventItem } from '@/types'

export const eventStatusLabels: Record<number, string> = {
  0: '已下架',
  1: '预售',
  2: '在售',
  3: '售罄',
}

export const eventStatusColors: Record<number, string> = {
  0: 'default',
  1: 'orange',
  2: 'green',
  3: 'red',
}

export function useEventList() {
  const loading = ref(false)
  const list = ref<EventItem[]>([])
  const pagination = reactive({ current: 1, pageSize: 10, total: 0 })
  const performerOptions = ref<any[]>([])
  const styleOptions = ref<any[]>([])

  async function fetchList() {
    loading.value = true
    try {
      const res = await eventApi.page({ current: pagination.current, size: pagination.pageSize })
      list.value = res?.records || []
      pagination.total = res?.total || 0
    } finally {
      loading.value = false
    }
  }

  async function fetchPerformerOptions() {
    try {
      const res = await performerApi.page({ current: 1, size: 100 })
      performerOptions.value = res?.records || []
    } catch {
      // 忽略错误
    }
  }

  async function fetchStyleOptions() {
    try {
      const res = await styleApi.page({ current: 1, size: 100 })
      styleOptions.value = res?.records || []
    } catch {
      // 忽略错误
    }
  }

  function onTableChange(pag: any) {
    pagination.current = pag.current
    pagination.pageSize = pag.pageSize
    void fetchList()
  }

  const formVisible = ref(false)
  const submitting = ref(false)
  const editingId = ref<number | null>(null)
  const formData = reactive({
    title: '',
    eventType: 0,
    venueId: null as number | null,
    startTime: '',
    posterUrl: '',
    performerId: null as number | null,
    ticketStage: 1,
    styleIds: [] as number[],
  })

  function resetForm() {
    editingId.value = null
    formData.title = ''
    formData.eventType = 0
    formData.venueId = null
    formData.startTime = ''
    formData.posterUrl = ''
    formData.performerId = null
    formData.ticketStage = 1
    formData.styleIds = []
  }

  function openForm(record?: EventItem) {
    if (record) {
      editingId.value = record.id
      formData.title = record.title
      formData.eventType = record.eventType
      formData.venueId = record.venueId
      formData.startTime = record.startTime
      formData.posterUrl = record.posterUrl
      formData.performerId = (record as any).performerId || null
      formData.ticketStage = record.ticketStage || 1
      formData.styleIds = (record as any).styleIds || []
    } else {
      resetForm()
    }
    formVisible.value = true
  }

  async function onSubmit() {
    if (!formData.title || !formData.startTime || !formData.venueId) {
      message.warning('请填写必填项')
      return
    }
    submitting.value = true
    try {
      if (editingId.value) {
        await eventApi.update({ id: editingId.value, ...formData })
        message.success('更新成功')
      } else {
        await eventApi.create(formData)
        message.success('创建成功')
      }
      formVisible.value = false
      void fetchList()
    } finally {
      submitting.value = false
    }
  }

  const configVisible = ref(false)
  const configSubmitting = ref(false)
  const configEventId = ref<number>(0)
  const configData = reactive<EventConfigItem>({
    eventId: 0,
    selectionMode: 0,
    isVerifyRequired: 0,
    maxTicketsPerUser: 4,
    refundPolicyType: 0,
    tier1FreeRefundHours: 24,
    tier2PartialRefundHours: 6,
    tier2RefundFeeRate: 0.2,
    isTransferable: 0,
    isWaitingAllowed: 0,
  })

  const configVerifyRequired = computed({
    get: () => configData.isVerifyRequired === 1,
    set: (value: boolean) => {
      configData.isVerifyRequired = value ? 1 : 0
    },
  })

  const configTransferable = computed({
    get: () => configData.isTransferable === 1,
    set: (value: boolean) => {
      configData.isTransferable = value ? 1 : 0
    },
  })

  const configWaitingAllowed = computed({
    get: () => configData.isWaitingAllowed === 1,
    set: (value: boolean) => {
      configData.isWaitingAllowed = value ? 1 : 0
    },
  })

  async function openConfig(record: EventItem) {
    configEventId.value = record.id
    configData.eventId = record.id
    try {
      const res = await eventConfigApi.getByEventId(record.id)
      if (res) Object.assign(configData, res)
    } catch {
      // 如没有配置记录则继续使用默认值
    }
    configVisible.value = true
  }

  async function onConfigSubmit() {
    configSubmitting.value = true
    try {
      await eventConfigApi.update(configData)
      message.success('配置已保存')
      configVisible.value = false
    } finally {
      configSubmitting.value = false
    }
  }

  async function onAction(key: string, record: EventItem) {
    if (key === 'config') {
      await openConfig(record)
      return
    }

    if (key === 'publish') {
      await eventApi.publish(record.id)
      message.success('已上架开售')
      void fetchList()
      return
    }

    if (key === 'shelve') {
      await eventApi.shelve(record.id)
      message.success('已下架')
      void fetchList()
      return
    }

    if (key === 'terminate') {
      Modal.confirm({
        title: '确认终止售票？',
        content: '此操作不可逆，确认后该演出将永久停止售票。',
        okType: 'danger',
        onOk: async () => {
          await eventApi.terminate(record.id)
          message.success('已终止')
          void fetchList()
        },
      })
      return
    }

    if (key === 'delete') {
      Modal.confirm({
        title: '确认删除？',
        content: `将删除演出「${record.title}」，此操作不可恢复。`,
        okType: 'danger',
        onOk: async () => {
          await eventApi.delete(record.id)
          message.success('已删除')
          void fetchList()
        },
      })
    }
  }

  onMounted(() => {
    void fetchList()
    void fetchPerformerOptions()
    void fetchStyleOptions()
  })

  return {
    loading,
    list,
    pagination,
    performerOptions,
    styleOptions,
    formVisible,
    submitting,
    editingId,
    formData,
    configVisible,
    configSubmitting,
    configEventId,
    configData,
    configVerifyRequired,
    configTransferable,
    configWaitingAllowed,
    fetchList,
    onTableChange,
    openForm,
    onSubmit,
    openConfig,
    onConfigSubmit,
    onAction,
  }
}
