import { onMounted, reactive, ref } from 'vue'
import { message } from 'ant-design-vue'
import { performerApi } from '@/api/performer'
import { styleApi } from '@/api/style'
import type { PerformerItem, PerformerSaveReq } from '@/types'
import { performerTableColumns } from './columns'

const DEFAULT_PERFORMER_STATUS = 1

function createDefaultFormData(): PerformerSaveReq {
  return {
    name: '',
    avatarUrl: '',
    styleIds: [],
    description: '',
    status: DEFAULT_PERFORMER_STATUS,
  }
}

export function usePerformerList() {
  const loading = ref(false)
  const list = ref<PerformerItem[]>([])
  const pagination = reactive({ current: 1, pageSize: 10, total: 0 })

  async function fetchList() {
    loading.value = true
    try {
      const res = await performerApi.page({ current: pagination.current, size: pagination.pageSize })
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

  const formVisible = ref(false)
  const submitting = ref(false)
  const editingId = ref<number | null>(null)
  const formData = reactive<PerformerSaveReq>(createDefaultFormData())
  const styleOptions = ref<any[]>([])

  async function fetchStyleOptions() {
    try {
      const res = await styleApi.page({ current: 1, size: 100 })
      styleOptions.value = res?.records || []
    } catch (err) {
      console.error('加载风格选项失败', err)
    }
  }

  function resetForm() {
    editingId.value = null
    Object.assign(formData, createDefaultFormData())
  }

  function openForm(record?: PerformerItem) {
    if (record) {
      editingId.value = record.id
      formData.name = record.name || ''
      formData.avatarUrl = record.avatarUrl || ''
      formData.styleIds = record.styleIds || []
      formData.description = record.description || ''
      formData.genre = record.genre || ''
      formData.status = record.status ?? DEFAULT_PERFORMER_STATUS
    } else {
      resetForm()
    }
    formVisible.value = true
  }

  async function onSubmit() {
    if (!formData.name.trim()) {
      message.warning('请填写艺人名称')
      return
    }

    submitting.value = true
    try {
      const payload: PerformerSaveReq = {
        name: formData.name.trim(),
        avatarUrl: formData.avatarUrl?.trim() || '',
        description: formData.description?.trim() || '',
        genre: formData.genre?.trim() || undefined,
        styleIds: formData.styleIds || [],
        status: formData.status ?? DEFAULT_PERFORMER_STATUS,
      }

      if (editingId.value) {
        await performerApi.update({ id: editingId.value, ...payload } as PerformerItem)
        message.success('更新成功')
      } else {
        await performerApi.create(payload)
        message.success('创建成功')
      }
      formVisible.value = false
      resetForm()
      void fetchList()
    } finally {
      submitting.value = false
    }
  }

  async function onDelete(id: number) {
    await performerApi.delete(id)
    message.success('已删除')
    void fetchList()
  }

  onMounted(() => {
    void fetchList()
    void fetchStyleOptions()
  })

  return {
    columns: performerTableColumns,
    loading,
    list,
    pagination,
    onTableChange,
    formVisible,
    submitting,
    editingId,
    formData,
    styleOptions,
    openForm,
    onSubmit,
    onDelete,
  }
}
