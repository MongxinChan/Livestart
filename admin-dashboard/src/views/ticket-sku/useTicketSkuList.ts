import { onMounted, reactive, ref, watch } from 'vue'
import { message } from 'ant-design-vue'
import { ticketSkuApi } from '@/api/ticketSku'
import { eventApi } from '@/api/event'
import type { EventItem, TicketSkuItem } from '@/types'
import { ticketSkuTableColumns } from './columns'

export function useTicketSkuList() {
  const loading = ref(false)
  const list = ref<TicketSkuItem[]>([])
  const pagination = reactive({
    current: 1,
    pageSize: 10,
    total: 0,
    showSizeChanger: true,
    pageSizeOptions: ['10', '20', '50', '100'],
    showTotal: (total: number) => `共 ${total} 条`,
  })

  const eventOptions = ref<{ label: string; value: number }[]>([])
  const eventTitleMap = reactive<Record<number, string>>({})

  function filterEventOption(input: string, option: { label: string; value: number }) {
    return (option.label || '').toLowerCase().includes(input.toLowerCase()) || String(option.value).includes(input)
  }

  async function fetchEventOptions() {
    try {
      const res = await eventApi.page({ current: 1, size: 500 })
      const records = res?.records || []
      eventOptions.value = records.map((event: EventItem) => ({ label: `${event.title} (ID: ${event.id})`, value: event.id }))
      records.forEach((event: EventItem) => {
        eventTitleMap[event.id] = event.title
      })
      if (records.length === 0) {
        message.warning('暂无演出数据')
      }
    } catch (err) {
      console.error('fetch events failed', err)
      message.error('演出列表加载失败')
    }
  }

  const filterMode = ref<'id' | 'name'>('id')
  const filterEventIdInput = ref<number | null>(null)
  const filterEventId = ref<number | null>(null)

  async function fetchList() {
    loading.value = true
    try {
      const res = await ticketSkuApi.page({
        eventId: filterEventId.value || undefined,
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
    pagination.pageSize = pag.pageSize
    void fetchList()
  }

  function onFilter() {
    filterEventId.value = filterEventIdInput.value
    pagination.current = 1
    void fetchList()
  }

  function onClearFilter() {
    filterEventIdInput.value = null
    filterEventId.value = null
    pagination.current = 1
    void fetchList()
  }

  const formVisible = ref(false)
  const submitting = ref(false)
  const editingId = ref<number | null>(null)
  const formData = reactive({
    eventId: 0,
    title: '',
    originalPrice: 0,
    sellingPrice: 0,
    totalStock: 0,
    stage1Stock: 0,
    stage2Stock: 0,
    limitNum: 4,
  })

  const increaseStockVisible = ref(false)
  const increaseStockSubmitting = ref(false)
  const increaseStockTarget = ref<TicketSkuItem | null>(null)
  const increaseStockCount = ref<number>(1)

  function resetForm() {
    editingId.value = null
    formData.eventId = 0
    formData.title = ''
    formData.originalPrice = 0
    formData.sellingPrice = 0
    formData.totalStock = 0
    formData.stage1Stock = 0
    formData.stage2Stock = 0
    formData.limitNum = 4
  }

  function openForm(record?: TicketSkuItem) {
    if (record) {
      editingId.value = record.id
      Object.assign(formData, record)
    } else {
      resetForm()
    }
    formVisible.value = true
  }

  watch(
    () => formData.totalStock,
    (newTotalStock, oldTotalStock) => {
      if (editingId.value || newTotalStock == null || Number.isNaN(newTotalStock)) {
        return
      }
      const previousTotalStock = oldTotalStock ?? 0
      if (formData.stage1Stock === previousTotalStock && (formData.stage2Stock ?? 0) === 0) {
        formData.stage1Stock = newTotalStock
      }
      if ((formData.stage1Stock ?? 0) + (formData.stage2Stock ?? 0) > newTotalStock) {
        formData.stage2Stock = Math.max(newTotalStock - (formData.stage1Stock ?? 0), 0)
      }
    }
  )

  watch(
    () => formData.stage1Stock,
    (newStage1Stock) => {
      if (editingId.value || newStage1Stock == null) {
        return
      }
      if ((newStage1Stock ?? 0) + (formData.stage2Stock ?? 0) > (formData.totalStock ?? 0)) {
        formData.stage2Stock = Math.max((formData.totalStock ?? 0) - (newStage1Stock ?? 0), 0)
      }
    }
  )

  function openIncreaseStock(record: TicketSkuItem) {
    increaseStockTarget.value = record
    increaseStockCount.value = 1
    increaseStockVisible.value = true
  }

  async function onSubmit() {
    if (!formData.title || !formData.sellingPrice) {
      message.warning('请填写必填项')
      return
    }
    if (!formData.eventId) {
      message.warning('请选择演出')
      return
    }
    if (!editingId.value && !formData.totalStock) {
      message.warning('请填写总库存')
      return
    }
    if (!editingId.value && formData.stage1Stock + formData.stage2Stock > formData.totalStock) {
      message.warning('一开和二开数量之和不能超过总库存')
      return
    }
    submitting.value = true
    try {
      if (editingId.value) {
        await ticketSkuApi.update({ id: editingId.value, ...formData } as any)
        message.success('更新成功')
      } else {
        await ticketSkuApi.create(formData)
        message.success('创建成功')
      }
      formVisible.value = false
      void fetchList()
    } finally {
      submitting.value = false
    }
  }

  async function onSubmitIncreaseStock() {
    if (!increaseStockTarget.value) return
    if (!increaseStockCount.value || increaseStockCount.value <= 0) {
      message.warning('请输入正确的增发数量')
      return
    }
    increaseStockSubmitting.value = true
    try {
      await ticketSkuApi.increaseStock({
        skuId: increaseStockTarget.value.id,
        count: increaseStockCount.value,
      })
      message.success('库存增发成功')
      increaseStockVisible.value = false
      await fetchList()
    } finally {
      increaseStockSubmitting.value = false
    }
  }

  async function onDelete(id: number) {
    await ticketSkuApi.delete(id)
    message.success('删除成功')
    void fetchList()
  }

  onMounted(() => {
    void fetchEventOptions()
    void fetchList()
  })

  return {
    columns: ticketSkuTableColumns,
    loading,
    list,
    pagination,
    eventOptions,
    eventTitleMap,
    filterEventOption,
    filterMode,
    filterEventIdInput,
    filterEventId,
    onFilter,
    onClearFilter,
    onTableChange,
    formVisible,
    submitting,
    editingId,
    formData,
    increaseStockVisible,
    increaseStockSubmitting,
    increaseStockTarget,
    increaseStockCount,
    openForm,
    openIncreaseStock,
    onSubmit,
    onSubmitIncreaseStock,
    onDelete,
  }
}
