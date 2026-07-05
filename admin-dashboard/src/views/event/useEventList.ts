import { computed, onMounted, reactive, ref } from 'vue'
import { message, Modal } from 'ant-design-vue'
import dayjs from 'dayjs'
import { eventApi } from '@/api/event'
import { performerApi } from '@/api/performer'
import { styleApi } from '@/api/style'
import { venueApi } from '@/api/venue'
import { eventConfigApi, type EventConfigItem } from '@/api/eventConfig'
import type { EventItem, SaleStageItem, VenueItem } from '@/types'

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

function createDefaultSaleStage(stageNo: number): SaleStageItem {
  return {
    stageNo,
    stageName: stageNo === 1 ? '预售第一阶段' : `预售第 ${stageNo} 阶段`,
    saleStartTime: '',
    remark: '',
  }
}

function normalizeSaleStages(stages?: SaleStageItem[], fallbackTicketStage?: number): SaleStageItem[] {
  if (stages && stages.length > 0) {
    return stages
      .map((item, index) => ({
        stageNo: item.stageNo || index + 1,
        stageName: item.stageName || (item.stageNo === 1 ? '预售第一阶段' : `预售第 ${item.stageNo || index + 1} 阶段`),
        saleStartTime: item.saleStartTime || '',
        remark: item.remark || '',
      }))
      .sort((a, b) => a.stageNo - b.stageNo)
  }

  const ticketStage = fallbackTicketStage && fallbackTicketStage > 1 ? fallbackTicketStage : 1
  return Array.from({ length: ticketStage }, (_, index) => createDefaultSaleStage(index + 1))
}

function getNow() {
  return dayjs()
}

function isBeforeNow(value?: string) {
  return !!value && dayjs(value).isBefore(getNow())
}

function disabledPastDate(current: dayjs.Dayjs) {
  return current && current.endOf('day').isBefore(getNow())
}

function disabledPastTime(current: dayjs.Dayjs | null) {
  const selected = current || getNow()
  const now = getNow()

  if (!selected.isSame(now, 'day')) {
    return {}
  }

  return {
    disabledHours: () => Array.from({ length: now.hour() }, (_, index) => index),
    disabledMinutes: (selectedHour: number) =>
      selectedHour === now.hour()
        ? Array.from({ length: now.minute() }, (_, index) => index)
        : [],
    disabledSeconds: (selectedHour: number, selectedMinute: number) =>
      selectedHour === now.hour() && selectedMinute === now.minute()
        ? Array.from({ length: now.second() }, (_, index) => index)
        : [],
  }
}

export function useEventList() {
  const loading = ref(false)
  const list = ref<EventItem[]>([])
  const pagination = reactive({ current: 1, pageSize: 10, total: 0 })
  const performerOptions = ref<any[]>([])
  const styleOptions = ref<any[]>([])
  const venueOptions = ref<{ label: string; value: number }[]>([])

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
      // ignore
    }
  }

  async function fetchStyleOptions() {
    try {
      const res = await styleApi.page({ current: 1, size: 100 })
      styleOptions.value = res?.records || []
    } catch {
      // ignore
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
    saleStages: [createDefaultSaleStage(1)] as SaleStageItem[],
    styleIds: [] as number[],
  })

  const stageCount = computed(() => formData.saleStages.length)

  function syncSaleStageMeta() {
    formData.saleStages = formData.saleStages
      .map((item, index) => ({
        ...item,
        stageNo: index + 1,
        stageName: item.stageName || (index === 0 ? '预售第一阶段' : `预售第 ${index + 1} 阶段`),
      }))
    formData.ticketStage = formData.saleStages.length
  }

  function resetForm() {
    editingId.value = null
    formData.title = ''
    formData.eventType = 0
    formData.venueId = null
    formData.startTime = ''
    formData.posterUrl = ''
    formData.performerId = null
    formData.ticketStage = 1
    formData.saleStages = [createDefaultSaleStage(1)]
    formData.styleIds = []
  }

  function addSaleStage() {
    const nextStageNo = formData.saleStages.length + 1
    formData.saleStages.push(createDefaultSaleStage(nextStageNo))
    syncSaleStageMeta()
  }

  function removeSaleStage(index: number) {
    if (formData.saleStages.length <= 1) {
      message.warning('至少需要保留一个开售阶段')
      return
    }
    formData.saleStages.splice(index, 1)
    syncSaleStageMeta()
  }

  async function openForm(record?: EventItem) {
    if (record) {
      const detail = await eventApi.getById(record.id)
      editingId.value = detail.id
      formData.title = detail.title
      formData.eventType = detail.eventType
      formData.venueId = detail.venueId
      formData.startTime = detail.startTime
      formData.posterUrl = detail.posterUrl
      formData.performerId = detail.performerId || null
      formData.ticketStage = detail.ticketStage || 1
      formData.saleStages = normalizeSaleStages(detail.saleStages, detail.ticketStage)
      formData.styleIds = (detail as any).styleIds || []
    } else {
      resetForm()
    }
    syncSaleStageMeta()
    formVisible.value = true
  }

  function validateSaleStages() {
    if (!formData.saleStages.length) {
      message.warning('请至少配置一个开售阶段')
      return false
    }
    const hasEmptyTime = formData.saleStages.some((item) => !item.saleStartTime)
    if (hasEmptyTime) {
      message.warning('请填写所有开售阶段的开售时间')
      return false
    }

    const hasPastTime = formData.saleStages.some((item) => isBeforeNow(item.saleStartTime))
    if (hasPastTime) {
      message.warning('开售时间不得早于当前时间')
      return false
    }

    const orderedTimes = formData.saleStages.map((item) => dayjs(item.saleStartTime).valueOf())
    for (let i = 1; i < orderedTimes.length; i += 1) {
      if (orderedTimes[i] < orderedTimes[i - 1]) {
        message.warning('开售阶段时间需要按顺序递增')
        return false
      }
    }
    const eventTime = dayjs(formData.startTime).valueOf()
    const hasStageAfterEvent = orderedTimes.some((time) => time > eventTime)
    if (hasStageAfterEvent) {
      message.warning('开售时间不得晚于演出时间')
      return false
    }
    return true
  }

  async function fetchVenueOptions() {
    try {
      const res = await venueApi.page({ current: 1, size: 500 })
      const records = res?.records || []
      venueOptions.value = records.map((venue: VenueItem) => ({
        label: `${venue.name} · ${venue.city || '未知城市'} · ID:${venue.id}`,
        value: venue.id,
      }))
    } catch {
      venueOptions.value = []
      message.warning('场馆列表加载失败，请稍后重试')
    }
  }

  async function onSubmit() {
    if (!formData.title || !formData.startTime || !formData.venueId) {
      message.warning('请填写必填项')
      return
    }
    if (isBeforeNow(formData.startTime)) {
      message.warning('演出时间不得早于当前时间')
      return
    }
    if (!validateSaleStages()) {
      return
    }

    submitting.value = true
    try {
      const payload = {
        ...formData,
        ticketStage: formData.saleStages.length,
        saleStages: formData.saleStages.map((item, index) => ({
          stageNo: index + 1,
          stageName: item.stageName,
          saleStartTime: item.saleStartTime,
          remark: item.remark,
        })),
      }

      if (editingId.value) {
        await eventApi.update({ id: editingId.value, ...payload })
        message.success('更新成功')
      } else {
        await eventApi.create(payload)
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
      // ignore and keep default values
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
        content: `将删除演出《${record.title}》，此操作不可恢复。`,
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
    void fetchVenueOptions()
  })

  return {
    loading,
    list,
    pagination,
    performerOptions,
    styleOptions,
    venueOptions,
    formVisible,
    submitting,
    editingId,
    formData,
    stageCount,
    disabledPastDate,
    disabledPastTime,
    addSaleStage,
    removeSaleStage,
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
